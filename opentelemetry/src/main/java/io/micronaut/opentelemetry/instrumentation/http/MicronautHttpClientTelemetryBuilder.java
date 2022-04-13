package io.micronaut.opentelemetry.instrumentation.http;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.PeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesExtractor;

import java.util.List;

public final class MicronautHttpClientTelemetryBuilder {

    private static final String INSTRUMENTATION_NAME = "io.micronaut.http.client";

    private final OpenTelemetry openTelemetry;
    private final HttpClientAttributesExtractorBuilder<HttpRequest, HttpResponse>
            httpAttributesExtractorBuilder =
            HttpClientAttributesExtractor.builder(MicronautHttpClientAttributesGetter.INSTANCE);

    public MicronautHttpClientTelemetryBuilder(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    /**
     * Configures the HTTP request headers that will be captured as span attributes.
     *
     * @param requestHeaders A list of HTTP header names.
     */
    public MicronautHttpClientTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
        httpAttributesExtractorBuilder.setCapturedRequestHeaders(requestHeaders);
        return this;
    }

    /**
     * Configures the HTTP response headers that will be captured as span attributes.
     *
     * @param responseHeaders A list of HTTP header names.
     */
    public MicronautHttpClientTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
        httpAttributesExtractorBuilder.setCapturedResponseHeaders(responseHeaders);
        return this;
    }

    public Instrumenter<MutableHttpRequest, HttpResponse> build() {
        MicronautHttpClientAttributesGetter httpAttributesGetter = MicronautHttpClientAttributesGetter.INSTANCE;
        MicronautHttpNetClientAttributesGetter netAttributesGetter = new MicronautHttpNetClientAttributesGetter();

        InstrumenterBuilder<MutableHttpRequest, HttpResponse> builder =
                Instrumenter.builder(openTelemetry, INSTRUMENTATION_NAME, HttpSpanNameExtractor.create(httpAttributesGetter));

        return builder.setSpanStatusExtractor(HttpSpanStatusExtractor.create(httpAttributesGetter))
                .addAttributesExtractor(httpAttributesExtractorBuilder.build())
                .addAttributesExtractor(NetClientAttributesExtractor.create(netAttributesGetter))
                .addAttributesExtractor(PeerServiceAttributesExtractor.create(netAttributesGetter))
                .addRequestMetrics(HttpClientMetrics.get())
                .newClientInstrumenter(HttpRequestSetter.INSTANCE);
    }

}
