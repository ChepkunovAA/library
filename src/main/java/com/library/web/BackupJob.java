package com.library.web;

import com.library.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class BackupJob {

    private static final Logger log = LoggerFactory.getLogger(BackupJob.class);

    private final StorageService storage;
    private final Path backupDir;
    private final String pgDump;
    private final String dbHost;
    private final String dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;

    public BackupJob(StorageService storage,
                     @Value("${app.backup.dir}") String backupDir,
                     @Value("${app.backup.pg-dump}") String pgDump,
                     @Value("${app.backup.db-host}") String dbHost,
                     @Value("${app.backup.db-port}") String dbPort,
                     @Value("${app.backup.db-name}") String dbName,
                     @Value("${app.backup.db-user}") String dbUser,
                     @Value("${app.backup.db-password}") String dbPassword) {
        this.storage = storage;
        this.backupDir = Paths.get(backupDir).toAbsolutePath();
        this.pgDump = pgDump;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    @Scheduled(cron = "${app.backup.cron}", zone = "${app.backup.zone:Europe/Moscow}")
    public void run() {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path target = backupDir.resolve(stamp);
        try {
            Files.createDirectories(target);
            storage.copyAllTo(target.resolve("files"));
            dumpDatabase(target.resolve("database.dump").toString());
            log.info("Резервная копия создана: {}", target);
        } catch (Exception e) {
            log.error("Резервное копирование не выполнено: {}", e.getMessage(), e);
        }
    }

    private void dumpDatabase(String dumpFile) throws Exception {
        List<String> command = new ArrayList<>(List.of(pgDump,
                "-h", dbHost, "-p", dbPort, "-U", dbUser, "-Fc", "-f", dumpFile, dbName));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("PGPASSWORD", dbPassword);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        if (process.waitFor() != 0) {
            throw new IllegalStateException("pg_dump завершился с ошибкой: " + output);
        }
    }
}
