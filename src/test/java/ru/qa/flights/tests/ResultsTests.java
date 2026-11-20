package ru.qa.flights.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import ru.qa.flights.model.Flight;
import ru.qa.flights.pages.ResultsPage;
import ru.qa.flights.pages.ResultsPage.Direction;
import ru.qa.flights.pages.ResultsPage.SortField;

import java.util.Comparator;
import java.util.List;

import static io.qameta.allure.Allure.step;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(4)
@DisplayName("Таблица и сортировка рейсов")
class ResultsTests extends FlightTest {
    @Test
    @DisplayName("RESULT-01 · Сортировка по времени меняет порядок рейсов в обе стороны")
    void sortsByDepartureTime() {
        checkSorting(SortField.TIME, Comparator.comparing(Flight::time));
    }

    @Test
    @DisplayName("RESULT-02 · Сортировка по цене меняет порядок рейсов в обе стороны")
    void sortsByPrice() {
        checkSorting(SortField.PRICE, Comparator.comparingInt(Flight::priceRubles));
    }

    @Test
    @DisplayName("RESULT-03 · После смены сортировки пустая таблица сохраняет сообщение об отсутствии рейсов")
    void sortingKeepsNoFlightsNotice() {
        var search = signIn();
        var results = findFlights(search, nextScheduledSearch(search, DATA.search().withoutFlights()));

        step("Проверить исходное сообщение об отсутствии рейсов",
                () -> assertEquals(List.of(DATA.messages().noFlights()), results.notices()));
        step("Изменить сортировку на цену по убыванию",
                () -> results.sortBy(SortField.PRICE, Direction.DESCENDING));
        step("Убедиться, что сообщение осталось в таблице и счётчик равен 0", () -> assertAll(
                () -> assertEquals(List.of(DATA.messages().noFlights()), results.notices(), "Сообщение после сортировки"),
                () -> assertEquals(0, results.flightCount(), "Счётчик после сортировки")));
    }

    private void checkSorting(SortField field, Comparator<Flight> ascending) {
        var search = signIn();
        var criteria = nextScheduledSearch(search, DATA.search().withFlights());
        ResultsPage results = findFlights(search, criteria);
        List<Flight> expected = DATA.search().expectedFlights(criteria.date());

        checkOrder(results, field, Direction.DESCENDING, expected.stream().sorted(ascending.reversed()).toList());
        checkOrder(results, field, Direction.ASCENDING, expected.stream().sorted(ascending).toList());
    }

    private void checkOrder(ResultsPage results, SortField field, Direction direction, List<Flight> expected) {
        String sorting = "по " + field + " " + direction;
        step("Выбрать сортировку " + sorting, () -> results.sortBy(field, direction));
        step("Сверить порядок и данные строк " + sorting, () -> assertAll(
                () -> assertEquals(expected, results.flights(), "Порядок рейсов"),
                () -> assertEquals(expected.size(), results.flightCount(), "Счётчик рейсов")));
    }
}
