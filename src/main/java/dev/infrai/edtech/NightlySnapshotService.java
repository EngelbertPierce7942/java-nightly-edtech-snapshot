package dev.infrai.edtech;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class NightlySnapshotService {
    private final SnapshotConfig config;
    private final InfraiStorageClient storage;
    private final NightlySnapshotPolicy policy;

    public NightlySnapshotService(SnapshotConfig config, InfraiStorageClient storage, NightlySnapshotPolicy policy) {
        this.config = config;
        this.storage = storage;
        this.policy = policy;
    }

    public Result capture(CourseDeliverySnapshot snapshot) throws IOException, InterruptedException {
        storage.createBucket(config.bucket());
        String key = "nightly/" + snapshot.businessDate() + "/course-delivery.json";
        boolean exists = storage.objectExists(config.bucket(), key);
        NightlySnapshotPolicy.Decision decision = policy.decide(snapshot.businessDate(), exists);
        if (decision.action() == NightlySnapshotPolicy.Action.SKIP) {
            return new Result("skipped", decision.objectKey(), decision.reason());
        }

        byte[] payload = Json.stringify(snapshot.asJson()).getBytes(StandardCharsets.UTF_8);
        String idempotencyKey = "edtech-snapshot-" + snapshot.businessDate();
        storage.putJson(config.bucket(), decision.objectKey(), payload, idempotencyKey);
        return new Result("written", decision.objectKey(), decision.reason());
    }

    public record Result(String status, String objectKey, String reason) {}
}
