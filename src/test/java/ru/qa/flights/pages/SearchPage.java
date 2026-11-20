package ru.qa.flights.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import ru.qa.flights.model.SearchCriteria;

import java.time.Duration;
import java.time.LocalDate;

public final class SearchPage extends Page {
    private static final By FORM = By.id("flightForm");
    private static final By DEPARTURE = By.id("departureCity");
    private static final By ARRIVAL = By.id("arrivalCity");
    private static final By DATE = By.id("departureDate");
    private static final By SUBMIT = By.cssSelector("#flightForm .input-group button");
    private static final By ERROR = By.id("searchMessage");
    private static final By GREETING = By.id("greeting");
    private static final By LOGOUT = By.id("logoutButton");

    public SearchPage(WebDriver driver, Duration timeout) {
        super(driver, timeout);
    }

    public void awaitReady() {
        visible(FORM);
        clickable(SUBMIT);
    }

    public boolean isVisible() {
        return displayed(FORM);
    }

    public String greeting() {
        return visible(GREETING).getText();
    }

    public void logout() {
        clickable(LOGOUT).click();
    }

    public void chooseRoute(String departure, String arrival) {
        new Select(clickable(DEPARTURE)).selectByVisibleText(departure);
        new Select(clickable(ARRIVAL)).selectByVisibleText(arrival);
    }

    public void enterDate(LocalDate date) {
        setDateValue(date.toString());
    }

    public void clearDate() {
        setDateValue("");
    }

    private void setDateValue(String value) {
        // Нативные поля даты используют разные маски ввода. Передаём ISO-значение через DOM.
        ((JavascriptExecutor) driver).executeScript("""
                const field = arguments[0];
                field.value = arguments[1];
                field.dispatchEvent(new Event('input', { bubbles: true }));
                field.dispatchEvent(new Event('change', { bubbles: true }));
                """, clickable(DATE), value);
    }

    public LocalDate browserToday() {
        String localDate = (String) ((JavascriptExecutor) driver).executeScript("""
                const now = new Date();
                return [now.getFullYear(), now.getMonth() + 1, now.getDate()]
                    .map(part => String(part).padStart(2, '0')).join('-');
                """);
        return LocalDate.parse(localDate);
    }

    public LocalDate departureDate() {
        return LocalDate.parse(value(DATE));
    }

    public SearchCriteria criteria() {
        String departure = new Select(visible(DEPARTURE)).getFirstSelectedOption().getText();
        String arrival = new Select(visible(ARRIVAL)).getFirstSelectedOption().getText();
        return new SearchCriteria(departure, arrival, departureDate());
    }

    public void submit() {
        clickable(SUBMIT).click();
    }

    public String error() {
        return visible(ERROR).getText();
    }
}
