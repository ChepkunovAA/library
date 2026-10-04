package com.library.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbc;
    private volatile Map<String, Object> availability = new LinkedHashMap<>();

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return availability;
    }

    @Scheduled(fixedDelayString = "${app.monitor.interval-ms:60000}", initialDelayString = "${app.monitor.interval-ms:60000}")
    public void monitor() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("checkedAt", LocalDateTime.now().toString());
        try {
            jdbc.queryForObject("select 1", Integer.class);
            report.put("database", "UP");
        } catch (Exception e) {
            report.put("database", "DOWN");
            log.error("Мониторинг: база данных недоступна: {}", e.getMessage());
        }
        availability = report;
    }
}
