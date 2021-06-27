package io.micronaut.opentelemetry.instrument.http.client;

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
