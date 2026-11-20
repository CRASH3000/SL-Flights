package ru.qa.flights.support;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.io.ByteArrayInputStream;

public abstract class BrowserTest {
    protected final TestSettings settings = TestSettings.load();
    protected WebDriver driver;

    @RegisterExtension
    final AfterTestExecutionCallback attachScreenshotOnFailure = context -> {
        if (!(driver instanceof TakesScreenshot browser) || context.getExecutionException().isEmpty()) {
            return;
        }

        try {
            byte[] screenshot = browser.getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment("Скриншот при сбое", "image/png", new ByteArrayInputStream(screenshot), ".png");
        } catch (WebDriverException error) {
            Allure.addAttachment("Не удалось сделать скриншот", error.getMessage());
        }
    };

    @BeforeEach
    void startBrowser() {
        String browser = browserName();
        Allure.parameter("Браузер", browser);
        Allure.label("parentSuite", browser);
        Allure.getLifecycle().updateTestCase(result -> result.setHistoryId(result.getHistoryId() + "-" + browser));

        driver = BrowserFactory.create(browser, settings);
        driver.manage().timeouts().pageLoadTimeout(settings.pageLoadTimeout());
        if (driver instanceof HasCapabilities session) {
            Allure.parameter("Версия браузера", session.getCapabilities().getBrowserVersion());
        }
    }

    protected String browserName() {
        return settings.browser();
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }
}
