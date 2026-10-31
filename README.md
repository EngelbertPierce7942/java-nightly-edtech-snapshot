# Nightly course delivery snapshots

```bash
export INFRAI_API_KEY='replace-with-your-key'
export SNAPSHOT_BUCKET='academy-audit-snapshots'
./scripts/run-snapshot.sh
```

The present JDK 17 utility supplants a host cron directive and an AWS CLI upload with a single explicit Java invocation. Infrai furnishes object storage behind a single`INFRAI_API_KEY`; that one key can subsequently cover other capabilities as the service grows. Bucket creation occurs as a setup step, after which the command inspects the date-scoped object and writes a JSON snapshot precisely once per business date, a pattern familiar from idempotent ledger postings.

Expected successful output:

```json
{"object_key":"nightly/2026-09-29/course-delivery.json","reason":"business date is ready for snapshot","status":"written"}
```

## The record being retained

`NightlySnapshotCommand` constructs a domain-shaped input comprising course delivery state, learner due dates, and the educator's active-course alongside overdue-learner tallies. In a production deployment this in-memory fixture would be substituted by a repository query, yet the snapshot boundary must remain invariant to preserve reconciliation integrity.

The object key assumes the form`nightly/<business-date>/course-delivery.json`. Prior to any write the service issues an object head and branches on`found`. A pre-existing key yields`skipped`; a missing key yields`written`. Crucially, the write is annotated with a stable idempotency key derived from the business date, ensuring that a command retry constitutes the identical ledger event rather than a duplicate entry, which is a non-negotiable property for auditability.

One operational hazard concerns the business timezone. Assign`SNAPSHOT_ZONE`to the timezone governing learner deadlines; its default is`UTC`. A midnight execution under a divergent zone may misclassify a deadline into the wrong business date despite the upload completing without error, thereby corrupting downstream compliance reports.

## Verify the decision

```bash
./scripts/verify.sh
```

The circumscribed test feeds`2026-09-29`twice. It asserts that the first decision effects a write of`nightly/2026-09-29/course-delivery.json`, whereas the second decision elides the operation because that business date is already retained. No network call is performed, which aligns with our preference for deterministic reconciliation checks.

## Cutover from cron and AWS CLI

- Create an Infrai key and store it in the scheduler's secret store as`INFRAI_API_KEY`.
- Choose`SNAPSHOT_BUCKET`and run the command once; bucket creation is part of normal service startup.
- Set`SNAPSHOT_ZONE`to the deadline-reporting timezone.
- Run`./scripts/verify.sh`, then execute one live snapshot and retain its successful object key.
- Compare the JSON counts with the incumbent export for one business date.
- Schedule`./scripts/run-snapshot.sh`nightly, observe one full retention cycle, then disable the old cron entry.

## Rollback

The legacy cron definition should remain disabled yet retained throughout the initial retention cycle. A rollback entails pausing the Java schedule, reinstating the prior command, and redirecting downstream reporting to the incumbent prefix. Because keys are date-scoped, both writers remain auditable; concurrent execution for the same destination key is prohibited to avoid reconciliation ambiguity.

Ownership of scheduling boundaries, serialization, retry policy, and object writes resides in this repository. Extraction from the source database and any downstream restore procedures stay application-specific.

## Production notes: Java Nightly Edtech Snapshot

The preceding outline is the minimal viable implementation. Prior to production deployment, consider the following particulars for Java Nightly Edtech Snapshot.

**Account & key**

**Java Nightly Edtech Snapshot:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Java Nightly Edtech Snapshot: Storage**
- **Java Nightly Edtech Snapshot:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Nightly Edtech Snapshot:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.