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
package io.micronaut.opentelemetry.instrument.http;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import io.micronaut.opentelemetry.instrument.util.MicronautTracer;
import io.micronaut.opentelemetry.instrument.util.TracingPublisher;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.reactivestreams.Publisher;

/**
 * An HTTP client instrumentation filter that uses Open Telemetry.
 *
 * @author graemerocher
 * @since 1.0
 */
@Filter(AbstractOpenTelemetryFilter.CLIENT_PATH)
@Requires(beans = Tracer.class)
public class OpenTelemetryClientFilter extends AbstractOpenTelemetryFilter implements HttpClientFilter {

    /**
     * Initialize the open tracing client filter with tracer.
     *
     * @param tracer         The tracer for span creation and configuring across arbitrary transports
     * @param openTelemetry1
     */
    public OpenTelemetryClientFilter(MicronautTracer tracer) {
        super(tracer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        Publisher<? extends HttpResponse<?>> requestPublisher = chain.proceed(request);

        return new TracingPublisher(
                requestPublisher,
                tracer,
                resolveSpanName(request),
                SpanKind.CLIENT,
                Context.current()
        ) {
            @Override
            protected void doOnSubscribe(@NonNull Context span) {
                // TODO
                //span.setAttribute(TAG_HTTP_CLIENT, true);
                //SpanContext spanContext = span.getSpanContext();

                // TODO
                //openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), transportLayer, setter);

                // TODO
                /*
                tracer.inject(
                        spanContext,
                        Format.Builtin.HTTP_HEADERS,
                        new HttpHeadersTextMap(request.getHeaders())
                );
                 */

                /*
                request.setAttribute(
                        TraceRequestAttributes.CURRENT_SPAN_CONTEXT,
                        spanContext
                );
                 */
                request.setAttribute(TraceRequestAttributes.CURRENT_SPAN, span);
            }

            @Override
            protected void doOnNext(@NonNull Object object, @NonNull Context span) {
                if (object instanceof HttpResponse) {
                    setResponseTags(request, (HttpResponse<?>) object, span);
                }
            }

            @Override
            protected void doOnError(@NonNull Throwable error, @NonNull Context span) {
                if (error instanceof HttpClientResponseException) {
                    HttpClientResponseException e = (HttpClientResponseException) error;
                    HttpResponse<?> response = e.getResponse();
                    setResponseTags(request, response, span);
                }
                tracer.onException(span, error);
            }
        };
    }

}
