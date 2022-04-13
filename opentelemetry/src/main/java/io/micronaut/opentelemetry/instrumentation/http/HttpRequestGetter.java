package io.micronaut.opentelemetry.instrumentation.http;

import io.micronaut.http.HttpRequest;
import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;

enum HttpRequestGetter implements TextMapGetter<HttpRequest> {
    INSTANCE;

    @Override
    public Iterable<String> keys(HttpRequest request) {
        return request.getHeaders().names();
    }

    @Override
    @Nullable
    public String get(@Nullable HttpRequest request, String key) {
        if (request == null) {
            return null;
        }
        return request.getHeaders().getFirst(key).orElse(null);
    }
}
