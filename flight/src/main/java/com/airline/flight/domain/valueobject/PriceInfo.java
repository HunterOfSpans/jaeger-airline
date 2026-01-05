package com.airline.flight.domain.valueobject;

import lombok.Value;

import java.math.BigDecimal;

/**
 * Price Information Value Object
 */
@Value
public class PriceInfo {
    BigDecimal amount;

    public static PriceInfo of(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Price amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price amount must be greater than zero");
        }

        return new PriceInfo(amount);
    }
}