package com.library.controller;

import com.library.service.ModerationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation/books")
public class ModerationController {

    private final ModerationService moderation;

    public ModerationController(ModerationService moderation) {
        this.moderation = moderation;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        moderation.deleteBook(id);
    }
}
