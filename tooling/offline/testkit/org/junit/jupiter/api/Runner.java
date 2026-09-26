package org.junit.jupiter.api;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Minimal reflective test runner used for the offline sandbox build. The test
 * sources themselves are plain JUnit 5, so the shipped Maven build runs the very
 * same tests through Surefire.
 */
public final class Runner {

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        List<String> classNames = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith("Test.class"))
                    .map(path -> root.relativize(path).toString()
                            .replace(".class", "").replace('/', '.'))
                    .sorted(Comparator.naturalOrder())
                    .forEach(classNames::add);
        } catch (IOException ex) {
            System.err.println("unable to scan test classes: " + ex.getMessage());
            System.exit(2);
        }

        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();
        for (String className : classNames) {
            Class<?> testClass = Class.forName(className);
            List<Method> tests = new ArrayList<>();
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    tests.add(method);
                }
            }
            tests.sort(Comparator.comparing(Method::getName));
            for (Method test : tests) {
                var constructor = testClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object instance = constructor.newInstance();
                try {
                    for (Method before : testClass.getDeclaredMethods()) {
                        if (before.isAnnotationPresent(BeforeEach.class)) {
                            before.setAccessible(true);
                            before.invoke(instance);
                        }
                    }
                    test.setAccessible(true);
                    test.invoke(instance);
                    passed++;
                    System.out.println("PASS " + className + "#" + test.getName());
                } catch (Exception ex) {
                    failed++;
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    String message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
                    failures.add(className + "#" + test.getName() + " -> " + message);
                    System.out.println("FAIL " + className + "#" + test.getName() + " -> " + message);
                    StackTraceElement[] trace = cause.getStackTrace();
                    for (int i = 0; i < Math.min(4, trace.length); i++) {
                        System.out.println("      at " + trace[i]);
                    }
                }
            }
        }
        System.out.println();
        System.out.println("Tests run: " + (passed + failed) + ", passed: " + passed + ", failed: " + failed);
        if (!failures.isEmpty()) {
            System.out.println("Failures:");
            failures.forEach(failure -> System.out.println(" - " + failure));
        }
        System.exit(failed == 0 ? 0 : 1);
    }
}
