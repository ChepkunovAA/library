package com.library.controller;

import com.library.service.BookService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService books;

    public BookController(BookService books) {
        this.books = books;
    }

    @GetMapping
    public List<BookService.BookDto> list(@RequestParam(required = false) String title) {
        return books.view(title);
    }

    @GetMapping("/{id}")
    public BookService.BookDto one(@PathVariable Long id) {
        return books.viewOne(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookService.BookDto upload(@RequestParam String title,
                                      @RequestParam String author,
                                      @RequestParam(required = false) String description,
                                      @RequestParam("file") MultipartFile file) {
        return books.upload(title, author, description, file);
    }

    @PutMapping("/{id}/like")
    public BookService.BookDto like(@PathVariable Long id) {
        return books.toggleLike(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        BookService.Download download = books.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.filename, StandardCharsets.UTF_8).build().toString())
                .body(download.resource);
    }
}
