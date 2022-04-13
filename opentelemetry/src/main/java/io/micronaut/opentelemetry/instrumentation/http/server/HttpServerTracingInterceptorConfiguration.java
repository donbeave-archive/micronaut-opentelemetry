/*
 * Copyright 2017-2019 original authors
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
package io.micronaut.opentelemetry.instrumentation.http.server;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.opentelemetry.instrumentation.http.MicronautHttpServerTelemetryBuilder;
import io.opentelemetry.api.OpenTelemetry;

import javax.annotation.Nonnull;

/**
 * Adds a TracingServerInterceptor when OpenTelemetry for GRPC is on the classpath.
 *
 * @author Alexey Zhokhov
 * @since 1.0
 */
@ConfigurationProperties(HttpServerTracingInterceptorConfiguration.PREFIX)
public class HttpServerTracingInterceptorConfiguration {

    public static final String PREFIX = "http.server.tracing";

    @ConfigurationBuilder(allowZeroArgs = true)
    protected final MicronautHttpServerTelemetryBuilder builder;

    /**
     * Default constructor.
     *
     * @param openTelemetry OpenTelemetry
     */
    protected HttpServerTracingInterceptorConfiguration(OpenTelemetry openTelemetry) {
        this.builder = new MicronautHttpServerTelemetryBuilder(openTelemetry);
    }

    /**
     * @return The {@link MicronautHttpServerTelemetryBuilder}
     */
    @Nonnull
    public MicronautHttpServerTelemetryBuilder getBuilder() {
        return builder;
    }

}
