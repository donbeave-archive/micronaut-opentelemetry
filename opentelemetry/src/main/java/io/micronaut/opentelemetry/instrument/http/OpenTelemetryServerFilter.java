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
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.opentelemetry.instrument.util.MicronautTracer;
import io.micronaut.opentelemetry.instrument.util.TracingPublisher;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.reactivestreams.Publisher;

/**
 * An HTTP server instrumentation filter that uses Open Telemetry.
 *
 * @author Alexey Zhokhov
 * @since 1.0
 */
@Filter(AbstractOpenTelemetryFilter.SERVER_PATH)
@Requires(beans = Tracer.class)
public class OpenTelemetryServerFilter extends AbstractOpenTelemetryFilter implements HttpServerFilter {

    private static final CharSequence APPLIED = OpenTelemetryServerFilter.class.getName() + "-applied";

    /**
     * Creates an HTTP server instrumentation filter.
     *
     * @param tracer        For span creation and propagation across transport
     * @param openTelemetry
     */
    public OpenTelemetryServerFilter(MicronautTracer tracer) {
        super(tracer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        boolean applied = request.getAttribute(APPLIED, Boolean.class).orElse(false);
        if (applied) {
            return chain.proceed(request);
        }

        return new TracingPublisher(
                chain.proceed(request),
                tracer,
                resolveSpanName(request),
                SpanKind.SERVER,
                initSpanContext(request)
        ) {

            @Override
            protected void doOnSubscribe(@NonNull Context span) {
                // TODO
                //span.setAttribute(TAG_HTTP_SERVER, true);
                request.setAttribute(TraceRequestAttributes.CURRENT_SPAN, span);
            }

            @Override
            protected void doOnNext(@NonNull Object object, @NonNull Context span) {
                if (object instanceof HttpResponse) {
                    HttpResponse<?> response = (HttpResponse<?>) object;

                    // TODO
                    /*
                    tracer.inject(
                            span.context(),
                            Format.Builtin.HTTP_HEADERS,
                            new HttpHeadersTextMap(response.getHeaders())
                    );
                     */

                    setResponseTags(request, response, span);
                }
            }

            @Override
            protected void doOnError(@NonNull Throwable throwable, @NonNull Context span) {
                tracer.onException(span, throwable);
            }
        };
    }

    @NonNull
    private Context initSpanContext(@NonNull HttpRequest<?> request) {
        request.setAttribute(APPLIED, true);

        Context context = tracer.extract(request.getHeaders(), new HttpHeadersTextMapGetter());

        request.setAttribute(
                TraceRequestAttributes.CURRENT_SPAN_CONTEXT,
                context
        );

        return context;
    }

}
