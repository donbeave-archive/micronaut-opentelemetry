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
package io.micronaut.opentelemetry.instrument.util;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.MutableHttpResponse;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Optional;

import static io.micronaut.opentelemetry.interceptor.TraceInterceptor.logError;

/**
 * A reactive streams publisher that traces.
 *
 * @param <T> the type of element signaled
 * @author Alexey Zhokhov
 * @since 1.0
 */
@SuppressWarnings("PublisherImplementation")
public class TracingPublisher<T> implements Publishers.MicronautPublisher<T> {

    private final Publisher<T> publisher;
    private final Tracer tracer;
    private final SpanBuilder spanBuilder;
    private final Context parentContext;
    private final boolean isSingle;

    /**
     * Creates a new tracing publisher for the given arguments.
     *
     * @param publisher     The target publisher
     * @param tracer        The tracer
     * @param operationName The operation name that should be started
     */
    public TracingPublisher(Publisher<T> publisher, Tracer tracer, String operationName) {
        this(publisher, tracer, tracer.spanBuilder(operationName));
    }

    /**
     * Creates a new tracing publisher for the given arguments. This constructor will just add tracing of the
     * existing span if it is present.
     *
     * @param publisher The target publisher
     * @param tracer    The tracer
     */
    public TracingPublisher(Publisher<T> publisher, Tracer tracer) {
        this(publisher, tracer, (SpanBuilder) null);
    }

    /**
     * Creates a new tracing publisher for the given arguments.
     *
     * @param publisher   The target publisher
     * @param tracer      The tracer
     * @param spanBuilder The span builder that represents the span that will be created when the publisher is subscribed to
     */
    public TracingPublisher(Publisher<T> publisher, Tracer tracer, SpanBuilder spanBuilder) {
        this(publisher, tracer, spanBuilder, Publishers.isSingle(publisher.getClass()));
    }

    /**
     * Creates a new tracing publisher for the given arguments.
     *
     * @param publisher   The target publisher
     * @param tracer      The tracer
     * @param spanBuilder The span builder that represents the span that will be created when the publisher is subscribed to
     * @param isSingle    Does the publisher emit a single item
     */
    public TracingPublisher(Publisher<T> publisher, Tracer tracer, SpanBuilder spanBuilder, boolean isSingle) {
        this.publisher = publisher;
        this.tracer = tracer;
        this.spanBuilder = spanBuilder;
        this.parentContext = ContextStorage.get().current();
        this.isSingle = isSingle;
        if (parentContext != null && spanBuilder != null) {
            spanBuilder.setParent(parentContext);
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        Span span;
        boolean finishOnClose;
        if (spanBuilder != null) {
            span = spanBuilder.startSpan();
            finishOnClose = true;
        } else {
            // TODO
            span = Span.current();
            finishOnClose = isContinued();
        }
        if (span != null) {
            try (Scope ignored = span.makeCurrent()) {
                //noinspection SubscriberImplementation
                publisher.subscribe(new Subscriber<T>() {
                    boolean finished = false;

                    @Override
                    public void onSubscribe(Subscription s) {
                        // TODO
                        //if (scopeManager.activeSpan() != span) {
                        //    try (Scope ignored = scopeManager.activate(span)) {
                        //        TracingPublisher.this.doOnSubscribe(span);
                        //        actual.onSubscribe(s);
                        //    }
                        //} else {
                        TracingPublisher.this.doOnSubscribe(span);
                        actual.onSubscribe(s);
                        //}
                    }

                    @Override
                    public void onNext(T object) {
                        boolean finishAfterNext = isSingle && finishOnClose;
                        try (Scope ignored = span.makeCurrent()) {
                            if (object instanceof MutableHttpResponse) {
                                MutableHttpResponse response = (MutableHttpResponse) object;
                                Optional<?> body = response.getBody();
                                if (body.isPresent()) {
                                    Object o = body.get();
                                    if (Publishers.isConvertibleToPublisher(o)) {
                                        Class<?> type = o.getClass();
                                        Publisher<?> resultPublisher = Publishers.convertPublisher(o, Publisher.class);
                                        Publisher<?> scopedPublisher = new ScopePropagationPublisher(resultPublisher, tracer, span);
                                        response.body(Publishers.convertPublisher(scopedPublisher, type));
                                    }
                                }

                            }
                            TracingPublisher.this.doOnNext(object, span);
                            actual.onNext(object);
                            if (isSingle) {
                                finished = true;
                                TracingPublisher.this.doOnFinish(span);
                            }
                        } finally {
                            if (finishAfterNext) {
                                span.end();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        try (Scope ignored = span.makeCurrent()) {
                            TracingPublisher.this.onError(t, span);
                            actual.onError(t);
                            finished = true;
                        } finally {
                            if (finishOnClose && isFinishOnError()) {
                                span.end();
                            }
                        }
                    }

                    @Override
                    public void onComplete() {
                        try (Scope ignored = span.makeCurrent()) {
                            actual.onComplete();
                            TracingPublisher.this.doOnFinish(span);
                        } finally {
                            if (!finished && finishOnClose) {
                                span.end();
                            }
                        }
                    }
                });
            }
        } else {
            publisher.subscribe(actual);
        }
    }

    /**
     * Designed for subclasses to override if the current active span is to be continued by this publisher. False by default.
     * This only has effects if no spanBuilder was defined.
     *
     * @return true, if the current span should be continued by this publisher
     * @since 2.0.3
     */
    protected boolean isContinued() {
        return false;
    }

    /**
     * Designed for subclasses to override if the span needs to be finished upon error. True by default.
     *
     * @return true, if the active span needs to be finished on error
     * @since 2.0.3
     */
    protected boolean isFinishOnError() {
        return true;
    }

    /**
     * Designed for subclasses to override and implement custom behaviour when an item is emitted.
     *
     * @param object The object
     * @param span   The span
     */
    protected void doOnNext(@NonNull T object, @NonNull Span span) {
        // no-op
    }

    /**
     * Designed for subclasses to override and implement custom on subscribe behaviour.
     *
     * @param span The span
     */
    protected void doOnSubscribe(@NonNull Span span) {
        // no-op
    }

    /**
     * Designed for subclasses to override and implement custom on finish behaviour. Fired
     * prior to calling {@link Span#end()}.
     *
     * @param span The span
     */
    @SuppressWarnings("WeakerAccess")
    protected void doOnFinish(@NonNull Span span) {
        // no-op
    }

    /**
     * Designed for subclasses to override and implement custom on error behaviour.
     *
     * @param throwable The error
     * @param span      The span
     */
    protected void doOnError(@NonNull Throwable throwable, @NonNull Span span) {
        // no-op
    }

    private void onError(Throwable t, Span span) {
        logError(span, t);
        doOnError(t, span);
    }

}
