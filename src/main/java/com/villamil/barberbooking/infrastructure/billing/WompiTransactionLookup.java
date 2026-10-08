package com.villamil.barberbooking.infrastructure.billing;

import com.fasterxml.jackson.databind.JsonNode;

public interface WompiTransactionLookup {
    JsonNode find(String transactionId);
}
