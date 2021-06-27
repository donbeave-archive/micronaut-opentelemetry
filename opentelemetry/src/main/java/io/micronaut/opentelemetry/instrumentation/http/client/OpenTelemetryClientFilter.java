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
package io.micronaut.opentelemetry.instrumentation.http.client;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * An HTTP client instrumentation filter that uses Open Telemetry.
 *
 * @author Alexey Zhokhov
 * @since 1.0
 */
@Filter("${tracing.http.client.path:/**}")
@Requires(beans = OpenTelemetry.class)
public class OpenTelemetryClientFilter implements HttpClientFilter {

    private final MicronautHttpClientTracer tracer;

    public OpenTelemetryClientFilter(OpenTelemetry openTelemetry) {
        tracer = new MicronautHttpClientTracer(openTelemetry);
    }

    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        Publisher<? extends HttpResponse<?>> requestPublisher = chain.proceed(request);

        Context parentContext = Context.current();
        if (!tracer.shouldStartSpan(parentContext)) {
            return requestPublisher;
        }

        return (Publishers.MicronautPublisher<HttpResponse<?>>) actual -> {
            Context span = tracer.startSpan(parentContext, request, request);

            try (Scope ignored = span.makeCurrent()) {
                requestPublisher.subscribe(new Subscriber<HttpResponse<?>>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        try (Scope ignored = span.makeCurrent()) {
                            actual.onSubscribe(s);
                        }
                    }

                    @Override
                    public void onNext(HttpResponse<?> object) {
                        try (Scope ignored = span.makeCurrent()) {
                            actual.onNext(object);
                        } finally {
                            tracer.end(span);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        try (Scope ignored = span.makeCurrent()) {
                            tracer.onException(span, t);
                            actual.onError(t);
                        } finally {
                            tracer.end(span);
                        }
                    }

                    @Override
                    public void onComplete() {
                        try (Scope ignored = span.makeCurrent()) {
                            actual.onComplete();
                        } finally {
                            tracer.end(span);
                        }
                    }
                });
            }
        };
    }

}
