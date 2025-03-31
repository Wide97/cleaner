package com.marco.cleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CleanerService {

    public static final Logger log = LoggerFactory.getLogger(CleanerService.class);
    private static final Set<String> ESTENSIONI_DA_IGNORARE = Set.of(".exe", ".msi", ".ini", ".bat");

    @Value("${cleaner.downloads.path}")
    private String downloadsPath;

    @Value("${cleaner.cestino.path}")
    private String cestinoPath;

    @Value("${cleaner.giorni}")
    private int giorni;

    @Value("${cleaner.cestino.maxGiorni}")
    private int maxGiorniNelCestino;

    @Value("${cleaner.backup.path}")
    private String backupPath;

    @Value("${cleaner.backup.maxGiorni}")
    private int maxGiorniBackup;

    public void startCleaning() {
        log.info("Cleaner in azione! 🚀");

        // Pulizia backup più vecchi di 6 mesi
        cleanOldBackups();

        createDirectory(downloadsPath);
        createDirectory(cestinoPath);
        createDirectory(backupPath);

        File downloads = new File(downloadsPath);
        if (!downloads.exists() || !downloads.isDirectory()) {
            log.warn("❌ La cartella 'Downloads' non esiste o non è una directory.");
            return;
        }

        List<File> files = listAllFiles(downloads);

        if (files == null || files.isEmpty()) {
            log.info("📂 Nessun file trovato nella cartella 'Downloads'.");
            return;
        }

        for (File file : files) {
            String nome = file.getName().toLowerCase();

            // Ignora file per estensione
            for (String estensione : ESTENSIONI_DA_IGNORARE) {
                if (nome.endsWith(estensione)) {
                    log.info("🚫 Ignorato (estensione): {}", nome);
                    continue;
                }
            }
            if (file.isFile()) {
                try {
                    Instant lastModified = Instant.ofEpochMilli(file.lastModified());
                    Instant fifteenDaysAgo = Instant.now().minus(giorni, ChronoUnit.DAYS);

                    if (lastModified.isBefore(fifteenDaysAgo)) {
                        Path target = new File(cestinoPath + "/" + file.getName()).toPath();
                        Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                        log.info("🧹 Spostato nel Cestino: " + file.getName());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Errore con il file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

        log.info("🧼 Avvio della pulizia del cestino personalizzato...");

        File[] cestinoFiles = new File(cestinoPath).listFiles();
        if (cestinoFiles != null) {
            for (File file : cestinoFiles) {
                String nome = file.getName().toLowerCase();

                // Ignora file per estensione
                for (String estensione : ESTENSIONI_DA_IGNORARE) {
                    if (nome.endsWith(estensione)) {
                        log.info("🚫 Ignorato (estensione): {}", nome);
                        continue;
                    }
                }
                if (file.isFile()) {
                    Instant lastModified = Instant.ofEpochMilli(file.lastModified());
                    Instant limite = Instant.now().minus(maxGiorniNelCestino, ChronoUnit.DAYS);

                    if (lastModified.isBefore(limite)) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            log.info("🗑️ Eliminato definitivamente dal cestino: {}", file.getName());
                        } else {
                            log.warn("⚠️ Non sono riuscito a eliminare: {}", file.getName());
                        }
                    }
                }
            }
        }
    }

    // Metodo che pulisce i file di backup più vecchi di 6 mesi
    public void cleanOldBackups() {
        log.info("🧹 Avvio della pulizia dei backup...");

        File backupDir = new File(backupPath);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            log.warn("❌ La cartella di backup non esiste o non è una directory.");
            return;
        }

        File[] backupFiles = backupDir.listFiles();
        if (backupFiles != null) {
            for (File file : backupFiles) {
                Instant lastModified = Instant.ofEpochMilli(file.lastModified());
                Instant limitDate = Instant.now().minus(maxGiorniBackup, ChronoUnit.DAYS);

                // Se il file è più vecchio di 6 mesi, elimina il file
                if (lastModified.isBefore(limitDate)) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.info("🗑️ File di backup eliminato: {}", file.getName());
                    } else {
                        log.warn("⚠️ Impossibile eliminare il file di backup: {}", file.getName());
                    }
                }
            }
        }
    }

    @Scheduled(cron = "0 0 9 * * *") // ogni giorno alle 9:00
    public void scheduledCleaning() {
        log.info("🕘 Avvio automatico della pulizia programmata");
        startCleaning();
    }

    // Metodo per ottenere tutti i file (inclusi quelli nelle sottocartelle)
    private List<File> listAllFiles(File dir) {
        List<File> files = new ArrayList<>();
        File[] entries = dir.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    files.addAll(listAllFiles(entry)); // 👈 Ricorsione per sottocartelle
                } else {
                    files.add(entry);
                }
            }
        }
        return files;
    }

    // Metodo per creare le directory se non esistono
    private void createDirectory(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("✅ Cartella creata con successo in: {}", path);
            } else {
                log.warn("❌ Impossibile creare la cartella in: {}", path);
            }
        } else {
            log.info("📂 La cartella esiste già: {}", path);
        }
    }
}

