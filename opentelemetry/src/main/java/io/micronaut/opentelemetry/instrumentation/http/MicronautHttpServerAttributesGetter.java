package io.micronaut.opentelemetry.instrumentation.http;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import javax.annotation.Nullable;
import java.util.List;

enum MicronautHttpServerAttributesGetter implements HttpServerAttributesGetter<HttpRequest, HttpResponse> {

    INSTANCE;

    @Override
    public String method(HttpRequest request) {
        return request.getMethodName();
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
    public Integer statusCode(HttpRequest request, HttpResponse response) {
        return response.code();
    }

    @Override
    @Nullable
    public Long responseContentLength(HttpRequest request, HttpResponse response) {
        return null;
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

    @Override
    @SuppressWarnings("UnnecessaryDefaultInEnumSwitch")
    @Nullable
    public String flavor(HttpRequest request) {
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
    public String target(HttpRequest request) {
        String requestPath = request.getPath();
        String queryString = request.getUri().getRawQuery();
        if (queryString != null && !queryString.isEmpty()) {
            return requestPath + "?" + queryString;
        }
        return requestPath;
    }

    @Override
    @Nullable
    public String route(HttpRequest request) {
        return null;
    }

    @Override
    public String scheme(HttpRequest request) {
        return request.getUri().getScheme();
    }

    @Override
    @Nullable
    public String serverName(HttpRequest request) {
        return null;
    }

}
