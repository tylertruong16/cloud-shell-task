package com.shell.service;

import com.shell.common.HttpUtil;
import com.shell.common.JsonConverter;
import com.shell.model.ProfileItem;
import lombok.extern.java.Log;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Service
@Log
public class ChromeService {

    @Value("${system.id}")
    private String headerKey;

    @Value("${system.profile-table-url}")
    private String profileTableUrl;

    public ChromeService() {
    }


    public void preventCloseTab(ChromeDriver driver) {
        var script = """
                    document.addEventListener('keydown', function(event) {
                        // Prevent opening a new tab with Ctrl+T or Ctrl+N
                        if ((event.ctrlKey && (event.key === 't' || event.key === 'n')) ||
                            // Prevent closing the current tab with Ctrl+W or Ctrl+F4
                            (event.ctrlKey && (event.key === 'w' || event.key === 'F4')) ||
                            // Prevent opening a new window with Ctrl+N
                            (event.ctrlKey && event.key === 'N') ||
                            // Prevent opening a new incognito window with Ctrl+Shift+N
                            (event.ctrlKey && event.shiftKey && event.key === 'N') ||
                            // Prevent closing the window with Alt+F4
                            (event.altKey && event.key === 'F4') ||
                            // Prevent switching between windows with Alt+Tab
                            (event.altKey && event.key === 'Tab') ||
                            // Prevent opening the system menu with F10
                            (event.key === 'F10') ||
                            // Prevent opening the context menu with Shift+F10
                            (event.shiftKey && event.key === 'F10') ||
                            // Prevent closing or minimizing the window with Ctrl+F4
                            (event.ctrlKey && event.key === 'F4') ||
                            // Prevent reopening the most recently closed tab with Ctrl+Shift+T
                            (event.ctrlKey && event.shiftKey && event.key === 'T')) {
                            event.preventDefault();
                        }
                    });
                """;
        var jsExecutor = (JavascriptExecutor) driver;
        jsExecutor.executeScript(script);
    }

    public ChromeOptions createProfile(String folderName, ChromeOptions options) {
        try {
            var profilePath = Paths.get(System.getProperty("user.home"), "chrome-profiles", folderName).toString();
            options.addArguments(MessageFormat.format("user-data-dir={0}", profilePath));
            options.addArguments("--disable-web-security");
            // this option so important to bypass google detection
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);
            options.addArguments("--disable-blink-features=AutomationControlled");

            return options;
        } catch (Exception e) {
            log.log(Level.WARNING, "cloud-shell-task >> ChromeService >> createProfile >> Exception:", e);
        }
        return options;
    }


    public String findEmail(ChromeDriver driver) {
        var input = driver.findElement(By.xpath("//a[contains(@href, 'accounts.google.com/SignOutOptions')]")).getAttribute("aria-label");
        var emailPattern = "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})";
        var pattern = Pattern.compile(emailPattern);
        var matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }


    void saveProfileDatabase(String email) {
        var header = HttpUtil.getHeaderPostRequest();
        header.add("realm", headerKey);
        var profile = ProfileItem.createOfflineProfile(email);
        var json = JsonConverter.convertObjectToJson(profile);
        var response = HttpUtil.sendPostRequest(profileTableUrl, json, header);
        var body = response.getBody();
        log.log(Level.INFO, "cloud-shell-task >> saveProfileDatabase >> header: {0} >> json: {1} >> url: {2} >> response: {3}",
                new Object[]{headerKey, json, profileTableUrl, body});

    }


}
