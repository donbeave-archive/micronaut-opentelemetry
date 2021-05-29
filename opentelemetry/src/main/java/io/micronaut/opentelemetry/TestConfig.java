package io.micronaut.opentelemetry;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import javax.inject.Singleton;

// TODO REMOVE ME PLEASE

/**
 * @author Alexey Zhokhov
 */
@Factory
public class TestConfig {

    @Bean
    @Singleton
    public SpanExporter otelSpanExporter() {
        return OtlpGrpcSpanExporter.builder().build();
    }

    @Bean
    @Singleton
    public SpanProcessor otelSpanProcessor(SpanExporter spanExporter) {
        //return BatchSpanProcessor.builder(spanExporter).build();
        return SimpleSpanProcessor.create(spanExporter);
    }

    @Bean
    @Singleton
    public SdkTracerProvider otelSdkTraceProvider(SpanProcessor spanProcessor) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .build();
    }

    @Bean
    @Singleton
    public OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }

    @Bean
    @Singleton
    public Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("micronaut-opentelemetry", "1.0.0");
    }

}
