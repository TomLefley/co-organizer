package dev.lefley.coorganizer.model;

import java.util.Objects;

public class Group {
    private final String name;
    private final String symmetricKey;
    private final String fingerprint;
    private final long createdAt;

    public Group(String name, String symmetricKey, String fingerprint) {
        this.name = name;
        this.symmetricKey = symmetricKey;
        this.fingerprint = fingerprint;
        this.createdAt = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public String getSymmetricKey() {
        return symmetricKey;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(fingerprint, group.fingerprint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fingerprint);
    }

    @Override
    public String toString() {
        return "Group{" +
                "name='" + name + '\'' +
                ", fingerprint='" + fingerprint + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}