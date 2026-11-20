package ru.qa.flights.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.Allure.step;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(1)
@DisplayName("Доступность SL Flights")
class AvailabilityTests extends FlightTest {
    @Test
    @Tag("smoke")
    @DisplayName("SM-01 · Форма входа открывается, принимает ввод и позволяет нажать «Войти»")
    void loginFormIsReady() {
        var login = openLogin();
        var credentials = DATA.standard().credentials();

        step("Проверить заголовок «Аутентификация»",
                () -> assertEquals("Аутентификация", login.title()));
        step("Ввести данные учебной учётной записи",
                () -> login.enterCredentials(credentials.username(), credentials.password()));
        step("Проверить введённые значения и доступность кнопки «Войти»", () -> {
            assertAll(
                    () -> assertEquals(credentials.username(), login.username(), "Имя пользователя"),
                    () -> assertEquals(credentials.password(), login.password(), "Пароль"));
            login.awaitReady();
        });
    }
}
