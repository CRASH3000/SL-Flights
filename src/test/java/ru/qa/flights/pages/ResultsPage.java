package ru.qa.flights.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import ru.qa.flights.model.Flight;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ResultsPage extends Page {
    private static final By TABLE = By.id("flightsTable");
    private static final By COUNT = By.id("flightsCount");
    private static final By BODY = By.id("flightsContainer");
    private static final By ROW = By.tagName("tr");
    private static final By CELL = By.tagName("td");
    private static final By EMPTY_NOTICE = By.cssSelector("#flightsContainer td[colspan]");
    private static final By SORT_FIELD = By.id("sortField");
    private static final By SORT_DIRECTION = By.id("sortDirection");
    private static final By NEW_SEARCH = By.cssSelector("#flightsList .new-search-btn");
    private static final By BOOK = By.className("register-btn");
    private static final String COUNT_PREFIX = "Найдено рейсов: ";
    private static final String PRICE_SUFFIX = " руб.";

    public ResultsPage(WebDriver driver, Duration timeout) {
        super(driver, timeout);
    }

    public void awaitReady() {
        visible(TABLE);
        visible(COUNT);
    }

    public boolean isVisible() {
        return displayed(TABLE);
    }

    public int flightCount() {
        String count = visible(COUNT).getText();
        if (!count.startsWith(COUNT_PREFIX)) {
            throw new IllegalStateException("Не удалось прочитать счётчик рейсов: " + count);
        }
        return Integer.parseInt(count.substring(COUNT_PREFIX.length()));
    }

    public List<String> notices() {
        awaitReady();
        return driver.findElements(EMPTY_NOTICE).stream().filter(WebElement::isDisplayed)
                .map(WebElement::getText).toList();
    }

    public List<Flight> flights() {
        return flightRows().stream().map(ResultsPage::readFlight).toList();
    }

    public void sortBy(SortField field, Direction direction) {
        new Select(clickable(SORT_FIELD)).selectByValue(field.value);
        new Select(clickable(SORT_DIRECTION)).selectByValue(direction.value);
    }

    public void newSearch() {
        clickable(NEW_SEARCH).click();
    }

    public void book(Flight expected) {
        WebElement row = flightRows().stream()
                .filter(candidate -> readFlight(candidate).equals(expected))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Рейс отсутствует в таблице: " + expected));
        row.findElement(BOOK).click();
    }

    private List<WebElement> flightRows() {
        return visible(TABLE).findElement(BODY).findElements(ROW).stream()
                .filter(row -> !row.findElements(BOOK).isEmpty())
                .toList();
    }

    private static Flight readFlight(WebElement row) {
        List<WebElement> cells = row.findElements(CELL);
        String price = cells.get(5).getText();
        if (!price.endsWith(PRICE_SUFFIX)) {
            throw new IllegalStateException("Не удалось прочитать цену в рублях: " + price);
        }
        String amount = price.substring(0, price.length() - PRICE_SUFFIX.length()).replaceAll("[\\s\\p{Zs}]", "");
        return new Flight(cells.get(1).getText(), cells.get(2).getText(),
                LocalDate.parse(cells.get(3).getText()), LocalTime.parse(cells.get(4).getText()),
                Integer.parseInt(amount));
    }

    public enum SortField {
        TIME("time", "времени вылета"), PRICE("price", "цене");

        private final String value;
        private final String label;

        SortField(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Direction {
        ASCENDING("asc", "по возрастанию"), DESCENDING("desc", "по убыванию");

        private final String value;
        private final String label;

        Direction(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
