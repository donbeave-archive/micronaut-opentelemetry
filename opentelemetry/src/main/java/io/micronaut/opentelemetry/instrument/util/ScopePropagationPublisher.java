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

import io.micronaut.core.async.publisher.Publishers;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A {@link Publisher} that just propagates tracing state without creating a new span.
 *
 * @param <T> The publisher generic type
 * @author Alexey Zhokhov
 * @since 1.0
 */
@SuppressWarnings("PublisherImplementation")
public class ScopePropagationPublisher<T> implements Publishers.MicronautPublisher<T> {

    private final Publisher<T> publisher;
    private final Context span;

    public ScopePropagationPublisher(Publisher<T> publisher, Context span) {
        this.publisher = publisher;
        this.span = span;
    }

    @SuppressWarnings("SubscriberImplementation")
    @Override
    public void subscribe(Subscriber<? super T> actual) {
        try (Scope ignored = span.makeCurrent()) {
            publisher.subscribe(new Subscriber<T>() {
                @Override
                public void onSubscribe(Subscription s) {
                    try (Scope ignored = span.makeCurrent()) {
                        actual.onSubscribe(s);
                    }
                }

                @Override
                public void onNext(T object) {
                    try (Scope ignored = span.makeCurrent()) {
                        actual.onNext(object);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    try (Scope ignored = span.makeCurrent()) {
                        actual.onError(t);
                    }
                }

                @Override
                public void onComplete() {
                    try (Scope ignored = span.makeCurrent()) {
                        actual.onComplete();
                    }
                }
            });
        }
    }

}
