package io.github.pi_java.agent.extension.api;

import java.util.Objects;

public record ExtensionApiVersion(int major, int minor, int patch) implements Comparable<ExtensionApiVersion> {

    public ExtensionApiVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("version numbers must not be negative");
        }
    }

    public static ExtensionApiVersion parse(String value) {
        String normalized = ExtensionStrings.requireNonBlank(value, "version").trim();
        String[] parts = normalized.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("version must use major.minor.patch format");
        }
        try {
            return new ExtensionApiVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("version must use numeric major.minor.patch format", ex);
        }
    }

    @Override
    public int compareTo(ExtensionApiVersion other) {
        Objects.requireNonNull(other, "other must not be null");
        int majorComparison = Integer.compare(major, other.major);
        if (majorComparison != 0) {
            return majorComparison;
        }
        int minorComparison = Integer.compare(minor, other.minor);
        if (minorComparison != 0) {
            return minorComparison;
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
