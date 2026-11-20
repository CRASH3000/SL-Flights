package ru.qa.flights.support;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.manager.SeleniumManager;
import org.openqa.selenium.manager.SeleniumManagerOutput.Result;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariDriverService;
import org.openqa.selenium.safari.SafariOptions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class BrowserFactory {
    private static final Map<String, Result> DRIVERS = new ConcurrentHashMap<>();

    private BrowserFactory() {
    }

    static WebDriver create(String browser, TestSettings settings) {
        return switch (browser) {
            case "chrome" -> chrome(settings.headless());
            case "firefox" -> firefox(settings.headless());
            case "safari" -> safari(settings);
            default -> throw new IllegalArgumentException("Неизвестный браузер: " + browser);
        };
    }

    private static WebDriver chrome(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments("--window-size=1440,1000");
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false,
                "profile.password_manager_leak_detection", false));
        if (headless) {
            options.addArguments("--headless");
        }

        Result paths = resolveDriver("chrome");
        options.setBinary(paths.getBrowserPath());
        return new ChromeDriver(new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(paths.getDriverPath())).build(), options);
    }

    private static WebDriver firefox(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        if (headless) {
            options.addArguments("-headless");
        }

        Result paths = resolveDriver("firefox");
        options.setBinary(paths.getBrowserPath());
        return new FirefoxDriver(new GeckoDriverService.Builder()
                .usingDriverExecutable(new File(paths.getDriverPath())).build(), options);
    }

    private static Result resolveDriver(String browser) {
        // Сверяем версию один раз в JVM прогона, затем переиспользуем пути в новых сеансах.
        return DRIVERS.computeIfAbsent(browser, name -> SeleniumManager.getInstance().getBinaryPaths(List.of(
                "--browser", name, "--skip-driver-in-path", "--ttl", "0", "--timeout", "60", "--avoid-stats")));
    }

    private static WebDriver safari(TestSettings settings) {
        SafariOptions options = new SafariOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        ClientConfig client = ClientConfig.defaultConfig()
                .connectionTimeout(Duration.ofSeconds(10))
                .readTimeout(settings.pageLoadTimeout().plusSeconds(5))
                .version("HTTP_1_1");
        String remoteUrl = settings.safariRemoteUrl();
        if (!remoteUrl.isBlank()) {
            try {
                return new RemoteWebDriver(URI.create(remoteUrl).toURL(), options, client);
            } catch (MalformedURLException error) {
                throw new IllegalArgumentException("Укажите полный URL Selenium Grid в safariRemoteUrl", error);
            }
        }
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            throw new IllegalStateException("Локальный Safari доступен на macOS. Для другого компьютера укажите safariRemoteUrl.");
        }
        return new SafariDriver(new SafariDriverService.Builder()
                .usingDriverExecutable(new File("/usr/bin/safaridriver")).build(), options, client);
    }
}
