package com.shell.common;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;

@UtilityClass
@Log
public class FileUtil {

    @SneakyThrows
    public static void copyFolder(Path sourceFolder, Path targetFolder) {
        // Copy folder and its contents
        Files.walkFileTree(sourceFolder, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                var targetDir = targetFolder.resolve(sourceFolder.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectory(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                var targetFile = targetFolder.resolve(sourceFolder.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        log.log(Level.INFO, "cloud-shell-task >> FileUtil >> copied successfully >> from: {0} >> to: {1} ", new Object[]{sourceFolder, targetFolder});
    }

}
