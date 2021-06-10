package io.micronaut.opentelemetry.annotation;

import io.micronaut.aop.Around;
import io.micronaut.context.annotation.Type;
import io.micronaut.core.annotation.Internal;
import io.micronaut.opentelemetry.api.SpanKind;
import io.micronaut.opentelemetry.interceptor.TraceInterceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO documentation
 *
 * @author Alexey Zhokhov
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Around
@Type(TraceInterceptor.class)
@Internal
public @interface WithSpanAdvice {

    /**
     * Optional name of the created span.
     * <p>
     * TODO: description
     *
     * <p>If not specified, an appropriate default name should be created by auto-instrumentation.
     * E.g. {@code "className"."method"}
     */
    String value() default "";

    /**
     * Specify the {@link SpanKind} of span to be created. Defaults to {@link SpanKind#INTERNAL}.
     */
    SpanKind kind() default SpanKind.INTERNAL;

}
