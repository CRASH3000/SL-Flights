package ru.qa.flights.tests;

import ru.qa.flights.data.TestData;
import ru.qa.flights.data.TestData.Credentials;
import ru.qa.flights.data.TestData.Route;
import ru.qa.flights.model.SearchCriteria;
import ru.qa.flights.pages.LoginPage;
import ru.qa.flights.pages.ResultsPage;
import ru.qa.flights.pages.SearchPage;
import ru.qa.flights.support.BrowserTest;

import static io.qameta.allure.Allure.step;
import static java.time.temporal.TemporalAdjusters.next;

abstract class FlightTest extends BrowserTest {
    protected static final TestData DATA = TestData.load();

    protected LoginPage openLogin() {
        var page = new LoginPage(driver, settings.waitTimeout());
        step("Открыть SL Flights и дождаться готовности формы входа", () -> page.open(settings.baseUrl()));
        return page;
    }

    protected void submitLogin(LoginPage login, Credentials credentials) {
        step("Заполнить форму входа",
                () -> login.enterCredentials(credentials.username(), credentials.password()));
        step("Нажать «Войти»", login::submit);
    }

    protected SearchPage signIn() {
        return signIn(openLogin());
    }

    protected SearchPage signIn(LoginPage login) {
        submitLogin(login, DATA.standard().credentials());
        var search = new SearchPage(driver, settings.waitTimeout());
        step("Дождаться формы поиска авиабилетов", search::awaitReady);
        return search;
    }

    protected SearchCriteria nextScheduledSearch(SearchPage search, Route route) {
        return route.on(search.browserToday().with(next(route.weekday())));
    }

    protected void enterCriteria(SearchPage search, SearchCriteria criteria) {
        step("Выбрать маршрут " + criteria.departure() + " → " + criteria.arrival(),
                () -> search.chooseRoute(criteria.departure(), criteria.arrival()));
        step("Указать дату вылета " + criteria.date(), () -> search.enterDate(criteria.date()));
    }

    protected ResultsPage findFlights(SearchPage search, SearchCriteria criteria) {
        enterCriteria(search, criteria);
        step("Нажать «Найти»", search::submit);
        var results = new ResultsPage(driver, settings.waitTimeout());
        step("Дождаться таблицы результатов и счётчика рейсов", results::awaitReady);
        return results;
    }
}
