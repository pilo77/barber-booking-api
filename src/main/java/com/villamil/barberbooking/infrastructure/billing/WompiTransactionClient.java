package com.villamil.barberbooking.infrastructure.billing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villamil.barberbooking.application.exception.BillingUnavailableException;

@Component
public class WompiTransactionClient implements WompiTransactionLookup {
    private final WompiConfiguration config;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    public WompiTransactionClient(WompiConfiguration config, ObjectMapper mapper) {
        this.config = config; this.mapper = mapper;
    }
    @Override public JsonNode find(String id) {
        if (!config.enabled || id == null || !id.matches("[A-Za-z0-9-]{1,100}")) throw new BillingUnavailableException();
        String host = "prod".equals(config.environment) ? "https://production.wompi.co" : "https://sandbox.wompi.co";
        try {
            var request = HttpRequest.newBuilder(URI.create(host + "/v1/transactions/" + id))
                    .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + config.privateKey).GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 65536) throw new BillingUnavailableException();
            JsonNode transaction = mapper.readTree(response.body()).path("data");
            if (!transaction.isObject()) throw new BillingUnavailableException();
            return transaction;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BillingUnavailableException();
        } catch (Exception e) { throw new BillingUnavailableException(); }
    }
}
