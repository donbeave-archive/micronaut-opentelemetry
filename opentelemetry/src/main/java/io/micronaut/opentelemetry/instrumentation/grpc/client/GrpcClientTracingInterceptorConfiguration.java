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
package io.micronaut.opentelemetry.instrumentation.grpc.client;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.grpc.channels.GrpcDefaultManagedChannelConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTracing;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTracingBuilder;

/**
 * Adds a TracingClientInterceptor when OpenTelemetry for GRPC is on the classpath.
 *
 * @author Alexey Zhokhov
 * @since 1.0
 */
@ConfigurationProperties(GrpcClientTracingInterceptorConfiguration.PREFIX)
public class GrpcClientTracingInterceptorConfiguration {

    public static final String PREFIX = GrpcDefaultManagedChannelConfiguration.PREFIX + ".tracing";

    @ConfigurationBuilder(allowZeroArgs = true)
    protected final GrpcTracingBuilder builder;

    /**
     * Default constructor.
     *
     * @param openTelemetry OpenTelemetry
     */
    protected GrpcClientTracingInterceptorConfiguration(OpenTelemetry openTelemetry) {
        this.builder = GrpcTracing.builder(openTelemetry);
    }

    /**
     * @return The {@link GrpcTracingBuilder}
     */
    @NonNull
    public GrpcTracingBuilder getBuilder() {
        return builder;
    }

}
