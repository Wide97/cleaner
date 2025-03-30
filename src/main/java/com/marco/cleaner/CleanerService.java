package com.marco.cleaner;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class CleanerService {

    private static final String DOWNLOADS_PATH = "src/main/resources/test-downloads";
    private static final String CESTINO_PATH = "src/main/resources/test-cestino";

    public void startCleaning() {
        System.out.println("Cleaner in azione! 🚀");

        File cestino = new File(CESTINO_PATH);
        if (!cestino.exists()) {
            boolean created = cestino.mkdirs();
            System.out.println(created ? "✅ Cartella 'Cestino' creata." : "❌ Errore nella creazione della cartella 'Cestino'.");
        }

        File downloads = new File(DOWNLOADS_PATH);
        if (!downloads.exists() || !downloads.isDirectory()) {
            System.out.println("❌ La cartella 'Downloads' non esiste o non è una directory.");
            return;
        }

        File[] files = downloads.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("📂 Nessun file trovato nella cartella 'Downloads'.");
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                try {
                    Instant lastModified = Instant.ofEpochMilli(file.lastModified());
                    Instant fifteenDaysAgo = Instant.now().minus(15, ChronoUnit.DAYS);

                    if (lastModified.isBefore(fifteenDaysAgo)) {
                        Path target = new File(CESTINO_PATH + "/" + file.getName()).toPath();
                        Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("🧹 Spostato nel Cestino: " + file.getName());
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Errore con il file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }


}

