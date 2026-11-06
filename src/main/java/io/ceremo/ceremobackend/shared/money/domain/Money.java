package io.ceremo.ceremobackend.shared.money.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.ceremo.ceremobackend.shared.error.domain.Assert;

public record Money(BigDecimal amount, Currency currency) {

  public Money {
    Assert.notNull("amount", amount);
    Assert.notNull("currency", currency);
    if (amount.scale() > currency.decimalPlaces()) {
      throw new IllegalArgumentException(
          "Amount scale (%d) exceeds currency decimal places (%d) for %s"
              .formatted(amount.scale(), currency.decimalPlaces(), currency.name()));
    }
  }

  public static Money of(BigDecimal amount, Currency currency) {
    return new Money(amount.setScale(currency.decimalPlaces(), RoundingMode.HALF_UP), currency);
  }

  public static Money of(String amount, Currency currency) {
    Assert.notBlank("amount", amount);
    return of(new BigDecimal(amount), currency);
  }

  public static Money of(long amount, Currency currency) {
    return of(BigDecimal.valueOf(amount), currency);
  }

  public static Money zero(Currency currency) {
    Assert.notNull("currency", currency);
    return new Money(
        BigDecimal.ZERO.setScale(currency.decimalPlaces(), RoundingMode.HALF_UP), currency);
  }

  public Money add(Money other) {
    assertSameCurrency(other);
    return new Money(amount.add(other.amount), currency);
  }

  public Money subtract(Money other) {
    assertSameCurrency(other);
    return new Money(amount.subtract(other.amount), currency);
  }

  public Money multiply(BigDecimal multiplier) {
    Assert.notNull("multiplier", multiplier);
    return Money.of(amount.multiply(multiplier), currency);
  }

  public Money multiply(long multiplier) {
    return multiply(BigDecimal.valueOf(multiplier));
  }

  public Money negate() {
    return new Money(amount.negate(), currency);
  }

  public boolean isPositive() {
    return amount.compareTo(BigDecimal.ZERO) > 0;
  }

  public boolean isNegative() {
    return amount.compareTo(BigDecimal.ZERO) < 0;
  }

  public boolean isZero() {
    return amount.compareTo(BigDecimal.ZERO) == 0;
  }

  public boolean isGreaterThan(Money other) {
    assertSameCurrency(other);
    return amount.compareTo(other.amount) > 0;
  }

  public boolean isLessThan(Money other) {
    assertSameCurrency(other);
    return amount.compareTo(other.amount) < 0;
  }

  private void assertSameCurrency(Money other) {
    Assert.notNull("other", other);
    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Cannot operate on different currencies: %s and %s".formatted(currency, other.currency));
    }
  }

  public BigDecimal get() {
    return amount();
  }

  @Override
  public String toString() {
    return currency.symbol() + amount.toPlainString();
  }
}
