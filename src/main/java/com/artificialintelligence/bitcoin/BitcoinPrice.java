package com.artificialintelligence.bitcoin;

public record BitcoinPrice(String source, String currency, double price, String updatedAt) {
}