package com.shell.job;

import com.shell.common.FileSplitter;
import com.shell.common.JsonConverter;
import com.shell.model.ProfileItem;
import com.shell.service.ChromeService;
import com.shell.service.ProfileManagerRepo;
import lombok.extern.java.Log;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class ShellJob {

    final ProfileManagerRepo profileManagerRepo;
    final ChromeService chromeService;

    @Value("${profile-folder.user-profile}")
    private String userProfileExtractFolder;

    public ShellJob(ProfileManagerRepo profileManagerRepo, ChromeService chromeService) {
        this.profileManagerRepo = profileManagerRepo;
        this.chromeService = chromeService;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void doShellTask() {
        var profile = profileManagerRepo.getAllProfile().stream().filter(ProfileItem::accountCanRunShell).toList();
        profile.forEach(it -> {
            try {
                var userFolderName = Paths.get(System.getProperty("user.home"), userProfileExtractFolder, it.getEmail()).toString();
                var folderProfile = new File((userFolderName));
                var totalFiles = Optional.ofNullable(folderProfile.listFiles()).map(e -> e.length).orElse(0);
                var validFolder = folderProfile.exists() && folderProfile.isDirectory() && totalFiles > 0;
                if (validFolder) {
                    log.log(Level.INFO, "cloud-shell-task >> ShellJob >> valid account: {0}", JsonConverter.convertObjectToJson(it));
                    var result = chromeService.connectGoogle(it.getEmail());
                    var updateTime = it.updateLastUpdateTime();
                    var saveProfileSuccess = profileManagerRepo.saveProfileItem(updateTime);
                    if (saveProfileSuccess) {
                        log.log(Level.INFO, "cloud-shell-task >> ShellJob >> update last active time >> email: {0}", it.getEmail());
                    }
                    if (result.cannotConnectToGoogle()) {
                        var deleteFolder = FileSplitter.deleteFolder(new File(userFolderName));
                        log.log(Level.WARNING, "cloud-shell-task >> ShellJob >> can not connect google delete folder name: {0} >> result: {1}", new Object[]{userFolderName, deleteFolder});
                    }
                } else {
                    log.log(Level.INFO, "cloud-shell-task >> ShellJob >> invalidFolder: {0}", userFolderName);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "cloud-shell-task >> ShellJob >> Exception:", e);
            }
        });

    }

    @Scheduled(fixedDelay = 3, initialDelay = 3, timeUnit = TimeUnit.MINUTES)
    void checkAliveAccount() {
        try {
            var profile = profileManagerRepo.getAllProfile().stream().filter(ProfileItem::accountAlreadyStop).toList();
            profile.forEach(it -> {
                log.log(Level.INFO, "cloud-shell-task >> checkAliveAccount >> invalid account: {0}", JsonConverter.convertObjectToJson(it));
                var clone = SerializationUtils.clone(it);
                var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                clone.setStatus("OFFLINE");
                clone.setUpdateDate(updateAt);
                profileManagerRepo.saveProfileItem(clone);
            });
        } catch (Exception e) {
            log.log(Level.WARNING, "cloud-shell-task >> checkAliveAccount >> Exception:", e);
        }
    }

}
