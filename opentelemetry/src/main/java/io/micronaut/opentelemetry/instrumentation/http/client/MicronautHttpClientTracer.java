/*
 * Copyright 2017-2021 original authors
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

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.tracer.HttpClientTracer;
import io.opentelemetry.instrumentation.api.tracer.net.NetPeerAttributes;

import java.net.URI;

class MicronautHttpClientTracer extends
        HttpClientTracer<MutableHttpRequest<?>, MutableHttpRequest<?>, HttpResponse<?>> {

    MicronautHttpClientTracer(OpenTelemetry openTelemetry) {
        super(openTelemetry, NetPeerAttributes.INSTANCE);
    }

    @Override
    protected String method(MutableHttpRequest httpRequest) {
        return httpRequest.getMethodName();
    }

    @Override
    protected URI url(MutableHttpRequest httpRequest) {
        return httpRequest.getUri();
    }

    @Override
    protected Integer status(HttpResponse httpResponse) {
        return httpResponse.code();
    }

    @Override
    protected String requestHeader(MutableHttpRequest request, String name) {
        return request.getHeaders().get(name);
    }

    @Override
    protected String responseHeader(HttpResponse response, String name) {
        return response.header(name);
    }

    @Override
    protected TextMapSetter<MutableHttpRequest<?>> getSetter() {
        return MutableHttpRequestInjectAdapter.SETTER;
    }

    @Override
    protected String getInstrumentationName() {
        return "io.micronaut.opentelemetry.micronaut-opentelemetry-1.0";
    }
}
