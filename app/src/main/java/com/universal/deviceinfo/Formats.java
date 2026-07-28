package com.universal.deviceinfo;

import java.util.Locale;

/**
 * Formatting helpers. Pure (no android.* imports) and therefore unit-testable
 * on any JVM. All numeric formatting uses {@link Locale#US} so decimal points
 * stay consistent regardless of the device locale.
 */
public final class Formats {

    /** Shown when a value could not be determined. */
    public static final String UNKNOWN = "desconocido";

    private Formats() {
    }

    public static String nn(String s) {
        if (s == null) {
            return UNKNOWN;
        }
        String t = s.trim();
        return t.isEmpty() ? UNKNOWN : t;
    }

    public static String nn(Object o) {
        return o == null ? UNKNOWN : nn(String.valueOf(o));
    }

    /** Human-readable byte size, base-1024, e.g. {@code 3.72 GB}. */
    public static String humanBytes(long bytes) {
        if (bytes < 0) {
            return UNKNOWN;
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        final String[] units = {"KB", "MB", "GB", "TB", "PB", "EB"};
        double v = bytes;
        int i = -1;
        do {
            v /= 1024.0;
            i++;
        } while (v >= 1024.0 && i < units.length - 1);
        return String.format(Locale.US, "%.2f %s", v, units[i]);
    }

    /** Same as {@link #humanBytes(long)} but appends the raw byte count. */
    public static String humanBytesDetailed(long bytes) {
        if (bytes < 0) {
            return UNKNOWN;
        }
        return humanBytes(bytes) + " (" + String.format(Locale.US, "%,d", bytes) + " bytes)";
    }

    /** Convert a value expressed in kB (as in /proc/meminfo) to human size. */
    public static String kbToHuman(long kb) {
        if (kb < 0) {
            return UNKNOWN;
        }
        return humanBytes(kb * 1024L);
    }

    /** Convert a CPU frequency in kHz (as in sysfs) to MHz/GHz. */
    public static String khzToHuman(long khz) {
        if (khz <= 0) {
            return UNKNOWN;
        }
        double mhz = khz / 1000.0;
        if (mhz >= 1000.0) {
            return String.format(Locale.US, "%.2f GHz", mhz / 1000.0);
        }
        return String.format(Locale.US, "%.0f MHz", mhz);
    }

    public static String percent(long part, long whole) {
        if (whole <= 0) {
            return UNKNOWN;
        }
        return String.format(Locale.US, "%.1f%%", (part * 100.0) / whole);
    }

    public static String yesNo(boolean b) {
        return b ? "sí" : "no";
    }

    /** Tenths of a degree (as battery temperature reports) -> "27.4 °C". */
    public static String deciCelsius(int tenths) {
        return String.format(Locale.US, "%.1f °C", tenths / 10.0);
    }

    /** Millivolts -> "4.31 V". */
    public static String milliVolts(int mv) {
        return String.format(Locale.US, "%.3f V", mv / 1000.0);
    }
}
