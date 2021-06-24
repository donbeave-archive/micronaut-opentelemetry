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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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
    private final MicronautTracer tracer;
    private final String spanName;
    private final SpanKind kind;
    private final Context parentContext;

    /**
     * Creates a new tracing publisher for the given arguments.
     */
    public TracingPublisher(Publisher<T> publisher, MicronautTracer tracer, String spanName, SpanKind kind,
                            Context parentContext) {
        this.publisher = publisher;
        this.tracer = tracer;
        this.spanName = spanName;
        this.kind = kind;
        this.parentContext = parentContext;
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        Context span = tracer.startSpan(parentContext, spanName, kind);

        try (Scope ignored = span.makeCurrent()) {
            publisher.subscribe(new Subscriber<T>() {
                @Override
                public void onSubscribe(Subscription s) {
                    try (Scope ignored = span.makeCurrent()) {
                        TracingPublisher.this.doOnSubscribe(span);
                        actual.onSubscribe(s);
                    }
                }

                @Override
                public void onNext(T object) {
                    try (Scope ignored = span.makeCurrent()) {
                        TracingPublisher.this.doOnNext(object, span);
                        actual.onNext(object);
                        TracingPublisher.this.doOnFinish(span);
                    } finally {
                        tracer.end(span);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    try (Scope ignored = span.makeCurrent()) {
                        TracingPublisher.this.onError(t, span);
                        actual.onError(t);
                    } finally {
                        tracer.end(span);
                    }
                }

                @Override
                public void onComplete() {
                    try (Scope ignored = span.makeCurrent()) {
                        actual.onComplete();
                        TracingPublisher.this.doOnFinish(span);
                    } finally {
                        tracer.end(span);
                    }
                }
            });
        }
    }

    /**
     * Designed for subclasses to override and implement custom behaviour when an item is emitted.
     *
     * @param object The object
     * @param span   The span
     */
    protected void doOnNext(@NonNull T object, @NonNull Context span) {
        // no-op
    }

    /**
     * Designed for subclasses to override and implement custom on subscribe behaviour.
     *
     * @param span The span
     */
    protected void doOnSubscribe(@NonNull Context span) {
        // no-op
    }

    /**
     * Designed for subclasses to override and implement custom on finish behaviour. Fired
     * prior to calling {@link Span#end()}.
     *
     * @param span The span
     */
    @SuppressWarnings("WeakerAccess")
    protected void doOnFinish(@NonNull Context span) {
        // no-op
    }

    /**
     * Designed for subclasses to override and implement custom on error behaviour.
     *
     * @param throwable The error
     * @param span      The span
     */
    protected void doOnError(@NonNull Throwable throwable, @NonNull Context span) {
        // no-op
    }

    private void onError(Throwable t, Context span) {
        tracer.onException(span, t);
        doOnError(t, span);
    }

}
