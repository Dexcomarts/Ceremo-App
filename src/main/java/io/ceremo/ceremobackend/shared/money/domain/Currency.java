package io.ceremo.ceremobackend.shared.money.domain;

import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.ceremo.ceremobackend.shared.error.domain.Assert;

public enum Currency {
  USD("$", 2),
  EUR("\u20AC", 2),
  GBP("\u00A3", 2),
  JPY("\u00A5", 0),
  CHF("CHF", 2),
  CAD("C$", 2),
  AUD("A$", 2),
  CNY("\u00A5", 2),
  INR("\u20B9", 2),
  BRL("R$", 2),
  GHS("GH\u20B5", 2);

  private static final Map<String, Currency> CURRENCIES = buildCurrencies();

  private final String symbol;
  private final int decimalPlaces;

  Currency(String symbol, int decimalPlaces) {
    this.symbol = symbol;
    this.decimalPlaces = decimalPlaces;
  }

  private static Map<String, Currency> buildCurrencies() {
    return Stream.of(values())
        .collect(Collectors.toUnmodifiableMap(Currency::name, Function.identity()));
  }

  public String symbol() {
    return symbol;
  }

  public int decimalPlaces() {
    return decimalPlaces;
  }

  public static Currency defaultCurrency() {
    return GHS;
  }

  public static Currency from(String code) {
    Assert.notBlank("code", code);
    Currency currency = CURRENCIES.get(code.toUpperCase(Locale.ROOT));
    if (currency == null) {
      throw new IllegalArgumentException("Unknown currency code: " + code);
    }
    return currency;
  }
}
