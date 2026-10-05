package dev.infrai.edtech;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiStorageClient {
    private final SnapshotConfig config;
    private final HttpClient http;

    public InfraiStorageClient(SnapshotConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    // infrai.storage.bucket.create
    public void createBucket(String name) throws IOException, InterruptedException {
        call("POST", "/v1/storage/bucket/create", Map.of("name", name));
    }

    // infrai.storage.object.head
    public boolean objectExists(String bucket, String key) throws IOException, InterruptedException {
        Map<String, Object> data = call("GET", "/v1/storage/object/head/" + segment(bucket) + "/" + path(key), null);
        return Boolean.TRUE.equals(data.get("found"));
    }

    // infrai.storage.object.put
    public void putJson(String bucket, String key, byte[] json, String idempotencyKey)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data_base64", Base64.getEncoder().encodeToString(json));
        body.put("content_type", "application/json");
        body.put("idempotency_key", idempotencyKey);
        call("PUT", "/v1/storage/object/put/" + segment(bucket) + "/" + path(key), body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, String apiPath, Map<String, Object> body)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(config.apiBase().resolve(apiPath))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json");
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(Json.stringify(body)));
            }

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Object decoded = Json.parse(response.body());
            if (!(decoded instanceof Map<?, ?> rawEnvelope)) {
                throw new IOException("Infrai response is not an envelope");
            }
            Map<String, Object> envelope = (Map<String, Object>) rawEnvelope;

            if (response.statusCode() == 429 && attempt < 3) {
                Thread.sleep(retryDelayMillis(response, attempt));
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Map<String, Object> error = envelope.get("error") instanceof Map<?, ?> raw
                        ? (Map<String, Object>) raw : Map.of();
                throw new InfraiException(response.statusCode(), String.valueOf(error.get("code")),
                        String.valueOf(error.getOrDefault("message", error.get("hint"))));
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport status " + response.statusCode());
            }
            Object data = envelope.get("data");
            return data instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        }
        throw new IOException("Retry budget exhausted");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> {
                    try { return Math.max(1L, Long.parseLong(value)) * 1_000L; }
                    catch (NumberFormatException ignored) { return 1_000L << attempt; }
                })
                .orElse(1_000L << attempt);
    }

    private static String path(String value) {
        return java.util.Arrays.stream(value.split("/", -1)).map(InfraiStorageClient::segment)
                .reduce((left, right) -> left + "/" + right).orElse("");
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static final class InfraiException extends IOException {
        private final int status;
        private final String code;

        InfraiException(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }

        public int status() { return status; }
        public String code() { return code; }
    }
}
