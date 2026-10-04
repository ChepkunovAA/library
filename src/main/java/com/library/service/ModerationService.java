package com.library.service;

import com.library.model.Book;
import com.library.repo.BookRepository;
import com.library.repo.FavoriteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);

    private final BookRepository books;
    private final FavoriteRepository favorites;
    private final StorageService storage;

    public ModerationService(BookRepository books, FavoriteRepository favorites, StorageService storage) {
        this.books = books;
        this.favorites = favorites;
        this.storage = storage;
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = books.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Книга не найдена"));
        favorites.deleteByBookId(id);
        books.delete(book);
        storage.delete(book.fileName);
        log.info("Книга {} ({}) удалена модератором", book.id, book.title);
    }
}
