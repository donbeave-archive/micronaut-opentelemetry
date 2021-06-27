package io.micronaut.opentelemetry.instrumentation.util;

import io.micronaut.context.annotation.Requires;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.tracer.BaseTracer;

import javax.inject.Singleton;

/**
 * @author Alexey Zhokhov
 */
@Singleton
@Requires(beans = OpenTelemetry.class)
public class MicronautTracer extends BaseTracer {

    public MicronautTracer(OpenTelemetry openTelemetry) {
        super(openTelemetry);
    }

    @Override
    protected String getInstrumentationName() {
        return "io.micronaut.opentelemetry.micronaut-opentelemetry-1.0";
    }

}
