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
package io.micronaut.opentelemetry.instrument.grpc.server;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.grpc.server.GrpcServerConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTracing;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTracingBuilder;

import javax.annotation.Nonnull;

/**
 * Adds a TracingServerInterceptor when OpenTelemetry for GRPC is on the classpath.
 *
 * @author Alexey Zhokhov
 * @since 1.0
 */
@ConfigurationProperties(GrpcServerTracingInterceptorConfiguration.PREFIX)
public class GrpcServerTracingInterceptorConfiguration {

    public static final String PREFIX = GrpcServerConfiguration.PREFIX + ".tracing";

    @ConfigurationBuilder(prefixes = "with", allowZeroArgs = true)
    protected final GrpcTracingBuilder builder;

    /**
     * Default constructor.
     *
     * @param tracer The tracer
     */
    protected GrpcServerTracingInterceptorConfiguration(OpenTelemetry openTelemetry) {
        this.builder = GrpcTracing.newBuilder(openTelemetry);
    }

    /**
     * @return The {@link GrpcTracingBuilder}
     */
    @Nonnull
    public GrpcTracingBuilder getBuilder() {
        return builder;
    }

    /**
     * Decorates the server span with custom data.
     *
     * @param serverSpanDecorator used to decorate the server span
     */
    // TODO
    /*
    @Inject
    public void setServerSpanDecorator(@Nullable ServerSpanDecorator serverSpanDecorator) {
        if (serverSpanDecorator != null) {
            builder.withServerSpanDecorator(serverSpanDecorator);
        }
    }
     */

    /**
     * Decorates the server span with custom data when the gRPC call is closed.
     *
     * @param serverCloseDecorator used to decorate the server span
     */
    // TODO
    /*
    @Inject
    public void setServerCloseDecorator(@Nullable ServerCloseDecorator serverCloseDecorator) {
        if (serverCloseDecorator != null) {
            builder.withServerCloseDecorator(serverCloseDecorator);
        }
    }
     */

}
