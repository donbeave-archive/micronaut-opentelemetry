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
package io.micronaut.opentelemetry.instrumentation.http.server;

import io.micronaut.http.HttpRequest;
import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;

/**
 * @author Alexey Zhokhov
 */
public class HttpRequestExtractAdaptor implements TextMapGetter<HttpRequest> {

    public static final HttpRequestExtractAdaptor GETTER = new HttpRequestExtractAdaptor();

    @Override
    public Iterable<String> keys(HttpRequest carrier) {
        return carrier.getHeaders().names();
    }

    @Nullable
    @Override
    public String get(@Nullable HttpRequest carrier, String key) {
        if (carrier == null) {
            return null;
        }
        return carrier.getHeaders().get(key);
    }

}
