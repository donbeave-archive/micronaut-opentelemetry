/*
 * Copyright 2020 original authors
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
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.opentelemetry.annotation.WithSpanAdvice;
import io.micronaut.opentelemetry.instrumentation.util.MicronautTracer;
import io.micronaut.opentelemetry.instrumentation.util.TracingPublisher;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.tracer.ClientSpan;
import io.opentelemetry.instrumentation.api.tracer.ServerSpan;
import io.opentelemetry.instrumentation.api.tracer.SpanNames;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * TODO description
 * <p>
 * Mostly based on the code from the official opentelemetry-java-instrumentation library.
 *
 * @author Alexey Zhokhov
 * @since 1.0
 */
@Singleton
@Requires(beans = Tracer.class)
public class TraceInterceptor implements MethodInterceptor<Object, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(TraceInterceptor.class);

    private static final String KIND_MEMBER = "kind";

    private final MicronautTracer tracer;

    public TraceInterceptor(MicronautTracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public int getOrder() {
        return InterceptPhase.TRACE.getPosition();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object intercept(MethodInvocationContext<Object, Object> invocationContext) {
        AnnotationValue<WithSpanAdvice> withSpanAdvice = invocationContext.getAnnotation(WithSpanAdvice.class);

        SpanKind kind = extractSpanKind(withSpanAdvice);
        Context current = Context.current();

        // don't create a nested span if you're not supposed to.
        if (!tracer.shouldStartSpan(current, kind)) {
            return invocationContext.proceed();
        }

        InterceptedMethod interceptedMethod = InterceptedMethod.of(invocationContext);
        try {
            switch (interceptedMethod.resultType()) {
                case PUBLISHER:
                    Publisher<?> publisher = interceptedMethod.interceptResultAsPublisher();
                    if (publisher instanceof TracingPublisher) {
                        return publisher;
                    }
                    return interceptedMethod.handleResult(
                            new TracingPublisher(
                                    publisher, tracer, spanNameForMethodWithAnnotation(withSpanAdvice, invocationContext.getExecutableMethod()),
                                    kind, current
                            ) {
                                @Override
                                protected void doOnSubscribe(@NonNull Context span) {
                                    populateTags(invocationContext, span);
                                }
                            }
                    );
                case COMPLETION_STAGE:
                    Context completionStageContext =
                            startSpan(current, withSpanAdvice, invocationContext.getExecutableMethod(), kind);

                    try (Scope ignored = completionStageContext.makeCurrent()) {
                        populateTags(invocationContext, completionStageContext);
                        try {
                            CompletionStage<?> completionStage = interceptedMethod.interceptResultAsCompletionStage();
                            if (completionStage != null) {
                                completionStage = completionStage.whenComplete((o, throwable) -> {
                                    if (throwable != null) {
                                        tracer.endExceptionally(completionStageContext, throwable);
                                    } else {
                                        tracer.end(completionStageContext);
                                    }
                                });
                            }
                            return interceptedMethod.handleResult(completionStage);
                        } catch (RuntimeException e) {
                            tracer.endExceptionally(completionStageContext, e);
                            throw e;
                        }
                    }
                case SYNCHRONOUS:
                    Context syncContext =
                            startSpan(current, withSpanAdvice, invocationContext.getExecutableMethod(), kind);

                    try (Scope ignored = syncContext.makeCurrent()) {
                        populateTags(invocationContext, syncContext);
                        return invocationContext.proceed();
                    } catch (RuntimeException e) {
                        tracer.onException(syncContext, e);
                        throw e;
                    } finally {
                        tracer.end(syncContext);
                    }
                default:
                    return interceptedMethod.unsupported();
            }
        } catch (Exception e) {
            return interceptedMethod.handleException(e);
        }
    }

    private void populateTags(MethodInvocationContext<Object, Object> context, Context otelContext) {
        Span span = Span.fromContext(otelContext);

        span.setAttribute(SemanticAttributes.CODE_NAMESPACE, context.getDeclaringType().getName());
        span.setAttribute(SemanticAttributes.CODE_FUNCTION, context.getMethodName());
    }

    private Context startSpan(Context parentContext, AnnotationValue<WithSpanAdvice> withSpanAdvice,
                              ExecutableMethod executableMethod, SpanKind kind) {
        Context context = tracer
                .startSpan(parentContext, spanNameForMethodWithAnnotation(withSpanAdvice, executableMethod), kind);
        Span span = Span.fromContext(context);
        if (kind == SpanKind.SERVER) {
            return ServerSpan.with(parentContext.with(span), span);
        }
        if (kind == SpanKind.CLIENT) {
            return ClientSpan.with(parentContext.with(span), span);
        }
        return parentContext.with(span);
    }

    /**
     * This method is used to generate an acceptable span (operation) name based on a given method
     * reference. It first checks for existence of {@link WithSpanAdvice} annotation. If it is present, then
     * tries to derive name from its {@code value} attribute. Otherwise delegates to spanNameForMethod(Method).
     */
    private String spanNameForMethodWithAnnotation(AnnotationValue<WithSpanAdvice> withSpanAdvice,
                                                   ExecutableMethod executableMethod) {
        Optional<String> value = withSpanAdvice.getValue(String.class);
        return value.orElseGet(() -> SpanNames.fromMethod(executableMethod.getTargetMethod()));
    }

    private SpanKind extractSpanKind(AnnotationValue<WithSpanAdvice> withSpanAdvice) {
        io.micronaut.opentelemetry.api.SpanKind applicationKind = withSpanAdvice
                .get(KIND_MEMBER, io.micronaut.opentelemetry.api.SpanKind.class)
                .orElse(io.micronaut.opentelemetry.api.SpanKind.INTERNAL);

        return toAgentOrNull(applicationKind);
    }

    private SpanKind toAgentOrNull(io.micronaut.opentelemetry.api.SpanKind applicationSpanKind) {
        try {
            return SpanKind.valueOf(applicationSpanKind.name());
        } catch (IllegalArgumentException e) {
            LOG.debug("unexpected span kind: {}", applicationSpanKind.name());
            return SpanKind.INTERNAL;
        }
    }

}
