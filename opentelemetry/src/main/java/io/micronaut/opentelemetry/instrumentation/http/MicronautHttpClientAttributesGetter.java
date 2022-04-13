package io.micronaut.opentelemetry.instrumentation.http;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import javax.annotation.Nullable;
import java.util.List;

enum MicronautHttpClientAttributesGetter implements HttpClientAttributesGetter<HttpRequest, HttpResponse> {

    INSTANCE;

    @Override
    public String method(HttpRequest request) {
        return request.getMethodName();
    }

    @Override
    public String url(HttpRequest request) {
        return request.getUri().toString();
    }

    @Override
    public List<String> requestHeader(HttpRequest request, String name) {
        return request.getHeaders().getAll(name);
    }

    @Override
    public Long requestContentLength(HttpRequest request, @Nullable HttpResponse response) {
        return request.getContentLength();
    }

    @Override
    @Nullable
    public Long requestContentLengthUncompressed(HttpRequest request, @Nullable HttpResponse response) {
        return null;
    }

    @Override
    @SuppressWarnings("UnnecessaryDefaultInEnumSwitch")
    @Nullable
    public String flavor(HttpRequest request, @Nullable HttpResponse response) {
        switch (request.getHttpVersion()) {
            case HTTP_1_0:
                return SemanticAttributes.HttpFlavorValues.HTTP_1_0;
            case HTTP_1_1:
                return SemanticAttributes.HttpFlavorValues.HTTP_1_1;
            case HTTP_2_0:
                return SemanticAttributes.HttpFlavorValues.HTTP_2_0;
            default:
                return null;
        }
    }

    @Override
    public Integer statusCode(HttpRequest request, HttpResponse response) {
        return response.code();
    }

    @Override
    public Long responseContentLength(HttpRequest request, HttpResponse response) {
        return response.getContentLength();
    }

    @Override
    @Nullable
    public Long responseContentLengthUncompressed(HttpRequest request, HttpResponse response) {
        return null;
    }

    @Override
    public List<String> responseHeader(HttpRequest request, HttpResponse response, String name) {
        return response.getHeaders().getAll(name);
    }

}
