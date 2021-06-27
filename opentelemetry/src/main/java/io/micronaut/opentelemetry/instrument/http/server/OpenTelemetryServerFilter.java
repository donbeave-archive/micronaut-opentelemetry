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
package io.micronaut.opentelemetry.instrument.http.server;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpAttributes;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Optional;

/**
 * An HTTP server instrumentation filter that uses Open Telemetry.
 *
 * @author Alexey Zhokhov
 * @since 1.0
 */
@Filter("${tracing.http.server.path:/**}")
@Requires(beans = Tracer.class)
public class OpenTelemetryServerFilter implements HttpServerFilter {

    private static final CharSequence APPLIED = OpenTelemetryServerFilter.class.getName() + "-applied";

    private final MicronautHttpServerTracer tracer;

    /**
     * Creates an HTTP server instrumentation filter.
     */
    public OpenTelemetryServerFilter(OpenTelemetry openTelemetry) {
        this.tracer = new MicronautHttpServerTracer(openTelemetry);
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(final HttpRequest<?> request, ServerFilterChain chain) {
        boolean applied = request.getAttribute(APPLIED, Boolean.class).orElse(false);

        if (applied) {
            return chain.proceed(request);
        }

        request.setAttribute(APPLIED, true);

        Publisher<MutableHttpResponse<?>> requestPublisher = chain.proceed(request);

        return (Publishers.MicronautPublisher<MutableHttpResponse<?>>) actual -> {
            Context span = tracer.startSpan(request, request, request, resolveSpanName(request));

            try (Scope ignored = span.makeCurrent()) {
                requestPublisher.subscribe(new Subscriber<MutableHttpResponse<?>>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        try (Scope ignored = span.makeCurrent()) {
                            actual.onSubscribe(s);
                        }
                    }

                    @Override
                    public void onNext(MutableHttpResponse<?> object) {
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

    /**
     * Resolve the span name to use for the request.
     *
     * @param request The request
     * @return The span name
     */
    protected String resolveSpanName(HttpRequest<?> request) {
        Optional<String> route = request.getAttribute(HttpAttributes.URI_TEMPLATE, String.class);
        return route.map(s -> request.getMethodName() + " " + s).orElse(request.getMethodName() + " " + request.getPath());
    }

}
