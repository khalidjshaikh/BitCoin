package com.artificialintelligence.bitcoin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class BitcoinPriceController {

    private final BitcoinPriceService bitcoinPriceService;

    public BitcoinPriceController(BitcoinPriceService bitcoinPriceService) {
        this.bitcoinPriceService = bitcoinPriceService;
    }

    @GetMapping
    public BitcoinPrice getBitcoinPrice() {
        return bitcoinPriceService.fetchPrice();
    }
}