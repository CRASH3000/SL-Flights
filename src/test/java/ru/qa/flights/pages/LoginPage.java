package ru.qa.flights.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.time.Duration;

public final class LoginPage extends Page {
    private static final By FORM = By.id("loginContainer");
    private static final By TITLE = By.cssSelector("#loginContainer h2");
    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By SUBMIT = By.id("loginButton");
    private static final By ERROR = By.cssSelector("#message.error");

    public LoginPage(WebDriver driver, Duration timeout) {
        super(driver, timeout);
    }

    public void open(URI url) {
        driver.get(url.toString());
        awaitReady();
    }

    public void awaitReady() {
        visible(FORM);
        clickable(USERNAME);
        clickable(PASSWORD);
        clickable(SUBMIT);
    }

    public String title() {
        return visible(TITLE).getText();
    }

    public void enterCredentials(String username, String password) {
        fill(USERNAME, username);
        fill(PASSWORD, password);
    }

    public void submit() {
        clickable(SUBMIT).click();
    }

    public String error() {
        return visible(ERROR).getText();
    }

    public String username() {
        return value(USERNAME);
    }

    public String password() {
        return value(PASSWORD);
    }

    public boolean isVisible() {
        return displayed(FORM);
    }
}
