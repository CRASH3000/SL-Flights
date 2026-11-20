package ru.qa.flights.model;

import java.time.LocalDate;

public record SearchCriteria(String departure, String arrival, LocalDate date) {}
