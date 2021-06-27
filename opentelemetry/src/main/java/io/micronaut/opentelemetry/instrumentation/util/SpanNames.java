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

import io.micronaut.core.annotation.Nullable;

import java.lang.reflect.Method;

public final class SpanNames {

    private SpanNames() {
    }

    /**
     * This method is used to generate an acceptable span (operation) name based on a given method
     * reference. Anonymous classes are named based on their parent.
     */
    public static String spanNameForMethod(Method method) {
        return spanNameForMethod(method.getDeclaringClass(), method.getName());
    }

    /**
     * This method is used to generate an acceptable span (operation) name based on a given method
     * reference. Anonymous classes are named based on their parent.
     */
    public static String spanNameForMethod(Class<?> clazz, @Nullable Method method) {
        return spanNameForMethod(clazz, method == null ? "<unknown>" : method.getName());
    }

    /**
     * This method is used to generate an acceptable span (operation) name based on a given method
     * reference. Anonymous classes are named based on their parent.
     */
    public static String spanNameForMethod(Class<?> cl, String methodName) {
        return spanNameForClass(cl) + "." + methodName;
    }

    /**
     * This method is used to generate an acceptable span (operation) name based on a given class
     * reference. Anonymous classes are named based on their parent.
     */
    public static String spanNameForClass(Class<?> clazz) {
        if (!clazz.isAnonymousClass()) {
            return clazz.getSimpleName();
        }
        String className = clazz.getName();
        if (clazz.getPackage() != null) {
            String pkgName = clazz.getPackage().getName();
            if (!pkgName.isEmpty()) {
                className = className.substring(pkgName.length() + 1);
            }
        }
        return className;
    }

}
