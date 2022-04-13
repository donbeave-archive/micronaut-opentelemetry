package io.micronaut.opentelemetry.instrumentation.http;

import io.micronaut.http.MutableHttpRequest;
import io.opentelemetry.context.propagation.TextMapSetter;

enum HttpRequestSetter implements TextMapSetter<MutableHttpRequest> {
    INSTANCE;

    @Override
    public void set(MutableHttpRequest httpRequest, String key, String value) {
        httpRequest.header(key, value);
    }
}
