package com.villamil.barberbooking.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.catalina.valves.ValveBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/** Exercises the actual Tomcat valve created from the committed production configuration. */
class ProductionProxyConfigurationTest {

    @Test
    void productionUsesNativeValveWithRestrictedInternalProxies() throws IOException {
        RemoteIpValve valve = productionValve();

        assertThat(valve.getInternalProxies()).isNotBlank();
        assertThat(Pattern.matches(valve.getInternalProxies(), "10.23.0.5")).isTrue();
        assertThat(Pattern.matches(valve.getInternalProxies(), "198.51.100.12")).isFalse();
        assertThat(valve.getTrustedProxies()).isNullOrEmpty();
    }

    @Test
    void untrustedPeerCannotSpoofForwardedAddressOrProtocol() throws IOException, ServletException {
        ClientView observed = invokeValve("198.51.100.12", "203.0.113.99");

        assertThat(observed.address()).isEqualTo("198.51.100.12");
        assertThat(observed.secure()).isFalse();
        assertThat(observed.scheme()).isEqualTo("http");
    }

    @Test
    void internalProxyUsesRightmostUntrustedAddressInsteadOfSpoofedPrefix() throws IOException, ServletException {
        ClientView observed = invokeValve("10.23.0.5", "198.51.100.99, 203.0.113.10, 10.23.0.4");

        assertThat(observed.address()).isEqualTo("203.0.113.10");
        assertThat(observed.secure()).isTrue();
        assertThat(observed.scheme()).isEqualTo("https");
    }

    @Test
    void differentClientsBehindTheSameInternalProxyRemainDistinct() throws IOException, ServletException {
        ClientView first = invokeValve("10.23.0.5", "198.51.100.99, 203.0.113.10, 10.23.0.4");
        ClientView second = invokeValve("10.23.0.5", "198.51.100.99, 203.0.113.11, 10.23.0.4");

        assertThat(first.address()).isEqualTo("203.0.113.10");
        assertThat(second.address()).isEqualTo("203.0.113.11");
        assertThat(first.address()).isNotEqualTo(second.address());
    }

    private RemoteIpValve productionValve() throws IOException {
        var environment = new StandardEnvironment();
        var sources = new YamlPropertySourceLoader().load("production", new ClassPathResource("application-prod.yml"));
        sources.forEach(source -> environment.getPropertySources().addFirst(source));
        ServerProperties properties = Binder.get(environment).bind("server", ServerProperties.class).get();
        assertThat(properties.getForwardHeadersStrategy()).isEqualTo(ServerProperties.ForwardHeadersStrategy.NATIVE);
        var factory = new TomcatServletWebServerFactory();
        new TomcatWebServerFactoryCustomizer(environment, properties).customize(factory);
        return factory.getEngineValves().stream().filter(RemoteIpValve.class::isInstance)
                .map(RemoteIpValve.class::cast).findFirst().orElseThrow();
    }

    private ClientView invokeValve(String peer, String forwardedFor) throws IOException, ServletException {
        RemoteIpValve valve = productionValve();
        var observed = new AtomicReference<ClientView>();
        valve.setNext(new ValveBase(true) {
            @Override public void invoke(Request request, Response response) {
                observed.set(new ClientView(request.getRemoteAddr(), request.isSecure(), request.getScheme()));
            }
        });
        var raw = new org.apache.coyote.Request();
        raw.scheme().setString("http");
        raw.serverName().setString("service.example.test");
        raw.getMimeHeaders().addValue("X-Forwarded-For").setString(forwardedFor);
        raw.getMimeHeaders().addValue("X-Forwarded-Proto").setString("https");
        var request = new Request(new Connector());
        request.setCoyoteRequest(raw);
        request.setRemoteAddr(peer);
        request.setRemoteHost(peer);
        request.setServerPort(8080);
        valve.invoke(request, new Response());
        return observed.get();
    }

    private record ClientView(String address, boolean secure, String scheme) { }
}
