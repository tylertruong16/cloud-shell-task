package com.shell.job;

import com.shell.common.FileSplitter;
import com.shell.github.model.GitHubConfig;
import com.shell.github.service.GitHubService;
import com.shell.model.ProfileItem;
import com.shell.service.ProfileManagerRepo;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class SyncProfileJob {

    @Value("${github.api-url}")
    private String githubApiUrl;
    @Value("${github.token}")
    private String githubToken;

    @Value("${profile-folder.user-profile-download}")
    private String chromeProfileDownloadFolder;

    @Value("${profile-folder.user-profile}")
    private String userProfileExtractFolder;

    final ProfileManagerRepo profileManagerRepo;

    public SyncProfileJob(ProfileManagerRepo profileManagerRepo) {
        this.profileManagerRepo = profileManagerRepo;
    }


    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    void collectProfile() {
        try {
            var config = new GitHubConfig(this.githubApiUrl, this.githubToken.trim());
            var profilePath = Paths.get(System.getProperty("user.home"), chromeProfileDownloadFolder).toString();
            var folder = new File(profilePath);
            if (!folder.exists()) {
                var created = folder.mkdirs();
                log.log(Level.INFO, "cloud-shell-task >> collectProfile >> create folder >> path: {0} >> result: {1}", new Object[]{folder.getAbsolutePath(), created});
            }
            var validAccounts = profileManagerRepo.getAllProfile()
                    .stream().filter(ProfileItem::validDownloadUrl)
                    .toList();
            validAccounts.forEach(it -> {
                try {
                    var userFolderDownLoadName = Paths.get(System.getProperty("user.home"), chromeProfileDownloadFolder, it.getEmail()).toString();
                    var userFolderProfile = Paths.get(System.getProperty("user.home"), userProfileExtractFolder, it.getEmail()).toString();
                    var userFolder = new File(userFolderProfile);
                    if (!userFolder.exists() || Optional.ofNullable(userFolder.listFiles()).filter(e -> e.length == 0).isPresent()) {
                        // download folder when it does not exist
                        GitHubService.downloadFile(userFolderDownLoadName, it.getEmail(), config);
                        var zipFileName = userFolderDownLoadName + File.separator + MessageFormat.format("{0}.zip", it.getEmail());
                        FileSplitter.mergeFiles(userFolderDownLoadName, zipFileName);
                        var fileZip = new File(zipFileName);
                        if (fileZip.exists()) {
                            var extractFolder = Paths.get(System.getProperty("user.home"), userProfileExtractFolder).toString();
                            FileSplitter.unzip(zipFileName, extractFolder);
                        }
                    }
                } catch (Exception e) {
                    log.log(Level.INFO, MessageFormat.format("cloud-shell-task >> collectProfile >> email: {0} >> Exception:", it.getEmail()), e);
                }

            });
        } catch (Exception e) {
            log.log(Level.WARNING, "cloud-shell-task >> collectProfile >> Exception:", e);
        }
    }

}
