package io.micronaut.opentelemetry.instrument.http.client;

import io.micronaut.http.MutableHttpRequest;
import io.opentelemetry.context.propagation.TextMapSetter;

final class MutableHttpRequestInjectAdapter implements TextMapSetter<MutableHttpRequest<?>> {

  static final MutableHttpRequestInjectAdapter SETTER = new MutableHttpRequestInjectAdapter();

  @Override
  public void set(MutableHttpRequest<?> carrier, String key, String value) {
    if (carrier == null) {
      return;
    }
    carrier.header(key, value);
  }
}
