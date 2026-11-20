package ru.qa.flights.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ru.qa.flights.model.Passenger;

import java.time.Duration;

import static org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent;

public final class BookingPage extends Page {
    private static final By FLIGHT_INFO = By.id("flightRegistrationInfo");
    private static final By SUBMIT = By.cssSelector("#registrationForm .input-group button");
    private static final By FEEDBACK = By.cssSelector("#registrationMessage:is(.error, .success)");
    private static final By BACK = By.cssSelector("#registrationForm .back-to-flights-btn");
    private static final By FORM = By.id("registrationForm");

    public BookingPage(WebDriver driver, Duration timeout) {
        super(driver, timeout);
    }

    public void awaitReady() {
        visible(FLIGHT_INFO);
        clickable(SUBMIT);
    }

    public String flightDetails() {
        return visible(FLIGHT_INFO).getText();
    }

    public boolean isVisible() {
        return displayed(FORM);
    }

    public Passenger passenger() {
        return new Passenger(value(Field.NAME.locator), value(Field.PASSPORT.locator),
                value(Field.EMAIL.locator), value(Field.PHONE.locator));
    }

    public void enterPassenger(Passenger passenger) {
        change(Field.NAME, passenger.fullName());
        change(Field.PASSPORT, passenger.passport());
        change(Field.EMAIL, passenger.email());
        change(Field.PHONE, passenger.phone());
    }

    public void change(Field field, String value) {
        fill(field.locator, value);
    }

    public void submit() {
        clickable(SUBMIT).click();
    }

    public Feedback feedback() {
        var message = visible(FEEDBACK);
        return new Feedback(message.getText(), displayed(By.cssSelector("#registrationMessage.error")));
    }

    public void backToResults() {
        clickable(BACK).click();
    }

    public String readAndCloseConfirmation() {
        var confirmation = wait.until(alertIsPresent());
        String text = confirmation.getText();
        confirmation.accept();
        return text;
    }

    public enum Field {
        NAME("passengerName", "ФИО"),
        PASSPORT("passportNumber", "номер паспорта"),
        EMAIL("email", "email"),
        PHONE("phone", "телефон");

        private final By locator;
        private final String label;

        Field(String id, String label) {
            this.locator = By.id(id);
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record Feedback(String text, boolean error) {}
}
