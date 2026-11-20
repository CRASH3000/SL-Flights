package ru.qa.flights.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qa.flights.model.Flight;
import ru.qa.flights.pages.ResultsPage;
import ru.qa.flights.pages.SearchPage;

import java.util.Comparator;
import java.util.List;

import static io.qameta.allure.Allure.step;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(3)
@DisplayName("Поиск рейсов")
class SearchTests extends FlightTest {
    @Test
    @Tag("smoke")
    @DisplayName("SEARCH-01 · Поиск по маршруту и дате возвращает рейсы из расписания и верный счётчик")
    void findsOnlyScheduledFlights() {
        var search = signIn();
        var criteria = nextScheduledSearch(search, DATA.search().withFlights());
        var results = findFlights(search, criteria);
        var expected = DATA.search().expectedFlights(criteria.date());

        step("Сверить маршрут, дату, время и цену всех найденных рейсов и проверить счётчик", () -> assertAll(
                () -> assertEquals(expected.size(), results.flightCount(), "Счётчик рейсов"),
                () -> assertEquals(byTime(expected), byTime(results.flights()), "Рейсы из учебного расписания"),
                () -> assertFalse(search.isVisible(), "Форма поиска скрыта")));
    }

    @Test
    @DisplayName("SEARCH-02 · Без даты форма запрашивает дату вылета и остаётся открытой")
    void requiresDepartureDate() {
        var search = signIn();
        step("Очистить поле даты вылета", search::clearDate);
        checkRejectedSearch(search, DATA.messages().missingDate());
    }

    @Test
    @DisplayName("SEARCH-03 · Вчерашняя дата отклоняется как прошедшая")
    void rejectsYesterday() {
        var search = signIn();
        var yesterday = search.browserToday().minusDays(1);
        step("Указать вчерашнюю дату браузера " + yesterday, () -> search.enterDate(yesterday));
        checkRejectedSearch(search, DATA.messages().pastDate());
    }

    @Test
    @DisplayName("SEARCH-04 · Маршрут без рейсов показывает сообщение и счётчик 0")
    void informsWhenNoFlightsMatch() {
        var search = signIn();
        var results = findFlights(search, nextScheduledSearch(search, DATA.search().withoutFlights()));

        step("Проверить один ряд с сообщением об отсутствии рейсов и счётчик 0", () -> assertAll(
                () -> assertEquals(List.of(DATA.messages().noFlights()), results.notices(), "Сообщение таблицы"),
                () -> assertEquals(0, results.flightCount(), "Счётчик рейсов"),
                () -> assertTrue(results.flights().isEmpty(), "Строки с рейсами отсутствуют")));
    }

    @Test
    @DisplayName("SEARCH-05 · «Новый поиск» восстанавливает маршрут и дату предыдущего запроса")
    void restoresCriteriaForNewSearch() {
        var search = signIn();
        var criteria = nextScheduledSearch(search, DATA.search().withoutFlights());
        var results = findFlights(search, criteria);

        step("Нажать «Новый поиск»", results::newSearch);
        step("Сверить оба города и дату с предыдущим запросом", () -> {
            search.awaitReady();
            assertAll(
                    () -> assertEquals(criteria, search.criteria(), "Сохранённые параметры"),
                    () -> assertFalse(results.isVisible(), "Таблица скрыта"));
        });
    }

    @Test
    @DisplayName("SEARCH-06 · Сегодняшняя дата установлена по умолчанию и допускает поиск")
    void acceptsToday() {
        var search = signIn();
        var today = search.browserToday();

        step("Проверить, что дата по умолчанию совпадает с датой браузера " + today,
                () -> assertEquals(today, search.departureDate(), "Дата по умолчанию"));
        step("Отправить поиск на сегодня и проверить открытие результатов", () -> {
            search.submit();
            new ResultsPage(driver, settings.waitTimeout()).awaitReady();
            assertFalse(search.isVisible(), "Форма поиска скрыта");
        });
    }

    private void checkRejectedSearch(SearchPage search, String message) {
        step("Нажать «Найти»", search::submit);
        step("Проверить ошибку «" + message + "» и доступность формы поиска", () -> assertAll(
                () -> assertEquals(message, search.error(), "Текст ошибки"),
                () -> assertTrue(search.isVisible(), "Форма поиска видна"),
                () -> assertFalse(new ResultsPage(driver, settings.waitTimeout()).isVisible(), "Таблица скрыта")));
    }

    private static List<Flight> byTime(List<Flight> flights) {
        return flights.stream().sorted(Comparator.comparing(Flight::time)).toList();
    }
}
