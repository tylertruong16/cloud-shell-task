package com.shell.job;

import com.shell.common.JsonConverter;
import com.shell.model.ProfileItem;
import com.shell.service.ChromeService;
import com.shell.service.ProfileManagerRepo;
import lombok.extern.java.Log;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class ShellJob {

    final ProfileManagerRepo profileManagerRepo;
    final ChromeService chromeService;

    public ShellJob(ProfileManagerRepo profileManagerRepo, ChromeService chromeService) {
        this.profileManagerRepo = profileManagerRepo;
        this.chromeService = chromeService;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void doShellTask() {
        var profile = profileManagerRepo.getAllProfile().stream().filter(ProfileItem::accountCanRunShell).toList();
        profile.forEach(it -> {
            try {
                var userFolderName = Paths.get(System.getProperty("user.home"), "chrome-profiles-download-extract", it.getEmail()).toString();
                var folderProfile = new File((userFolderName));
                var totalFiles = Optional.ofNullable(folderProfile.listFiles()).map(e -> e.length).orElse(0);
                var validFolder = folderProfile.exists() && folderProfile.isDirectory() && totalFiles > 0;
                if (validFolder) {
                    log.log(Level.INFO, "cloud-shell-task >> ShellJob >> valid account: {0}", JsonConverter.convertObjectToJson(it));
                    chromeService.connectGoogle(it.getEmail());
                    var updateTime = it.updateLastUpdateTime();
                    var saveProfileSuccess = profileManagerRepo.saveProfileItem(updateTime);
                    if (saveProfileSuccess) {
                        log.log(Level.INFO, "cloud-shell-task >> ShellJob >> update last active time >> email: {0}", it.getEmail());
                    }
                } else {
                    log.log(Level.INFO, "cloud-shell-task >> ShellJob >> invalidFolder: {0}", userFolderName);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "cloud-shell-task >> ShellJob >> Exception:", e);
            }
        });

    }

}
