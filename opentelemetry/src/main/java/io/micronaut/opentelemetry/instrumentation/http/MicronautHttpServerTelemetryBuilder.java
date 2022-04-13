package io.micronaut.opentelemetry.instrumentation.http;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpRouteHolder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetServerAttributesExtractor;

import java.util.List;

public final class MicronautHttpServerTelemetryBuilder {

    private static final String INSTRUMENTATION_NAME = "io.micronaut.http.server";

    private final OpenTelemetry openTelemetry;
    private final HttpServerAttributesExtractorBuilder<HttpRequest, HttpResponse>
            httpAttributesExtractorBuilder =
            HttpServerAttributesExtractor.builder(MicronautHttpServerAttributesGetter.INSTANCE);

    public MicronautHttpServerTelemetryBuilder(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    /**
     * Configures the HTTP request headers that will be captured as span attributes.
     *
     * @param requestHeaders A list of HTTP header names.
     */
    public MicronautHttpServerTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
        httpAttributesExtractorBuilder.setCapturedRequestHeaders(requestHeaders);
        return this;
    }

    /**
     * Configures the HTTP response headers that will be captured as span attributes.
     *
     * @param responseHeaders A list of HTTP header names.
     */
    public MicronautHttpServerTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
        httpAttributesExtractorBuilder.setCapturedResponseHeaders(responseHeaders);
        return this;
    }

    public Instrumenter<HttpRequest, HttpResponse> build() {
        MicronautHttpServerAttributesGetter httpAttributesGetter = MicronautHttpServerAttributesGetter.INSTANCE;

        InstrumenterBuilder<HttpRequest, HttpResponse> builder =
                Instrumenter.builder(openTelemetry, INSTRUMENTATION_NAME,
                        HttpSpanNameExtractor.create(httpAttributesGetter));

        return builder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(httpAttributesGetter))
                .addAttributesExtractor(httpAttributesExtractorBuilder.build())
                .addAttributesExtractor(
                        NetServerAttributesExtractor.create(new MicronautHttpNetServerAttributesGetter()))
                .addRequestMetrics(HttpServerMetrics.get())
                .addContextCustomizer(HttpRouteHolder.get())
                .newServerInstrumenter(HttpRequestGetter.INSTANCE);
    }

}
