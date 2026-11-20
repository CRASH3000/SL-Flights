package ru.qa.flights.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qa.flights.model.Flight;
import ru.qa.flights.pages.BookingPage;
import ru.qa.flights.pages.BookingPage.Field;
import ru.qa.flights.pages.ResultsPage;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.qameta.allure.Allure.step;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(5)
@DisplayName("Бронирование рейса")
class BookingTests extends FlightTest {
    private static final Pattern RUBLES_IN_PRICE_LINE = Pattern.compile("(Цена: )([\\d\\p{Zs}]+)( руб\\.)");

    @Test
    @Tag("smoke")
    @DisplayName("BOOK-01 · Выбранный рейс и данные пассажира попадают в подтверждение брони")
    void confirmsBookingWithEditedPassenger() {
        var journey = selectFlight();
        var booking = journey.booking();
        var flight = journey.flight();

        step("Сверить маршрут, дату, время и цену выбранного рейса", () -> {
            String details = booking.flightDetails();
            assertAll(
                    () -> assertTrue(details.contains("Маршрут: " + flight.route()), details),
                    () -> assertTrue(details.contains("Дата: " + flight.date()), details),
                    () -> assertTrue(details.contains("Время: " + flight.time()), details),
                    () -> assertTrue(priceWithoutGrouping(details).contains("Цена: " + flight.priceRubles() + " руб."), details));
        });
        step("Проверить автозаполнение ФИО, паспорта, email и телефона из профиля",
                () -> assertEquals(DATA.standard().passenger(), booking.passenger(), "Данные профиля"));

        var passenger = DATA.editedPassenger();
        step("Ввести другие данные во все четыре поля пассажира", () -> booking.enterPassenger(passenger));
        step("Завершить регистрацию на рейс", booking::submit);
        step("Проверить сообщение об успехе и подтверждение с выбранным рейсом и данными пассажира", () -> {
            var feedback = booking.feedback();
            String confirmation = booking.readAndCloseConfirmation().replace("\r\n", "\n");
            String expectedConfirmation = """
                    %s

                    Рейс: %s
                    Дата: %s
                    Время: %s
                    Цена: %d руб.

                    Пассажир: %s
                    Паспорт: %s
                    Email: %s
                    Телефон: %s""".formatted(DATA.messages().confirmationHeading(), flight.route(), flight.date(),
                    flight.time(), flight.priceRubles(), passenger.fullName(), passenger.passport(),
                    passenger.email(), passenger.phone());
            assertAll(
                    () -> assertEquals(DATA.messages().bookingSuccess(), feedback.text(), "Текст в форме"),
                    () -> assertFalse(feedback.error(), "Показан успех"),
                    () -> assertEquals(expectedConfirmation, priceWithoutGrouping(confirmation), "Текст подтверждения"));
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPassengerFields")
    @DisplayName("Валидация данных пассажира")
    void rejectsInvalidPassengerField(InvalidPassengerField scenario) {
        var booking = selectFlight().booking();
        String inputStep = scenario.value().isEmpty()
                ? "Очистить поле «" + scenario.field() + "»"
                : "Ввести некорректное значение в поле «" + scenario.field() + "»";
        step(inputStep, () -> booking.change(scenario.field(), scenario.value()));
        step("Нажать «Завершить регистрацию»", booking::submit);
        step("Проверить, что бронирование отклонено и форма остаётся открытой", () -> {
            var feedback = booking.feedback();
            assertAll(
                    () -> assertTrue(feedback.error(), () -> "Ожидалась ошибка валидации. Получено: " + feedback.text()),
                    () -> assertTrue(booking.isVisible(), "Форма бронирования видна"));
            if (scenario.error() != null) {
                assertEquals(scenario.error(), feedback.text(), "Текст ошибки");
            }
        });
    }

    private static List<InvalidPassengerField> invalidPassengerFields() {
        var invalid = DATA.invalidInputs();
        var messages = DATA.messages();
        return List.of(
                new InvalidPassengerField("BOOK-02 · Паспорт с буквами отклоняется с сообщением о допустимом формате",
                        Field.PASSPORT, invalid.passport(), messages.invalidPassport()),
                new InvalidPassengerField("BOOK-03 · ФИО на латинице отклоняется с сообщением о допустимом формате",
                        Field.NAME, invalid.name(), messages.invalidName()),
                new InvalidPassengerField("BOOK-04 · Email без @ отклоняется с сообщением о формате",
                        Field.EMAIL, invalid.email(), messages.invalidEmail()),
                new InvalidPassengerField("BOOK-05 · Пустой телефон вызывает сообщение об обязательных полях",
                        Field.PHONE, "", messages.requiredPassengerFields()),
                new InvalidPassengerField("BOOK-06 · Телефон из букв должен быть отклонён как некорректный",
                        Field.PHONE, invalid.phone(), null));
    }

    @Test
    @DisplayName("BOOK-07 · Возврат к рейсам отменяет несохранённые изменения пассажира")
    void returnsToResultsWithoutSavingPassengerChanges() {
        var journey = selectFlight();
        var booking = journey.booking();

        step("Изменить ФИО и нажать «Вернуться к найденным рейсам»", () -> {
            booking.change(Field.NAME, DATA.editedPassenger().fullName());
            booking.backToResults();
        });
        step("Проверить возврат к тому же списку рейсов", () -> {
            journey.results().awaitReady();
            assertAll(
                    () -> assertFalse(booking.isVisible(), "Форма бронирования скрыта"),
                    () -> assertEquals(DATA.search().flights().size(), journey.results().flightCount(), "Счётчик рейсов"),
                    () -> assertTrue(journey.results().flights().contains(journey.flight()), "Выбранный рейс доступен"));
        });
        step("Открыть тот же рейс и проверить исходные данные профиля", () -> {
            journey.results().book(journey.flight());
            booking.awaitReady();
            assertEquals(DATA.standard().passenger(), booking.passenger(), "Данные пассажира после возврата");
        });
    }

    private Journey selectFlight() {
        var search = signIn();
        var criteria = nextScheduledSearch(search, DATA.search().withFlights());
        var results = findFlights(search, criteria);
        Flight flight = DATA.search().expectedFlights(criteria.date()).getFirst();
        step("Выбрать рейс " + flight.route() + " в " + flight.time(), () -> results.book(flight));

        var booking = new BookingPage(driver, settings.waitTimeout());
        step("Дождаться формы бронирования", booking::awaitReady);
        return new Journey(results, flight, booking);
    }

    private static String priceWithoutGrouping(String text) {
        // Разные браузеры показывают группировку тысяч обычным или неразрывным пробелом.
        return RUBLES_IN_PRICE_LINE.matcher(text).replaceAll(match -> Matcher.quoteReplacement(
                match.group(1) + match.group(2).replaceAll("\\p{Zs}", "") + match.group(3)));
    }

    private record Journey(ResultsPage results, Flight flight, BookingPage booking) {}

    private record InvalidPassengerField(String title, Field field, String value, String error) {
        @Override
        public String toString() {
            return title;
        }
    }
}
