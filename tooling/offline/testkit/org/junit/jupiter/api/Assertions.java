package org.junit.jupiter.api;

/** Offline stand-in implementing the JUnit 5 assertions used by the test suite. */
public final class Assertions {

    private Assertions() {
    }

    public static void assertTrue(boolean condition) {
        assertTrue(condition, "expected condition to be true");
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition) {
        assertFalse(condition, "expected condition to be false");
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(long expected, long actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(describe(message, expected, actual));
        }
    }

    public static void assertEquals(double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(describe(null, expected, actual));
        }
    }

    public static void assertEquals(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(describe(message, expected, actual));
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(describe(message, expected, actual));
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual) {
        if (unexpected == null ? actual == null : unexpected.equals(actual)) {
            throw new AssertionError("expected a value different from <" + unexpected + ">");
        }
    }

    public static void assertNull(Object value) {
        if (value != null) {
            throw new AssertionError("expected null but was <" + value + ">");
        }
    }

    public static void assertNotNull(Object value) {
        assertNotNull(value, "expected a non-null value");
    }

    public static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    public static void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new AssertionError(describe(null, expected, actual));
        }
    }

    public static <T extends Throwable> T assertThrows(Class<T> expected, Executable executable) {
        try {
            executable.execute();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) {
                return expected.cast(thrown);
            }
            throw new AssertionError("expected " + expected.getName() + " but " + thrown.getClass().getName()
                    + " was thrown: " + thrown.getMessage());
        }
        throw new AssertionError("expected " + expected.getName() + " to be thrown but nothing was thrown");
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }

    private static String describe(String message, Object expected, Object actual) {
        String base = "expected <" + expected + "> but was <" + actual + ">";
        return message == null ? base : message + " ==> " + base;
    }

    /** Matches the JUnit 5 functional interface used by {@code assertThrows}. */
    public interface Executable {
        void execute() throws Throwable;
    }
}
