package ru.qa.flights.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.qa.flights.model.Flight;
import ru.qa.flights.model.Passenger;
import ru.qa.flights.model.SearchCriteria;
import ru.qa.flights.support.TestSettings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TestData(Account standard, Credentials locked, String wrongPassword,
                       SearchFixture search, Passenger editedPassenger,
                       InvalidInputs invalidInputs, Messages messages) {

    public static TestData load() {
        var path = TestSettings.load().dataFile();
        try {
            return new ObjectMapper()
                    .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                    .readValue(path.toFile(), TestData.class);
        } catch (IOException error) {
            throw new UncheckedIOException("Проверьте JSON с тестовыми данными " + path, error);
        }
    }

    public record Credentials(String username, String password) {}

    public record Account(Credentials credentials, Passenger passenger) {}

    public record Route(String departure, String arrival, DayOfWeek weekday) {
        public SearchCriteria on(LocalDate date) {
            return new SearchCriteria(departure, arrival, date);
        }
    }

    public record SearchFixture(Route withFlights, List<Departure> flights, Route withoutFlights) {
        public List<Flight> expectedFlights(LocalDate date) {
            return flights.stream()
                    .map(flight -> new Flight(withFlights.departure(), withFlights.arrival(), date,
                            LocalTime.parse(flight.time()), flight.priceRubles()))
                    .toList();
        }
    }

    public record Departure(String time, int priceRubles) {}

    public record InvalidInputs(String name, String passport, String email, String phone) {}

    public record Messages(String greeting, String wrongCredentials, String lockedUser, String emptyCredentials,
                           String missingDate, String pastDate, String noFlights, String requiredPassengerFields,
                           String invalidName, String invalidPassport, String invalidEmail,
                           String bookingSuccess, String confirmationHeading) {}
}
