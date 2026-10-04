package com.library.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    public static final long MAX_SIZE = 50L * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String title;

    public String author;

    public String description;

    public String fileName;

    public String originalName;

    public String format;

    public long size;

    public String sha256;

    public String uploadedAt = java.time.LocalDateTime.now().toString();
}
