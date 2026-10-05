package dev.infrai.edtech;

import java.time.LocalDate;

public final class NightlySnapshotPolicyTest {
    public static void main(String[] args) {
        NightlySnapshotPolicy policy = new NightlySnapshotPolicy();
        LocalDate date = LocalDate.of(2026, 9, 29);

        NightlySnapshotPolicy.Decision firstRun = policy.decide(date, false);
        require(firstRun.action() == NightlySnapshotPolicy.Action.WRITE, "first run must write");
        require(firstRun.objectKey().equals("nightly/2026-09-29/course-delivery.json"), "key must be date-scoped");

        NightlySnapshotPolicy.Decision repeatedRun = policy.decide(date, true);
        require(repeatedRun.action() == NightlySnapshotPolicy.Action.SKIP, "repeat must not overwrite the snapshot");

        System.out.println("PASS: first run writes; repeated business date skips");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
