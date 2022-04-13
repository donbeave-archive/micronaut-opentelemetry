package io.micronaut.opentelemetry.instrumentation.http;

import io.micronaut.http.HttpRequest;
import io.opentelemetry.instrumentation.api.instrumenter.net.InetSocketAddressNetServerAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import java.net.InetSocketAddress;

final class MicronautHttpNetServerAttributesGetter extends InetSocketAddressNetServerAttributesGetter<HttpRequest> {

    @Override
    public InetSocketAddress getAddress(HttpRequest request) {
        return request.getRemoteAddress();
    }

    @Override
    public String transport(HttpRequest request) {
        return SemanticAttributes.NetTransportValues.IP_TCP;
    }

}
