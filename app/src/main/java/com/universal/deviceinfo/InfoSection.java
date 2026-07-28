package com.universal.deviceinfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A titled group of {@link InfoItem}s (e.g. "CPU", "Pantalla", "Batería").
 *
 * <p>Pure model (no android.* imports) so it can be unit-tested on any JVM.
 * The fluent {@code add(...)} overloads keep the collector code compact.
 */
public final class InfoSection {
    private final String title;
    private final List<InfoItem> items = new ArrayList<InfoItem>();

    public InfoSection(String title) {
        this.title = title == null ? "" : title;
    }

    public String getTitle() {
        return title;
    }

    public List<InfoItem> getItems() {
        return items;
    }

    public InfoSection add(String label, String value) {
        items.add(new InfoItem(label, value));
        return this;
    }

    public InfoSection add(String label, long value) {
        return add(label, String.valueOf(value));
    }

    public InfoSection add(String label, int value) {
        return add(label, String.valueOf(value));
    }

    public InfoSection add(String label, boolean value) {
        return add(label, Formats.yesNo(value));
    }

    /** Adds only if the value is meaningful (not null/blank/unknown). */
    public InfoSection addIfPresent(String label, String value) {
        if (value != null && !value.trim().isEmpty()
                && !Formats.UNKNOWN.equals(value.trim())) {
            add(label, value);
        }
        return this;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }
}
