package io.micronaut.opentelemetry.instrument.http;

import io.micronaut.http.HttpHeaders;
import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;

/**
 * @author Alexey Zhokhov
 */
public class HttpHeadersTextMapGetter implements TextMapGetter<HttpHeaders> {

    @Override
    public Iterable<String> keys(HttpHeaders carrier) {
        return carrier.names();
    }

    @Nullable
    @Override
    public String get(@Nullable HttpHeaders carrier, String key) {
        return carrier.get(key);
    }

}
