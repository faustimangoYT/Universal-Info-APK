package com.universal.deviceinfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure (android-free) search / category filtering used by the UI, so the
 * "buscador" and category logic can be unit-tested on any JVM.
 */
public final class Filters {

    private Filters() {
    }

    /**
     * Sections belonging to a category. {@code cat[0]} is the display name and
     * {@code cat[1..]} are the section titles to include, in order.
     */
    public static List<InfoSection> byTitles(List<InfoSection> base, String[] cat) {
        List<InfoSection> out = new ArrayList<InfoSection>();
        for (int i = 1; i < cat.length; i++) {
            for (InfoSection s : base) {
                if (s.getTitle().equals(cat[i])) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    /**
     * Case-insensitive search. A section matches wholly if its title contains
     * the query; otherwise only its matching items are kept. Empty query returns
     * everything unchanged.
     */
    public static List<InfoSection> search(List<InfoSection> base, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return new ArrayList<InfoSection>(base);
        }
        List<InfoSection> out = new ArrayList<InfoSection>();
        for (InfoSection s : base) {
            if (s.getTitle().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(s);
                continue;
            }
            InfoSection filtered = new InfoSection(s.getTitle());
            for (InfoItem it : s.getItems()) {
                if (it.getLabel().toLowerCase(Locale.ROOT).contains(q)
                        || it.getValue().toLowerCase(Locale.ROOT).contains(q)) {
                    filtered.add(it.getLabel(), it.getValue());
                }
            }
            if (!filtered.isEmpty()) {
                out.add(filtered);
            }
        }
        return out;
    }

    /** First value whose label contains {@code labelContains} in the named section. */
    public static String value(List<InfoSection> all, String sectionTitle, String labelContains) {
        String want = labelContains.toLowerCase(Locale.ROOT);
        for (InfoSection s : all) {
            if (!s.getTitle().equals(sectionTitle)) {
                continue;
            }
            for (InfoItem it : s.getItems()) {
                if (it.getLabel().toLowerCase(Locale.ROOT).contains(want)) {
                    return it.getValue();
                }
            }
        }
        return null;
    }
}
