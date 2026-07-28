package com.universal.deviceinfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parsers for Linux {@code /proc} pseudo-files (cpuinfo, meminfo, version).
 *
 * <p>Pure (no android.* imports): the caller reads the file into a String and
 * passes it here, which makes every branch unit-testable on a normal JVM.
 */
public final class ProcParser {

    private ProcParser() {
    }

    /**
     * Counts the lines whose first token equals {@code token} followed by ':' or
     * whitespace. Used to count "processor" entries in /proc/cpuinfo.
     *
     * <p>Matching is CASE-SENSITIVE on purpose: /proc field names have canonical
     * casing, and the lowercase per-core "processor : N" lines must not be
     * confused with the capitalised "Processor : ARMv7..." description line that
     * some ARM kernels emit (which would otherwise inflate the core count).
     */
    public static int countLinesStartingWith(String content, String token) {
        if (content == null || token == null) {
            return 0;
        }
        int count = 0;
        for (String line : content.split("\n")) {
            String t = line.trim();
            if (t.startsWith(token)) {
                String rest = t.substring(token.length());
                if (rest.isEmpty() || rest.charAt(0) == ':'
                        || Character.isWhitespace(rest.charAt(0))) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns the trimmed value of the first {@code key : value} line whose key
     * exactly matches (case-sensitive) any of {@code keys}, or {@code null}.
     * Case-sensitivity keeps "Processor" (ARM CPU description) distinct from
     * "processor" (per-core index).
     */
    public static String firstValueForKeys(String content, String... keys) {
        if (content == null) {
            return null;
        }
        for (String line : content.split("\n")) {
            int idx = line.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String k = line.substring(0, idx).trim();
            for (String key : keys) {
                if (k.equals(key)) {
                    String v = line.substring(idx + 1).trim();
                    if (!v.isEmpty()) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    /** Reads a /proc/meminfo numeric field (in kB); -1 if absent/unparseable. */
    public static long meminfoKb(String content, String key) {
        String v = firstValueForKeys(content, key);
        if (v == null) {
            return -1;
        }
        String[] parts = v.trim().split("\\s+");
        try {
            return Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** All distinct values for a repeated key (e.g. per-core "model name"). */
    public static List<String> distinctValuesForKey(String content, String key) {
        List<String> out = new ArrayList<String>();
        if (content == null) {
            return out;
        }
        Set<String> seen = new LinkedHashSet<String>();
        for (String line : content.split("\n")) {
            int idx = line.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String k = line.substring(0, idx).trim();
            if (k.equals(key)) {
                String val = line.substring(idx + 1).trim();
                if (!val.isEmpty()) {
                    seen.add(val);
                }
            }
        }
        out.addAll(seen);
        return out;
    }
}
