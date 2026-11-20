package ru.qa.flights.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qa.flights.data.TestData.Credentials;
import ru.qa.flights.pages.SearchPage;

import java.util.List;

import static io.qameta.allure.Allure.step;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(2)
@DisplayName("Вход и выход")
class AuthenticationTests extends FlightTest {
    @Test
    @Tag("smoke")
    @DisplayName("AUTH-01 · Верные данные открывают поиск и приветствие с ФИО")
    void showsSearchAndGreetingAfterLogin() {
        var login = openLogin();
        var search = signIn(login);

        step("Проверить приветствие с ФИО и скрытие формы входа", () -> assertAll(
                () -> assertEquals(DATA.messages().greeting(), search.greeting(), "Приветствие"),
                () -> assertFalse(login.isVisible(), "Форма входа скрыта")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedLogins")
    @DisplayName("Отказ во входе")
    void rejectsInvalidLogin(RejectedLogin scenario) {
        var login = openLogin();
        submitLogin(login, scenario.credentials());

        step("Проверить ошибку «" + scenario.error() + "» и закрытый доступ к поиску", () -> assertAll(
                () -> assertEquals(scenario.error(), login.error(), "Текст ошибки"),
                () -> assertTrue(login.isVisible(), "Форма входа видна"),
                () -> assertFalse(new SearchPage(driver, settings.waitTimeout()).isVisible(), "Форма поиска скрыта")));
    }

    private static List<RejectedLogin> rejectedLogins() {
        return List.of(
                new RejectedLogin("AUTH-02 · Неверный пароль оставляет пользователя на форме с ошибкой учётных данных",
                        new Credentials(DATA.standard().credentials().username(), DATA.wrongPassword()),
                        DATA.messages().wrongCredentials()),
                new RejectedLogin("AUTH-03 · Оба пустых поля вызывают сообщение об обязательности логина и пароля",
                        new Credentials("", ""), DATA.messages().emptyCredentials()),
                new RejectedLogin("AUTH-04 · Верный пароль заблокированной учётной записи вызывает сообщение о блокировке",
                        DATA.locked(), DATA.messages().lockedUser()));
    }

    @Test
    @DisplayName("AUTH-05 · Logout очищает поля входа и сбрасывает дату поиска на текущую")
    void logoutClearsCredentialsAndSearchDate() {
        var login = openLogin();
        var search = signIn(login);
        var future = nextScheduledSearch(search, DATA.search().withFlights()).date();

        step("Задать будущую дату " + future, () -> search.enterDate(future));
        step("Нажать Logout", search::logout);
        step("Проверить возврат к пустой форме входа и скрытие поиска", () -> {
            login.awaitReady();
            assertAll(
                    () -> assertEquals("", login.username(), "Очищен логин"),
                    () -> assertEquals("", login.password(), "Очищен пароль"),
                    () -> assertFalse(search.isVisible(), "Поиск скрыт"));
        });
        var reopenedSearch = signIn(login);
        step("После повторного входа проверить, что дата вылета совпадает с сегодняшней датой браузера",
                () -> assertEquals(reopenedSearch.browserToday(), reopenedSearch.departureDate(), "Дата после Logout"));
    }

    private record RejectedLogin(String title, Credentials credentials, String error) {
        @Override
        public String toString() {
            return title;
        }
    }
}
