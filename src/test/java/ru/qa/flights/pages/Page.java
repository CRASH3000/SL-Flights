package ru.qa.flights.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

abstract class Page {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    Page(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, timeout);
    }

    protected WebElement visible(By locator) {
        return wait.until(visibilityOfElementLocated(locator));
    }

    protected WebElement clickable(By locator) {
        return wait.until(elementToBeClickable(locator));
    }

    protected boolean displayed(By locator) {
        return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }

    protected String value(By locator) {
        return visible(locator).getDomProperty("value");
    }

    protected void fill(By locator, String value) {
        WebElement field = clickable(locator);
        field.clear();
        if (!value.isEmpty()) {
            field.sendKeys(value);
        }
    }
}
