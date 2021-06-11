package io.micronaut.opentelemetry;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

// TODO REMOVE ME PLEASE

/**
 * @author Alexey Zhokhov
 */
@Factory
public class OpenTelemetryConfig {

    @Bean
    @Singleton
    public SpanExporter otelSpanExporter() {
        return new SpanExporter() {
            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                for (SpanData spanData : spans) {
                    System.out.println("EXPORT SPAN:");
                    System.out.println(spanData.toString());
                }
                return CompletableResultCode.ofSuccess();
            }

            @Override
            public CompletableResultCode flush() {
                return CompletableResultCode.ofSuccess();
            }

            @Override
            public CompletableResultCode shutdown() {
                return CompletableResultCode.ofSuccess();
            }
        };
        //return OtlpGrpcSpanExporter.builder().build();
    }

    @Bean
    @Singleton
    public SpanProcessor otelSpanProcessor(SpanExporter spanExporter) {
        return SimpleSpanProcessor.create(spanExporter);
        //return BatchSpanProcessor.builder(spanExporter).setMaxQueueSize(1).setMaxExportBatchSize(1).build();
    }

    @Inject
    @Bean
    @Singleton
    public SdkTracerProvider otelSdkTraceProvider(SpanProcessor spanProcessor, ApplicationContext applicationContext) {
        String applicationName = applicationContext.getProperty("micronaut.application.name", String.class)
                .orElse(io.micronaut.context.env.Environment.DEFAULT_NAME);

        return SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .setResource(Resource.getDefault().toBuilder().put(SERVICE_NAME, applicationName).build())
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

    @PreDestroy
    public void preDestroy() {
        GlobalOpenTelemetry.resetForTest();
    }

}
