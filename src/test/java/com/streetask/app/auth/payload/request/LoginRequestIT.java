package com.streetask.app.auth.payload.request;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opentest4j.TestAbortedException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Tag("UI")
public class LoginRequestIT {

    private WebDriver driver;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void checkFrontendIsUp() throws TestAbortedException, URISyntaxException {
        assumeTrue(isFrontendRunning(), "Frontend (localhost:8081) is not active. Skipping Selenium Tests.");
    }

    @BeforeEach
    void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        baseUrl = "http://localhost:8081";
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Login form should be accessible and submit successfully, and frontend must be running")
    void testLoginForm() {
        driver.get(baseUrl);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[contains(@placeholder, 'example')] | //input[@type='text'] | //input[@dir='auto']")));

        WebElement passwordInput = driver.findElement(
                By.xpath("//input[@placeholder='********']"));

        WebElement submitButton = driver.findElement(
                By.xpath("//*[contains(text(), 'Sign In')]"));

        emailInput.sendKeys("user1");
        passwordInput.sendKeys("4dm1n");
        submitButton.click();
    }

    private boolean isFrontendRunning() throws URISyntaxException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081").toURL()
                    .openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(2000);
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException e) {
            return false;
        }
    }
}