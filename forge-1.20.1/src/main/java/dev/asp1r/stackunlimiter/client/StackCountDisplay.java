package dev.asp1r.stackunlimiter.client;

import java.util.Locale;

public final class StackCountDisplay {
    public static final int MAX_LABEL_WIDTH = 16;
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q", "E"};

    private StackCountDisplay() {
    }

    public static String format(long count) {
        if (count < 1_000) {
            return Long.toString(count);
        }

        double scaled = count;
        int suffix = 0;
        while (scaled >= 1_000.0D && suffix < SUFFIXES.length - 1) {
            scaled /= 1_000.0D;
            suffix++;
        }

        String number;
        if (scaled < 10.0D) {
            number = String.format(Locale.ROOT, "%.1f", scaled);
            if (number.endsWith(".0")) {
                number = number.substring(0, number.length() - 2);
            }
        } else {
            number = String.format(Locale.ROOT, "%.0f", scaled);
        }
        return number + SUFFIXES[suffix];
    }

    public static float scaleForWidth(int width) {
        if (width <= 0 || width <= MAX_LABEL_WIDTH) {
            return 1.0F;
        }
        return (float) MAX_LABEL_WIDTH / width;
    }
}
