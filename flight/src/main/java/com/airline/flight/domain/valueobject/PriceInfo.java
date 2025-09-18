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

    public boolean isMoreExpensiveThan(PriceInfo other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public PriceInfo applyDiscount(BigDecimal discountPercentage) {
        if (discountPercentage.compareTo(BigDecimal.ZERO) < 0 ||
            discountPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }

        BigDecimal discount = amount.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
        return PriceInfo.of(amount.subtract(discount));
    }
}