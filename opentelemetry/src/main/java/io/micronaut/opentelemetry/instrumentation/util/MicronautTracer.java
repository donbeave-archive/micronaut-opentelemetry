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
package io.micronaut.opentelemetry.instrumentation.util;

import io.micronaut.context.annotation.Requires;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.tracer.BaseTracer;
import jakarta.inject.Singleton;


/**
 * @author Alexey Zhokhov
 */
@Singleton
@Requires(beans = OpenTelemetry.class)
public class MicronautTracer extends BaseTracer {

    public MicronautTracer(OpenTelemetry openTelemetry) {
        super(openTelemetry);
    }

    @Override
    protected String getInstrumentationName() {
        return "io.micronaut.opentelemetry.micronaut-opentelemetry-1.0";
    }

}
