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


    public void startCleaning() {
        log.info("Cleaner in azione! 🚀");

        File cestino = new File(cestinoPath);
        if (!cestino.exists()) {
            boolean created = cestino.mkdirs();
            log.info(created ? "✅ Cartella 'Cestino' creata." : "❌ Errore nella creazione della cartella 'Cestino'.");
        }

        File downloads = new File(downloadsPath);
        if (!downloads.exists() || !downloads.isDirectory()) {
            log.warn("❌ La cartella 'Downloads' non esiste o non è una directory.");
            return;
        }

        File[] files = downloads.listFiles();
        if (files == null || files.length == 0) {
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


    @Scheduled(cron = "0 0 9 * * *") // ogni giorno alle 9:00
    public void scheduledCleaning() {
        log.info("🕘 Avvio automatico della pulizia programmata");
        startCleaning();
    }


}

