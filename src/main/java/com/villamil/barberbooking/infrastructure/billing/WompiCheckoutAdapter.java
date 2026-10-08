package com.villamil.barberbooking.infrastructure.billing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import com.villamil.barberbooking.application.exception.BillingUnavailableException;
import com.villamil.barberbooking.application.port.out.PaymentCheckoutPort;
import com.villamil.barberbooking.domain.model.BillingOrder;

@Component
public class WompiCheckoutAdapter implements PaymentCheckoutPort {
    private final WompiConfiguration config;
    public WompiCheckoutAdapter(WompiConfiguration config) { this.config = config; }
    @Override public boolean enabled() { return config.enabled; }
    @Override public String environment() { return config.environment; }
    @Override public String checkoutUrl(BillingOrder order) {
        if (!config.enabled || !order.environment().equals(config.environment)) throw new BillingUnavailableException();
        String signature = sha256(order.reference() + order.amountInCents() + order.currency() + config.integritySecret);
        return UriComponentsBuilder.fromUriString("https://checkout.wompi.co/p/")
                .queryParam("public-key", config.publicKey).queryParam("currency", order.currency())
                .queryParam("amount-in-cents", order.amountInCents()).queryParam("reference", order.reference())
                .queryParam("signature:integrity", signature).queryParam("redirect-url", config.redirectUrl)
                .build().encode().toUriString();
    }
    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
