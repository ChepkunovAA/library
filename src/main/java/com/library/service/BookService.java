package com.library.service;

import com.library.model.Book;
import com.library.model.Favorite;
import com.library.model.User;
import com.library.repo.BookRepository;
import com.library.repo.FavoriteRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository books;
    private final FavoriteRepository favorites;
    private final StorageService storage;
    private final UserService users;

    public BookService(BookRepository books, FavoriteRepository favorites, StorageService storage, UserService users) {
        this.books = books;
        this.favorites = favorites;
        this.storage = storage;
        this.users = users;
    }

    public List<Book> find(String title) {
        if (title == null || title.isBlank()) {
            return books.findAll(Sort.by(Sort.Direction.DESC, "id"));
        }
        return books.findByTitleContainingIgnoreCase(title.trim());
    }

    public Book get(Long id) {
        return books.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Книга не найдена"));
    }

    public List<BookDto> view(String title) {
        return dto(find(title), users.current().orElse(null)).stream()
                .sorted(Comparator.comparingLong((BookDto book) -> book.likes).reversed()
                        .thenComparing(book -> book.id, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    public BookDto viewOne(Long id) {
        return dto(Collections.singletonList(get(id)), users.current().orElse(null)).get(0);
    }

    @Transactional
    public BookDto upload(String title, String author, String description, MultipartFile file) {
        if (title == null || title.isBlank() || author == null || author.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название и автор обязательны");
        }
        StorageService.Stored stored = storage.store(file);
        Book book = new Book();
        book.title = title.trim();
        book.author = author.trim();
        book.description = description == null ? "" : description.trim();
        book.fileName = stored.fileName;
        book.originalName = file.getOriginalFilename();
        book.format = StorageService.formatOf(file.getOriginalFilename());
        book.size = stored.size;
        book.sha256 = stored.sha256;
        return dto(Collections.singletonList(books.save(book)), null).get(0);
    }

    public Download download(Long id) {
        Book book = get(id);
        storage.verify(book);
        return new Download(new FileSystemResource(storage.path(book.fileName)), book.originalName);
    }

    @Transactional
    public BookDto toggleLike(Long id) {
        Book book = get(id);
        User user = users.current()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация"));
        Optional<Favorite> existing = favorites.findByUserIdAndBookId(user.id, book.id);
        if (existing.isPresent()) {
            favorites.delete(existing.get());
        } else {
            Favorite favorite = new Favorite();
            favorite.user = user;
            favorite.book = book;
            favorites.save(favorite);
        }
        return dto(Collections.singletonList(book), user).get(0);
    }

    private List<BookDto> dto(List<Book> books, User current) {
        return books.stream().map(book -> {
            BookDto dto = new BookDto();
            dto.id = book.id;
            dto.title = book.title;
            dto.author = book.author;
            dto.description = book.description;
            dto.format = book.format;
            dto.size = book.size;
            dto.likes = favorites.countByBookId(book.id);
            dto.liked = current != null && favorites.findByUserIdAndBookId(current.id, book.id).isPresent();
            return dto;
        }).collect(Collectors.toList());
    }

    public static class BookDto {
        public Long id;
        public String title;
        public String author;
        public String description;
        public String format;
        public long size;
        public long likes;
        public boolean liked;
    }

    public static class Download {
        public final Resource resource;
        public final String filename;

        Download(Resource resource, String filename) {
            this.resource = resource;
            this.filename = filename;
        }
    }
}
