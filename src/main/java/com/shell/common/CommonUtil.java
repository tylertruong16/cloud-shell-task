package com.shell.common;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

@UtilityClass
@Log
public class CommonUtil {

    public String getServerIP() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream().map(NetworkInterface::getInetAddresses).map(Collections::list).flatMap(List::stream)
                    .filter(ip -> ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && !ip.isLinkLocalAddress() && !ip.getHostAddress().contains(":"))
                    .findFirst().map(InetAddress::getHostAddress).orElseThrow(() -> new RuntimeException("Cannot determine server IP address"));
        } catch (IOException ex) {
            log.log(Level.SEVERE, "cloud-shell-task >> CommonUtil >> getServerIP >> Cannot determine server IP address", ex);
            return "";
        }
    }

    public static List<String> getAllFolderNames() {
        var userHome = System.getProperty("user.home");
        var chromeProfilesPath = MessageFormat.format("{0}/{1}", userHome, "chrome-profiles");
        var chromeProfilesDir = new File(chromeProfilesPath);
        if (chromeProfilesDir.exists() && chromeProfilesDir.isDirectory()) {
            return Arrays.stream(Objects.requireNonNull(chromeProfilesDir.listFiles()))
                    .filter(File::isDirectory)
                    .map(File::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static boolean renameFolder(String oldFolderName, String newFolderName) {
        var baseDirectoryPath = System.getProperty("user.home") + File.separator + "chrome-profiles";
        var oldFolder = new File(baseDirectoryPath + File.separator + oldFolderName);
        var newFolder = new File(baseDirectoryPath + File.separator + newFolderName);
        // Rename the folder
        var success = oldFolder.renameTo(newFolder);
        if (!success) {
            log.log(Level.SEVERE, "cloud-shell-task >> CommonUtil >> can not rename the folder from: {0} >> to: {1}", new Object[]{oldFolder.getAbsolutePath(), newFolder.getAbsolutePath()});
        }
        return success;
    }


    public static boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            var files = folder.listFiles();
            if (files != null) {
                for (var file : files) {
                    // Recursive call to delete files and subfolders
                    deleteFolder(file);
                }
            }
        }
        // Delete the file or empty folder
        return folder.delete();
    }

    public static boolean deleteFolderByName(String folderName) {
        var baseDirectoryPath = System.getProperty("user.home") + File.separator + "chrome-profiles";
        var folder = new File(baseDirectoryPath + File.separator + folderName);
        return deleteFolder(folder);
    }

}
