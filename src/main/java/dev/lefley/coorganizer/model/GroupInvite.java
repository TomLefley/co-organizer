package dev.lefley.coorganizer.model;

public class GroupInvite {
    private final String name;
    private final String key;
    private final String fingerprint;

    public GroupInvite(String name, String key, String fingerprint) {
        this.name = name;
        this.key = key;
        this.fingerprint = fingerprint;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public Group toGroup() {
        return new Group(name, key, fingerprint);
    }
}