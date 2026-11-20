package ru.qa.flights.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Flight(String departure, String arrival, LocalDate date, LocalTime time, int priceRubles) {
    public String route() {
        return departure + " → " + arrival;
    }
}
