package com.library.service;

import com.library.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Component
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private static final String[] FORMATS = {"pdf", "epub", "fb2"};

    private final Path dir;

    public StorageService(@Value("${app.storage.dir}") String dir) throws IOException {
        this.dir = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.dir);
    }

    public static String formatOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public boolean allowed(String filename) {
        return Arrays.asList(FORMATS).contains(formatOf(filename));
    }

    public Stored store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл не выбран");
        }
        if (!allowed(file.getOriginalFilename())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Допустимы только форматы PDF, EPUB и FB2");
        }
        if (file.getSize() > Book.MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл больше 50 МБ");
        }
        String storedName = UUID.randomUUID() + "." + formatOf(file.getOriginalFilename());
        Path target = dir.resolve(storedName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить файл", e);
        }
        return new Stored(storedName, file.getSize(), hash(target));
    }

    public Path path(String storedName) {
        Path path = dir.resolve(storedName).normalize();
        if (!path.startsWith(dir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректное имя файла");
        }
        return path;
    }

    public void verify(Book book) {
        Path path = path(book.fileName);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Файл книги отсутствует в хранилище");
        }
        String actual = hash(path);
        if (book.sha256 != null && !book.sha256.equals(actual)) {
            log.error("Контрольная сумма книги {} не совпадает: ожидалось {}, получено {}", book.id, book.sha256, actual);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Файл повреждён: контрольная сумма не совпадает");
        }
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(path(storedName));
        } catch (IOException e) {
            log.error("Не удалось удалить файл {}: {}", storedName, e.getMessage());
        }
    }

    public void copyAllTo(Path target) throws IOException {
        Files.createDirectories(target);
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                Files.copy(file, target.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static String hash(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(path));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось вычислить контрольную сумму", e);
        }
    }

    public static class Stored {
        public final String fileName;
        public final long size;
        public final String sha256;

        Stored(String fileName, long size, String sha256) {
            this.fileName = fileName;
            this.size = size;
            this.sha256 = sha256;
        }
    }
}
