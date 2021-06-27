package io.micronaut.opentelemetry.instrumentation.http.server;

import io.micronaut.http.HttpRequest;
import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;

/**
 * @author Alexey Zhokhov
 */
public class HttpRequestExtractAdaptor implements TextMapGetter<HttpRequest> {

    public static final HttpRequestExtractAdaptor GETTER = new HttpRequestExtractAdaptor();

    @Override
    public Iterable<String> keys(HttpRequest carrier) {
        return carrier.getHeaders().names();
    }

    @Nullable
    @Override
    public String get(@Nullable HttpRequest carrier, String key) {
        if (carrier == null) {
            return null;
        }
        return carrier.getHeaders().get(key);
    }

}
