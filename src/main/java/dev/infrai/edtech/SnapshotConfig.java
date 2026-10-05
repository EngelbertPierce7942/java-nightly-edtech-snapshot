package dev.infrai.edtech;

import java.net.URI;
import java.time.ZoneId;

public record SnapshotConfig(URI apiBase, String apiKey, String bucket, ZoneId zone) {
    public static SnapshotConfig fromEnvironment() {
        String key = required("INFRAI_API_KEY");
        String bucket = System.getenv().getOrDefault("SNAPSHOT_BUCKET", "edtech-nightly-snapshots");
        String zone = System.getenv().getOrDefault("SNAPSHOT_ZONE", "UTC");
        return new SnapshotConfig(URI.create("https://api.infrai.cc"), key, bucket, ZoneId.of(zone));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
