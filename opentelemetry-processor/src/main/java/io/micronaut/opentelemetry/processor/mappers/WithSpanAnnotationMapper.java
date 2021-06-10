package io.micronaut.opentelemetry.processor.mappers;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexey Zhokhov
 */
public class WithSpanAnnotationMapper implements NamedAnnotationMapper {

    @NonNull
    @Override
    public String getName() {
        return "io.opentelemetry.extension.annotations.WithSpan";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        AnnotationValueBuilder<Annotation> builder =
                AnnotationValue.builder("io.micronaut.transaction.annotation.TransactionalAdvice");
        return Collections.singletonList(
                builder.build()
        );
    }

}
