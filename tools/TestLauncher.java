import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class TestLauncher {
    private static final List<String> BROWSERS = List.of("chrome", "firefox", "safari");
    private static final String OS = System.getProperty("os.name").toLowerCase(Locale.ROOT);

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> tasks = args.length == 0 ? chooseRun() : List.of(args);
        if (tasks.isEmpty()) {
            return;
        }

        List<String> command = new ArrayList<>(OS.startsWith("windows")
                ? List.of("cmd.exe", "/d", "/c", "gradlew.bat")
                : List.of("sh", "./gradlew"));
        command.add("--console=plain");
        command.addAll(tasks);
        System.exit(new ProcessBuilder(command).inheritIO().start().waitFor());
    }

    private static List<String> chooseRun() throws IOException {
        var input = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("""
                SL Flights · Автотесты
                1. Быстрая проверка (Smoke)
                2. Полный набор
                3. Подготовить проект
                0. Выход""");
        String suite = choose(input, "Набор [1]: ", List.of("0", "1", "2", "3"));
        if (suite.equals("0")) {
            return List.of();
        }
        if (suite.equals("3")) {
            return List.of("prepare");
        }

        System.out.println("""

                1. Chrome
                2. Firefox
                3. Safari
                4. Все три браузера
                5. Chrome и Firefox""");
        String browser = choose(input, "Браузер [1]: ", List.of("0", "1", "2", "3", "4", "5"));
        if (browser.equals("0")) {
            return List.of();
        }
        if (!OS.contains("mac") && (browser.equals("3") || browser.equals("4"))) {
            System.out.println("Для Safari на этой ОС укажите safariRemoteUrl в config/test.properties.");
        }

        String suiteOption = suite.equals("1") ? "-Psuite=smoke" : "-Psuite=full";
        return switch (browser) {
            case "4" -> List.of("crossBrowserTest", suiteOption, "--continue");
            case "5" -> List.of("chromeTest", "firefoxTest", suiteOption, "--continue");
            default -> List.of("test", suiteOption,
                    "-Pbrowser=" + BROWSERS.get(Integer.parseInt(browser) - 1));
        };
    }

    private static String choose(BufferedReader input, String prompt, List<String> choices) throws IOException {
        while (true) {
            System.out.print(prompt);
            System.out.flush();
            String line = input.readLine();
            if (line == null) {
                return "0";
            }
            String answer = line.isBlank() ? "1" : line.trim();
            if (choices.contains(answer)) {
                return answer;
            }
            System.out.println("Введите номер из меню.");
        }
    }
}
