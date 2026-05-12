package com.streetask.app.ui;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class UserFlowUITest {

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // Uncomment the next line if your device does not have Chrome installed
        // options.addArguments("--headless");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void performLogin(String email, String password) {
        driver.get("http://localhost:8081/");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[placeholder='you@example.com']")));
            emailInput.clear();
            emailInput.sendKeys(email);

            WebElement passwordInput = driver.findElement(By.cssSelector("input[placeholder='********']"));
            passwordInput.clear();
            passwordInput.sendKeys(password);

            WebElement signInButton = driver.findElement(By.xpath("//div[contains(text(), 'Sign In')]"));
            signInButton.click();

            wait.until(ExpectedConditions.invisibilityOf(signInButton));

        } catch (TimeoutException e) {
            System.out.println("Error en Login. URL actual: " + driver.getCurrentUrl());
            throw e;
        }
    }

    @Test
    @Disabled("Manual E2E test: Requires the backend to be OFF and the frontend to be running on port 8081")
    @DisplayName("Login flow recorded with TestCase Studio")
    void testLoginFlow() {
        performLogin("user1", "4dm1n");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        boolean isMenuIconPresent = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(text(),'')]"))).isDisplayed();

        assertThat(isMenuIconPresent).isTrue();
    }

    @Test
    @Disabled("Manual E2E test: Requires the backend to be OFF and the frontend to be running on port 8081")
    @DisplayName("Create question flow recorded with TestCase Studio")
    void testCreateQuestionFlow() throws InterruptedException {
        performLogin("premium1", "4dm1n");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement askButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[normalize-space()='Ask a question']")));
        askButton.click();

        WebElement pickOnMapButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[normalize-space()='Pick on map']")));
        pickOnMapButton.click();

        Thread.sleep(2000);

        WebElement confirmButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(text(),'Confirm')]")));
        confirmButton.click();

        WebElement topicInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[placeholder='What is this about?']")));
        topicInput.sendKeys("Topic");

        WebElement questionInput = driver
                .findElement(By.cssSelector("textarea[placeholder='Type your question here...']"));
        questionInput.sendKeys("Question");

        WebElement postButton = driver.findElement(By.xpath("//div[contains(text(),'Post Question')]"));
        postButton.click();

        assertThat(driver.getCurrentUrl()).isEqualTo("http://localhost:8081/");
    }

    @Test
    @Disabled("Manual E2E test: Requires the backend to be OFF and the frontend to be running on port 8081")
    @DisplayName("Answer question flow recorded with TestCase Studio")
    void testAnswerQuestionFlow() {
        performLogin("premium1", "4dm1n");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement menuIcon = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(text(),'')]")));
        menuIcon.click();

        WebElement questionCard = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(text(),'Do you like someone')]")));
        questionCard.click();

        WebElement answerInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[placeholder='Write your answer...']")));
        answerInput.sendKeys("Hello");

        answerInput.sendKeys(Keys.TAB, Keys.ENTER);

        wait.until(ExpectedConditions.attributeToBe(answerInput, "value", ""));
        assertThat(driver.getCurrentUrl()).isEqualTo("http://localhost:8081/");
    }
}