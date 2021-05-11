/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.opentelemetry.interceptor;

import io.micronaut.aop.InterceptPhase;
import io.micronaut.aop.InterceptedMethod;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.util.StringUtils;
import io.micronaut.opentelemetry.annotation.ContinueSpan;
import io.micronaut.opentelemetry.annotation.NewSpan;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * FIXME
 * An interceptor that implements tracing logic for {@link io.micronaut.opentelemetry.annotation.ContinueSpan} and
 * {@link io.micronaut.opentelemetry.annotation.NewSpan}. Using the Open Tracing API.
 *
 * @author graemerocher
 * @since 1.0
 */
@Singleton
// FIXME
//@Requires(beans = Tracer.class)
public class TraceInterceptor implements MethodInterceptor<Object, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(TraceInterceptor.class);

    public static final String CLASS_TAG = "class";
    public static final String METHOD_TAG = "method";

    private static final String TAG_HYSTRIX_COMMAND = "hystrix.command";
    private static final String TAG_HYSTRIX_GROUP = "hystrix.group";
    private static final String TAG_HYSTRIX_THREAD_POOL = "hystrix.threadPool";
    private static final String HYSTRIX_ANNOTATION = "io.micronaut.configuration.hystrix.annotation.HystrixCommand";

    private final ConversionService<?> conversionService;

    // FIXME
    private OtlpGrpcSpanExporter otlpGrpcSpanExporter;
    private SdkTracerProvider sdkTracerProvider;
    private OpenTelemetry openTelemetry;

    /**
     * Initialize the interceptor with tracer and conversion service.
     *
     * @param conversionService A service to convert from one type to another
     */
    public TraceInterceptor(ConversionService<?> conversionService) {
        this.conversionService = conversionService;

        otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder().build();

        sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(otlpGrpcSpanExporter).build())
                .build();

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }

    @Override
    public int getOrder() {
        return InterceptPhase.TRACE.getPosition();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        boolean isContinue = context.hasAnnotation(ContinueSpan.class);
        AnnotationValue<NewSpan> newSpan = context.getAnnotation(NewSpan.class);
        boolean isNew = newSpan != null;
        if (!isContinue && !isNew) {
            return context.proceed();
        }

        // FIXME
        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");

        Span currentSpan = Span.current();

        if (!currentSpan.getSpanContext().isValid()) {
            currentSpan = null;
        }

        if (currentSpan != null) {
            LOG.debug("TRACE ID: {}", currentSpan.getSpanContext().getTraceId());
        }

        if (isContinue) {
            if (currentSpan == null) {
                return context.proceed();
            }
            InterceptedMethod interceptedMethod = InterceptedMethod.of(context);
            try {
                switch (interceptedMethod.resultType()) {
                    case PUBLISHER:
                        // FIXME
                        throw new UnsupportedOperationException();
                        /*
                        Flow.Publisher<?> publisher = interceptedMethod.interceptResultAsPublisher();
                        if (publisher instanceof TracingPublisher) {
                            return publisher;
                        }
                        return interceptedMethod.handleResult(
                                new TracingPublisher(publisher, tracer) {
                                    @Override
                                    protected void doOnSubscribe(@NonNull Span span) {
                                        tagArguments(span, context);
                                    }
                                }
                        );
                         */
                    case COMPLETION_STAGE:
                    case SYNCHRONOUS:
                        tagArguments(currentSpan, context);
                        try {
                            return context.proceed();
                        } catch (RuntimeException e) {
                            logError(currentSpan, e);
                            throw e;
                        }
                    default:
                        return interceptedMethod.unsupported();
                }
            } catch (Exception e) {
                return interceptedMethod.handleException(e);
            }
        } else {
            // must be new
            String operationName = newSpan.stringValue().orElse(null);
            Optional<String> hystrixCommand = context.stringValue(HYSTRIX_ANNOTATION);
            if (StringUtils.isEmpty(operationName)) {
                // try hystrix command name
                operationName = hystrixCommand.orElse(context.getMethodName());
            }
            SpanBuilder builder = tracer.spanBuilder(operationName);
            if (currentSpan != null) {
                // FIXME check
                builder.setParent(Context.current());
            }

            InterceptedMethod interceptedMethod = InterceptedMethod.of(context);
            try {
                switch (interceptedMethod.resultType()) {
                    case PUBLISHER:
                        throw new UnsupportedOperationException();
                        /*
                        Publisher<?> publisher = interceptedMethod.interceptResultAsPublisher();
                        if (publisher instanceof TracingPublisher) {
                            return publisher;
                        }
                        return interceptedMethod.handleResult(
                                new TracingPublisher(publisher, tracer, builder) {
                                    @Override
                                    protected void doOnSubscribe(@NonNull Span span) {
                                        populateTags(context, hystrixCommand, span);
                                    }
                                }
                        );
                         */
                    case COMPLETION_STAGE:
                        Span span = builder.startSpan();
                        if (span != null) {
                            LOG.debug("TRACE ID: {}", span.getSpanContext().getTraceId());
                        }
                        try (Scope scope = span.makeCurrent()) {
                            populateTags(context, hystrixCommand, span);
                            try {
                                CompletionStage<?> completionStage = interceptedMethod.interceptResultAsCompletionStage();
                                if (completionStage != null) {
                                    completionStage = completionStage.whenComplete((o, throwable) -> {
                                        if (throwable != null) {
                                            logError(span, throwable);
                                        }
                                        span.end();
                                    });
                                }
                                return interceptedMethod.handleResult(completionStage);
                            } catch (RuntimeException e) {
                                logError(span, e);
                                throw e;
                            }
                        }
                    case SYNCHRONOUS:
                        Span syncSpan = builder.startSpan();
                        if (syncSpan != null) {
                            LOG.debug("TRACE ID: {}", syncSpan.getSpanContext().getTraceId());
                        }
                        try (Scope scope = syncSpan.makeCurrent()) {
                            populateTags(context, hystrixCommand, syncSpan);
                            try {
                                return context.proceed();
                            } catch (RuntimeException e) {
                                logError(syncSpan, e);
                                throw e;
                            } finally {
                                syncSpan.end();
                            }
                        }
                    default:
                        return interceptedMethod.unsupported();
                }
            } catch (Exception e) {
                return interceptedMethod.handleException(e);
            }
        }
    }

    private void populateTags(MethodInvocationContext<Object, Object> context, Optional<String> hystrixCommand, Span span) {
        span.setAttribute(CLASS_TAG, context.getDeclaringType().getSimpleName());
        span.setAttribute(METHOD_TAG, context.getMethodName());
        hystrixCommand.ifPresent(s -> span.setAttribute(TAG_HYSTRIX_COMMAND, s));
        context.stringValue(HYSTRIX_ANNOTATION, "group").ifPresent(s ->
                span.setAttribute(TAG_HYSTRIX_GROUP, s)
        );
        context.stringValue(HYSTRIX_ANNOTATION, "threadPool").ifPresent(s ->
                span.setAttribute(TAG_HYSTRIX_THREAD_POOL, s)
        );
        tagArguments(span, context);
    }

    /**
     * Logs an error to the span.
     *
     * @param span The span
     * @param e    The error
     */
    public static void logError(Span span, Throwable e) {
        if (e.getMessage() != null) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
        } else {
            span.setStatus(StatusCode.ERROR);
        }
        // FIXME add error object
        /*
        HashMap<String, Object> fields = new HashMap<>(2);
        fields.put(Fields.ERROR_OBJECT, e);
        String message = e.getMessage();
        if (message != null) {
            fields.put(Fields.MESSAGE, message);
        }
        span.log(fields);
         */
    }

    private void tagArguments(Span span, MethodInvocationContext<Object, Object> context) {
        // FIXME
        /*
        Argument[] arguments = context.getArguments();
        Object[] parameterValues = context.getParameterValues();
        for (int i = 0; i < arguments.length; i++) {
            Argument argument = arguments[i];
            AnnotationMetadata annotationMetadata = argument.getAnnotationMetadata();
            if (annotationMetadata.hasAnnotation(SpanTag.class)) {
                Object v = parameterValues[i];
                if (v != null) {
                    String tagName = annotationMetadata.stringValue(SpanTag.class).orElse(argument.getName());
                    span.setTag(tagName, v.toString());
                }
            }
        }
         */
    }

}
