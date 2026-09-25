package com.example.androidfeasibility;

/** The hand-built prototype is debug-profile only; release signing is intentionally gated. */
public final class BuildProfile {
    public enum Variant { DEBUG, INTERNAL }
    public static final Variant CURRENT = Variant.DEBUG;

    private BuildProfile() { }

    public static boolean testControlsEnabled() { return CURRENT == Variant.DEBUG; }
}
