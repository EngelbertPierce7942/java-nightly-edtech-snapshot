package dev.infrai.edtech;

import java.time.LocalDate;

public final class NightlySnapshotPolicy {
    public Decision decide(LocalDate businessDate, boolean snapshotAlreadyExists) {
        String key = "nightly/" + businessDate + "/course-delivery.json";
        return snapshotAlreadyExists
                ? new Decision(Action.SKIP, key, "snapshot already recorded for business date")
                : new Decision(Action.WRITE, key, "business date is ready for snapshot");
    }

    public enum Action { WRITE, SKIP }
    public record Decision(Action action, String objectKey, String reason) {}
}
