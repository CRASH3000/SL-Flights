package ru.qa.flights.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;

public record TestSettings(String browser, boolean headless, URI baseUrl, Duration waitTimeout,
                           Duration pageLoadTimeout, Path dataFile, String safariRemoteUrl) {
    private static final Path CONFIG = Path.of("config", "test.properties");

    public static TestSettings load() {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(CONFIG)) {
            properties.load(reader);
        } catch (IOException error) {
            throw new UncheckedIOException("Откройте корень проекта с файлом config/test.properties", error);
        }

        return new TestSettings(
                value(properties, "browser").toLowerCase(Locale.ROOT),
                Boolean.parseBoolean(value(properties, "headless")),
                URI.create(value(properties, "baseUrl")),
                duration(properties, "waitSeconds"),
                duration(properties, "pageLoadSeconds"),
                Path.of(value(properties, "dataFile")),
                value(properties, "safariRemoteUrl"));
    }

    private static String value(Properties properties, String key) {
        return System.getProperty(key, properties.getProperty(key, "")).trim();
    }

    private static Duration duration(Properties properties, String key) {
        long seconds = Long.parseLong(value(properties, key));
        if (seconds <= 0) {
            throw new IllegalArgumentException(key + " должно быть положительным числом секунд");
        }
        return Duration.ofSeconds(seconds);
    }
}
