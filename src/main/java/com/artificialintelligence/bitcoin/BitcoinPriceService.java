package com.artificialintelligence.bitcoin;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class BitcoinPriceService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${bitcoin.price.url:https://api.kraken.com/0/public/Ticker?pair=XBTUSD}")
    private String priceUrl;

    private volatile BitcoinPrice lastKnownPrice;

    @CircuitBreaker(name = "bitcoinPrice", fallbackMethod = "fetchPriceFallback")
    @Retry(name = "bitcoinPrice", fallbackMethod = "fetchPriceFallback")
    public BitcoinPrice fetchPrice() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(priceUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("External service returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode errors = root.path("error");
            if (errors.isArray() && !errors.isEmpty()) {
                throw new IllegalStateException("Kraken API error: " + errors);
            }
            JsonNode result = root.path("result");
            if (!result.isObject() || result.isEmpty()) {
                throw new IllegalStateException("Kraken API returned no result");
            }
            // Kraken returns a single ticker object keyed by the pair name (e.g. XXBTZUSD).
            JsonNode ticker = result.properties().iterator().next().getValue();
            JsonNode lastTrade = ticker.path("c");
            double price = lastTrade.isArray() && !lastTrade.isEmpty()
                    ? lastTrade.get(0).asDouble()
                    : ticker.path("a").isArray() ? ticker.path("a").get(0).asDouble() : 0.0;
            String updated = java.time.Instant.now().toString();
            BitcoinPrice priceResult = new BitcoinPrice("Kraken", "USD", price, updated);
            lastKnownPrice = priceResult;
            return priceResult;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Bitcoin price", e);
        }
    }

    public BitcoinPrice fetchPriceFallback(Throwable throwable) {
        if (lastKnownPrice != null) {
            return new BitcoinPrice(lastKnownPrice.source(), lastKnownPrice.currency(),
                    lastKnownPrice.price(), lastKnownPrice.updatedAt() + " (cached)");
        }
        return new BitcoinPrice("fallback", "USD", 0.0, java.time.Instant.now().toString());
    }
}