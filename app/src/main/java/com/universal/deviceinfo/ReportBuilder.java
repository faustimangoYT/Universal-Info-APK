package com.universal.deviceinfo;

import java.util.List;

/**
 * Turns the collected {@link InfoSection}s into the plain-text report that is
 * written to storage. Pure (no android.* imports) and unit-testable.
 */
public final class ReportBuilder {

    private static final int WIDTH = 74;

    private ReportBuilder() {
    }

    public static String build(String title, String generatedAt, List<InfoSection> sections) {
        StringBuilder sb = new StringBuilder(8192);
        String bar = repeat('=', WIDTH);
        sb.append(bar).append('\n');
        sb.append(center(title, WIDTH)).append('\n');
        if (generatedAt != null && !generatedAt.isEmpty()) {
            sb.append(center(generatedAt, WIDTH)).append('\n');
        }
        sb.append(bar).append("\n\n");

        int n = 0;
        int totalItems = 0;
        for (InfoSection s : sections) {
            n++;
            sb.append(repeat('-', WIDTH)).append('\n');
            sb.append("  [").append(n).append("] ").append(s.getTitle()).append('\n');
            sb.append(repeat('-', WIDTH)).append('\n');
            for (InfoItem it : s.getItems()) {
                totalItems++;
                String v = it.getValue() == null ? "" : it.getValue();
                if (v.indexOf('\n') >= 0) {
                    sb.append("  ").append(it.getLabel()).append(" :\n");
                    for (String ln : v.split("\n")) {
                        sb.append("        ").append(ln).append('\n');
                    }
                } else {
                    sb.append("  ").append(padRight(it.getLabel(), 30))
                      .append(" : ").append(v).append('\n');
                }
            }
            sb.append('\n');
        }

        sb.append(bar).append('\n');
        sb.append(center("Fin - " + sections.size() + " secciones, "
                + totalItems + " campos", WIDTH)).append('\n');
        sb.append(bar).append('\n');
        return sb.toString();
    }

    private static String repeat(char c, int count) {
        StringBuilder b = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            b.append(c);
        }
        return b.toString();
    }

    private static String padRight(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        StringBuilder b = new StringBuilder(s);
        while (b.length() < width) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String center(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        int left = (width - s.length()) / 2;
        return repeat(' ', left) + s;
    }
}
