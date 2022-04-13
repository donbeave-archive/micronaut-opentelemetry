package io.micronaut.opentelemetry.instrumentation.http;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.opentelemetry.instrumentation.api.instrumenter.net.InetSocketAddressNetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

final class MicronautHttpNetClientAttributesGetter
        extends InetSocketAddressNetClientAttributesGetter<MutableHttpRequest, HttpResponse> {

    @Override
    public InetSocketAddress getAddress(MutableHttpRequest request, @Nullable HttpResponse response) {
        return request.getRemoteAddress();
    }

    @Override
    public String transport(MutableHttpRequest request, @Nullable HttpResponse response) {
        return SemanticAttributes.NetTransportValues.IP_TCP;
    }

}
