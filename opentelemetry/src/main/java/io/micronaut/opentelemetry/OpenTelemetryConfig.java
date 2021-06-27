/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.opentelemetry;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.rxjava2.TracingAssembly;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

// TODO REMOVE ME PLEASE

/**
 * @author Alexey Zhokhov
 */
@Factory
public class OpenTelemetryConfig {

    @Bean
    @Context
    public TracingAssembly tracingAssembly() {
        TracingAssembly tracingAssembly = TracingAssembly.create();
        tracingAssembly.enable();
        return tracingAssembly;
    }

    @Bean
    @Singleton
    public SpanExporter otelSpanExporter() {
        return OtlpGrpcSpanExporter.builder()
                .setTimeout(Duration.ZERO)
                .build();
    }

    @Bean(preDestroy = "close")
    @Singleton
    public SpanProcessor otelSpanProcessor(SpanExporter spanExporter) {
        return BatchSpanProcessor.builder(spanExporter)
                .setScheduleDelay(Duration.ZERO)
                .setMaxExportBatchSize(1)
                .build();
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

    // we use @Context here because we need to register this bean globally just when the application is started
    // initialization
    @Bean
    @Context
    public OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(
                        TextMapPropagator.composite(
                                W3CTraceContextPropagator.getInstance(),
                                W3CBaggagePropagator.getInstance()
                        )
                ))
                .buildAndRegisterGlobal();
    }

    @Bean
    @Singleton
    public Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("micronaut-opentelemetry", "1.0.0");
    }

    @PreDestroy
    public void preDestroy(SpanProcessor spanProcessor, TracingAssembly tracingAssembly) {
        GlobalOpenTelemetry.resetForTest();
        tracingAssembly.disable();
    }

}
