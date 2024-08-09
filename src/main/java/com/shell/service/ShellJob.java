package com.shell.service;

import com.shell.common.JsonConverter;
import com.shell.model.ProfileItem;
import lombok.extern.java.Log;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.MINUTES)
    void doShellTask() {
        var profile = profileManagerRepo.getAllProfile().stream().filter(ProfileItem::accountCanRunShell).toList();
        profile.forEach(it -> {
            try {
                log.log(Level.INFO, "cloud-shell-task >> ShellJob >> valid account: {0}", JsonConverter.convertObjectToJson(it));
                chromeService.connectGoogle(it.getEmail());
                var updateTime = it.updateLastUpdateTime();
                var saveProfileSuccess = profileManagerRepo.saveProfileItem(updateTime);
                if(saveProfileSuccess){
                    log.log(Level.INFO, "cloud-shell-task >> ShellJob >> update last active time >> email: {0}", it.getEmail());
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "cloud-shell-task >> ShellJob >> Exception:", e);
            }
        });

    }

}
