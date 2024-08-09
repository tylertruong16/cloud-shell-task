package com.shell.service;

import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;

@Service
@Log
public class ChromeService {

    private static final String GOOGLE_ACCOUNT_PAGE = "https://accounts.google.com";
    private static final String GOOGLE_SHELL_PAGE = "https://console.cloud.google.com";
    private static final String SHELL_FRAME_NAME = "cloudshell-frame";
    @Value("${system.cmd}")
    private String cmd;


    public void connectGoogle(String email) {
        var options = createProfile(email, new ChromeOptions());
        var driver = new ChromeDriver(options);
        var cmdValue = MessageFormat.format("{0}\n", StringUtils.defaultIfBlank(cmd, "").trim());
        try {
            driver.get(GOOGLE_ACCOUNT_PAGE);
            Thread.sleep(Duration.ofSeconds(5));
            var loginSuccess = loginSuccess(driver);
            if (loginSuccess) {
                log.log(Level.INFO, "cloud-shell-task >> ChromeService >> connectGoogle >> email: {0} >> title: {1}", new Object[]{email, driver.getTitle()});
                driver.get(GOOGLE_SHELL_PAGE);
                var wait = new WebDriverWait(driver, Duration.ofSeconds(30));
                var cloudShellIcon = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@aria-label='Activate Cloud Shell']")));
                cloudShellIcon.click();
                var iframe = driver.findElement(By.className(SHELL_FRAME_NAME));
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(SHELL_FRAME_NAME)));
                // Switch to the Cloud Shell iframe
                driver.switchTo().frame(iframe);


                log.log(Level.INFO, "cloud-shell-task >> ChromeService >> connectGoogle >> email: {0} >> title: {1}", new Object[]{email, driver.getTitle()});
                if (isDisplayTermPopUp(driver)) {
                    // Wait for the shell command prompt to be ready
                    inputCommandToShell(driver, cmdValue);
                    // Send a command to the Cloud Shell
                    handleAuthorizeShell(driver);

                }
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));

            }
        } catch (Exception e) {
            log.log(Level.WARNING, "cloud-shell-task >> ChromeService >> connectGoogle >> Exception:", e);
        } finally {
            driver.close();
        }
    }

    public void inputCommandToShell(WebDriver driver, String command) {
        var wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofMinutes(1))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(NoSuchElementException.class);
        var shellSelector = ".terminal .xterm-helper-textarea";

        var isShellDisplayCondition = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement element = driver.findElement(By.cssSelector(shellSelector));
                    String accessibleName = element.getAccessibleName(); // or element.getText() if you want the visible text
                    return StringUtils.endsWithIgnoreCase(accessibleName, "Terminal input");
                } catch (NoSuchElementException e) {
                    return false; // Profile icon not found, not signed in
                }
            }
        };
        var shellDisplay = wait.until(isShellDisplayCondition);
        if (shellDisplay) {
            var elementShell = driver.findElement(By.cssSelector(shellSelector));
            var textInsideShell = elementShell.getText();
            log.log(Level.INFO, "cloud-shell-task >> inputCommandToShell >> textInsideShell: {0}", textInsideShell);
            elementShell.sendKeys(command); // Example command: listing gcloud configurations
        }

    }

    public boolean isDisplayTermPopUp(WebDriver driver) {
        var wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);
        var dialog = wait.until(driver1 -> {
            WebElement element = driver1.findElement(By.cssSelector("mat-dialog-container"));
            if (element.isDisplayed()) {
                return element;
            }
            return null;
        });
        var termsLink = dialog.findElement(By.cssSelector("[article='GCP_TERMS_OF_SERVICE']"));
        var popupDisplay = Optional.ofNullable(termsLink).isPresent();
        if (popupDisplay) {
            var textInsidePopUp = dialog.getText();
            log.log(Level.INFO, "cloud-shell-task >> ChromeService >> isDisplayTermPopUp >> textInsidePopUp: {0}", textInsidePopUp);
            confirmPopUp(driver);

        }
        return popupDisplay;
    }

    public void handleAuthorizeShell(WebDriver driver) {
        try {
            var wait = new FluentWait<>(driver)
                    .withTimeout(Duration.ofMinutes(1))
                    .pollingEvery(Duration.ofSeconds(5))
                    .ignoring(NoSuchElementException.class);
            var checkAuthorizeShell = new Function<WebDriver, Boolean>() {
                public Boolean apply(WebDriver driver) {
                    try {
                        return driver.findElements(By.cssSelector(".mat-mdc-button")).stream().anyMatch(it -> it.getText().equals("Authorize"));
                    } catch (NoSuchElementException e) {
                        return false; // Profile icon not found, not signed in
                    }
                }
            };
            var pupUpShow = wait.until(checkAuthorizeShell);
            if (pupUpShow) {
                var popUpText = driver.findElement(By.className("mat-mdc-dialog-content")).getText();
                log.log(Level.INFO, "cloud-shell-task >> handleAuthorizeShell >> popUpText: {0}", popUpText);
                // Cloud Shell needs permission to use your credentials for the gcloud CLI command
                driver.findElements(By.cssSelector(".mat-mdc-button")).stream().filter(it -> it.getText().equals("Authorize")).findFirst().ifPresent(WebElement::click);
            }
        } catch (Exception e) {
            log.log(Level.INFO, "cloud-shell-task >> handleAuthorizeShell >> Exception: {0}", e.getMessage());
        }

    }

    private static void confirmPopUp(WebDriver driver) {
        var checkbox = driver.findElement(By.id("mat-mdc-checkbox-1-input"));
        checkbox.click();
        // Locate the submit button and click on it
        var submitButton = driver.findElement(By.xpath("//button[span[contains(text(),'Start Cloud Shell')]]"));
        submitButton.click();
    }


    private boolean loginSuccess(ChromeDriver driver) {
        var wait = new FluentWait<WebDriver>(driver)
                .withTimeout(Duration.ofMinutes(5))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(NoSuchElementException.class);


        // Define the condition to check for the profile icon
        var checkLogin = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    driver.findElement(By.xpath("//a[contains(@href, 'accounts.google.com/SignOutOptions')]"));
                    return true; // Profile icon found, already signed in
                } catch (NoSuchElementException e) {
                    return false; // Profile icon not found, not signed in
                }
            }
        };
        return wait.until(checkLogin);
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


}
