package dev.infrai.edtech;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class NightlySnapshotCommand {
    private NightlySnapshotCommand() {}

    public static void main(String[] args) throws Exception {
        SnapshotConfig config = SnapshotConfig.fromEnvironment();
        Clock clock = Clock.system(config.zone());
        LocalDate date = LocalDate.now(clock);

        CourseDeliverySnapshot input = new CourseDeliverySnapshot(
                date,
                Instant.now(clock),
                List.of(new CourseDeliverySnapshot.CourseDelivery("course-ledger-101", "cohort-2026-q3", "active")),
                List.of(new CourseDeliverySnapshot.LearnerDeadline("learner-1042", "course-ledger-101",
                        date.plusDays(2), "open")),
                new CourseDeliverySnapshot.EducatorReport("educator-17", 1, 0));

        NightlySnapshotService service = new NightlySnapshotService(
                config, new InfraiStorageClient(config), new NightlySnapshotPolicy());
        NightlySnapshotService.Result result = service.capture(input);
        System.out.println(Json.stringify(java.util.Map.of(
                "status", result.status(), "object_key", result.objectKey(), "reason", result.reason())));
    }
}
