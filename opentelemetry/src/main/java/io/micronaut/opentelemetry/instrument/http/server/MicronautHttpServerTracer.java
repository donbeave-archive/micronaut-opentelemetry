package io.micronaut.opentelemetry.instrument.http.server;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.tracer.HttpServerTracer;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;

public class MicronautHttpServerTracer extends HttpServerTracer<HttpRequest, HttpResponse, HttpRequest, HttpRequest> {

    MicronautHttpServerTracer(OpenTelemetry openTelemetry) {
        super(openTelemetry);
    }

    @Override
    protected Integer peerPort(HttpRequest httpRequest) {
        InetSocketAddress socketAddress = httpRequest.getRemoteAddress();
        return socketAddress.getPort();
    }

    @Override
    protected String peerHostIp(HttpRequest httpRequest) {
        InetSocketAddress socketAddress = httpRequest.getRemoteAddress();
        return socketAddress.getAddress().getHostAddress();
    }

    @Override
    protected String flavor(HttpRequest httpRequest, HttpRequest httpRequest2) {
        switch (httpRequest.getHttpVersion()) {
            case HTTP_1_0:
                return "HTTP/1.0";
            case HTTP_1_1:
                return "HTTP/1.1";
            case HTTP_2_0:
                return "HTTP/2.0";
        }
        return null;
    }

    @Override
    protected TextMapGetter<HttpRequest> getGetter() {
        return HttpRequestExtractAdaptor.GETTER;
    }

    @Override
    protected String url(HttpRequest httpRequest) {
        String uri = httpRequest.getUri().toString();
        if (isRelativeUrl(uri) && httpRequest.getHeaders().get(HOST) != null) {
            return "http://" + httpRequest.getHeaders().get(HOST) + httpRequest.getUri();
        } else {
            return uri;
        }
    }

    @Override
    protected String method(HttpRequest httpRequest) {
        return httpRequest.getMethodName();
    }

    @Override
    protected String requestHeader(HttpRequest httpRequest, String name) {
        return httpRequest.getHeaders().get(name);
    }

    @Override
    protected int responseStatus(HttpResponse httpResponse) {
        return httpResponse.code();
    }

    @Override
    public Context getServerContext(HttpRequest httpRequest) {
        return (Context) httpRequest.getAttribute(CONTEXT_ATTRIBUTE).orElse(null);
    }

    @Override
    protected void attachServerContext(Context context, HttpRequest httpRequest) {
        httpRequest.setAttribute(CONTEXT_ATTRIBUTE, context);
    }

    @Override
    protected String getInstrumentationName() {
        return "io.micronaut.opentelemetry.micronaut-opentelemetry-1.0";
    }
}
