package com.shell.github.service;

import com.shell.common.HttpUtil;
import com.shell.common.JsonConverter;
import com.shell.github.model.ContentResponse;
import com.shell.github.model.GitHubConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.springframework.http.HttpHeaders;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.logging.Level;

@UtilityClass
@Log
public class GitHubService {


    private static HttpHeaders getHttpHeaders(String token) {
        var header = HttpUtil.getHeaderPostRequest();
        header.add("Authorization", "Bearer " + token);
        header.add("Content-Type", "application/vnd.github+json");
        header.add("X-GitHub-Api-Version", "2022-11-28");
        return header;
    }


    public List<ContentResponse> getAllFileUnderFolder(String folderName, GitHubConfig gitHubConfig) {
        var url = MessageFormat.format("{0}/{1}", gitHubConfig.getApiUrl(), folderName);
        var header = getHttpHeaders(gitHubConfig.getApiKey());
        var request = new HttpGet(url);
        var response = HttpUtil.sendRequest(url, request, header);
        if (response.getStatusCode().value() == 404) {
            return new ArrayList<>();
        }
        var responseBody = response.getBody();
        return Arrays.stream(JsonConverter.convertToObject(responseBody, ContentResponse[].class).orElse(new ContentResponse[]{})).toList();
    }


    @SneakyThrows
    public void downloadFile(String targetFolder, String email, GitHubConfig gitHubConfig) {
        var fileNames = getAllFileUnderFolder(email, gitHubConfig);
        var folder = new File(targetFolder);
        if (!folder.exists()) {
            var folderCreation = folder.mkdirs();
            log.log(Level.INFO, "cloud-shell-task >> GitHubService >> downloadFile >> create folder: {0} >> response: {1}", new Object[]{targetFolder, folderCreation});
        }
        if (CollectionUtils.isNotEmpty(fileNames)) {
            var param = fileNames.stream().map(it -> new DownLoadCallable(it.getName(), targetFolder, it.getDownloadUrl())).toList();
            try (var task = Executors.newFixedThreadPool(10)) {
                var result = task.invokeAll(param)
                        .stream().map(it -> {
                            try {
                                return it.get();
                            } catch (Exception e) {
                                return 0;
                            }
                        }).toList();
                log.log(Level.INFO, "cloud-shell-task >> GitHubService >> downloadFile >> email: {0} >> response: {1}", new Object[]{email, JsonConverter.convertListToJson(result)});
            }


        }
    }


    @Data
    @AllArgsConstructor
    @Log
    public static class DownLoadCallable implements Callable<Integer> {
        private String fileName;
        private String outputFolder;
        private String url;

        @Override
        public Integer call() {
            var targetFile = outputFolder + File.separator + fileName;
            HttpGet request = new HttpGet(url);
            var response = HttpUtil.downloadFile(targetFile, request, HttpUtil.getHeaderPostRequest());
            var code = response.getStatusCode().value();
            log.log(Level.INFO, "cloud-shell-task >> GitHubService >> downloadFile >> url: {0} >> folder: {1} >> responseCode: {2}", new Object[]{url, targetFile, code});
            return code;
        }
    }

}
