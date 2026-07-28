package com.universal.deviceinfo;

/**
 * Description of one mounted storage volume. Plain data holder (String-based)
 * so it stays android-free and testable; {@link VolumeUtil} fills it in.
 */
public final class VolumeInfo {
    public String root;      // e.g. /storage/emulated/0 or /storage/1A2B-3C4D
    public String appDir;    // app-specific dir on this volume, may be null
    public String label;     // human description, may be null
    public String uuid;      // volume uuid, may be null
    public String state;     // e.g. mounted
    public boolean primary;  // primary (internal/emulated) volume?
    public boolean removable;// removable (SD card / USB)?
    public boolean emulated; // emulated storage?

    /** A removable/secondary volume is the target the user asked to write to. */
    public boolean isSecondaryTarget() {
        return !primary || removable;
    }

    @Override
    public String toString() {
        return (root == null ? "?" : root)
                + (label != null ? " (" + label + ")" : "")
                + " [" + (primary ? "primary" : "secondary")
                + (removable ? ",removable" : "")
                + (emulated ? ",emulated" : "")
                + (state != null ? "," + state : "") + "]";
    }
}
