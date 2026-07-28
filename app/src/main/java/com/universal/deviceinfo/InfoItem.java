package com.universal.deviceinfo;

/**
 * A single label -> value pair of device information.
 *
 * <p>Pure model (no android.* imports) so it can be unit-tested on any JVM.
 */
public final class InfoItem {
    private final String label;
    private final String value;

    public InfoItem(String label, String value) {
        this.label = label == null ? "" : label;
        this.value = value == null ? Formats.UNKNOWN : value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}
