package com.shell.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.ToIntBiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@UtilityClass
@Log
public class FileSplitter {

    private static final String KEY = "03TnhH9pyFnOJhYhVxqnwl5JsiVfIl3N";

    @Data
    public static class FileZipDetail {
        private int totalPart;
        private String md5CheckSum;
    }

    @SneakyThrows
    public static FileZipDetail splitFile(String sourceFilePath, int partSizeMB) {
        var sourceFile = new File(sourceFilePath);
        long fileSize = sourceFile.length();
        long partSize = (long) partSizeMB * 1024 * 1024;

        int partCount = (int) (fileSize / partSize) + (fileSize % partSize == 0 ? 0 : 1);
        var fileZipDetail = new FileZipDetail();
        fileZipDetail.setTotalPart(partCount);
        var md5Digest = MessageDigest.getInstance("MD5");
        var folderPath = sourceFile.getParent();
        var fileName = sourceFile.getName();

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile))) {
            for (int i = 0; i < partCount; i++) {
                var newFileName = Base64.encodeBase64URLSafeString(AESUtil.encrypt(fileName + ".part" + i, KEY).getBytes());
                var partFileName = folderPath + File.separator + newFileName;
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(partFileName))) {
                    long bytesWritten = 0;
                    byte[] buffer = new byte[8192];
                    while (bytesWritten < partSize && bis.available() > 0) {
                        int bytesRead = bis.read(buffer);
                        bos.write(buffer, 0, bytesRead);
                        bytesWritten += bytesRead;

                        // Update MD5 checksum
                        md5Digest.update(buffer, 0, bytesRead);
                    }
                }
            }
        }
        // Set the MD5 checksum in the fileZipDetail object
        byte[] md5Bytes = md5Digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : md5Bytes) {
            sb.append(String.format("%02x", b));
        }
        fileZipDetail.setMd5CheckSum(sb.toString());
        return fileZipDetail;
    }

    @SneakyThrows
    public static void mergeFiles(String partFilePath, String destinationFilePath) {
        var partFileFolder = new File(partFilePath);
        var directory = partFileFolder.isDirectory() ? partFileFolder : partFileFolder.getParentFile();
        var fileName = new File(destinationFilePath).getName();
        var zipHint = fileName + ".part";

        var fileParts = Arrays.stream(Optional.ofNullable(directory.listFiles())
                        .orElse(new File[]{}))
                .filter(it -> Base64.isBase64(it.getName()))
                .map(it -> {
                    var name = it.getName();
                    var decode = new String(Base64.decodeBase64(name), StandardCharsets.UTF_8);
                    var oldFileName = AESUtil.decrypt(decode, KEY);
                    return new FileNum(it, oldFileName, NumberUtils.toInt(oldFileName.replace(zipHint, ""), 0));
                }).sorted(Comparator.comparing(FileNum::getNo)).toList();

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destinationFilePath))) {
            for (var partFile : fileParts) {
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(partFile.getFile()))) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    public static void unzip(String zipFile, String destFolder) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destFolder + File.separator + entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
            }
        }
    }



    @Data
    @AllArgsConstructor
    public static class FileNum {
        private File file;
        private String fileName;
        private int no;
    }


}
