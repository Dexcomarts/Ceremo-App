package io.ceremo.ceremobackend.shared.money.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;

class MoneyTest {

  @Test
  void shouldNotBuildWithoutAmount() {
    assertThatThrownBy(() -> new Money(null, Currency.USD))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("amount");
  }

  @Test
  void shouldNotBuildWithoutCurrency() {
    assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("currency");
  }

  @Test
  void shouldNotBuildWithScaleExceedingCurrencyDecimalPlaces() {
    assertThatThrownBy(() -> new Money(new BigDecimal("10.123"), Currency.USD))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("scale");
  }

  @Test
  void shouldCreateMoneyWithOf() {
    Money money = Money.of(new BigDecimal("10.5"), Currency.USD);
    assertThat(money.amount()).isEqualByComparingTo("10.50");
    assertThat(money.currency()).isEqualTo(Currency.USD);
  }

  @Test
  void shouldCreateMoneyWithOfFromString() {
    Money money = Money.of("10.5", Currency.USD);
    assertThat(money.amount()).isEqualByComparingTo("10.50");
  }

  @Test
  void shouldNotCreateMoneyWithOfFromBlankString() {
    assertThatThrownBy(() -> Money.of(" ", Currency.USD))
      .isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldCreateMoneyWithOfFromLong() {
    Money money = Money.of(10L, Currency.USD);
    assertThat(money.amount()).isEqualByComparingTo("10.00");
  }

  @Test
  void shouldCreateZeroMoney() {
    Money money = Money.zero(Currency.USD);
    assertThat(money.amount()).isEqualByComparingTo("0.00");
    assertThat(money.isZero()).isTrue();
  }

  @Test
  void shouldNotCreateZeroMoneyWithoutCurrency() {
    assertThatThrownBy(() -> Money.zero(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldAddMoney() {
    Money a = Money.of(10L, Currency.USD);
    Money b = Money.of(5L, Currency.USD);
    Money result = a.add(b);
    assertThat(result.amount()).isEqualByComparingTo("15.00");
  }

  @Test
  void shouldNotAddMoneyWithDifferentCurrencies() {
    Money a = Money.of(10L, Currency.USD);
    Money b = Money.of(5L, Currency.EUR);
    assertThatThrownBy(() -> a.add(b))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("different currencies");
  }

  @Test
  void shouldSubtractMoney() {
    Money a = Money.of(10L, Currency.USD);
    Money b = Money.of(3L, Currency.USD);
    Money result = a.subtract(b);
    assertThat(result.amount()).isEqualByComparingTo("7.00");
  }

  @Test
  void shouldNotSubtractMoneyWithDifferentCurrencies() {
    Money a = Money.of(10L, Currency.USD);
    Money b = Money.of(5L, Currency.EUR);
    assertThatThrownBy(() -> a.subtract(b))
      .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldMultiplyMoney() {
    Money money = Money.of(10L, Currency.USD);
    Money result = money.multiply(BigDecimal.valueOf(2));
    assertThat(result.amount()).isEqualByComparingTo("20.00");
  }

  @Test
  void shouldNotMultiplyMoneyWithNullMultiplier() {
    Money money = Money.of(10L, Currency.USD);
    assertThatThrownBy(() -> money.multiply((BigDecimal) null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldMultiplyMoneyWithLong() {
    Money money = Money.of(10L, Currency.USD);
    Money result = money.multiply(3L);
    assertThat(result.amount()).isEqualByComparingTo("30.00");
  }

  @Test
  void shouldNegateMoney() {
    Money money = Money.of(10L, Currency.USD);
    Money result = money.negate();
    assertThat(result.amount()).isEqualByComparingTo("-10.00");
  }

  @Test
  void shouldCheckIsPositive() {
    assertThat(Money.of(10L, Currency.USD).isPositive()).isTrue();
    assertThat(Money.of(-10L, Currency.USD).isPositive()).isFalse();
    assertThat(Money.zero(Currency.USD).isPositive()).isFalse();
  }

  @Test
  void shouldCheckIsNegative() {
    assertThat(Money.of(-10L, Currency.USD).isNegative()).isTrue();
    assertThat(Money.of(10L, Currency.USD).isNegative()).isFalse();
    assertThat(Money.zero(Currency.USD).isNegative()).isFalse();
  }

  @Test
  void shouldCheckIsZero() {
    assertThat(Money.zero(Currency.USD).isZero()).isTrue();
    assertThat(Money.of(10L, Currency.USD).isZero()).isFalse();
  }

  @Test
  void shouldCompareGreaterThan() {
    Money a = Money.of(10L, Currency.USD);
    Money b = Money.of(5L, Currency.USD);
    assertThat(a.isGreaterThan(b)).isTrue();
    assertThat(b.isGreaterThan(a)).isFalse();
  }

  @Test
  void shouldNotCompareGreaterThanWithDifferentCurrencies() {
    Money a = Money.of(10L, Currency.USD);
    Money b = Money.of(5L, Currency.EUR);
    assertThatThrownBy(() -> a.isGreaterThan(b))
      .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCompareLessThan() {
    Money a = Money.of(5L, Currency.USD);
    Money b = Money.of(10L, Currency.USD);
    assertThat(a.isLessThan(b)).isTrue();
    assertThat(b.isLessThan(a)).isFalse();
  }

  @Test
  void shouldNotCompareLessThanWithDifferentCurrencies() {
    Money a = Money.of(5L, Currency.USD);
    Money b = Money.of(10L, Currency.EUR);
    assertThatThrownBy(() -> a.isLessThan(b))
      .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldGetAmount() {
    Money money = Money.of(10L, Currency.USD);
    assertThat(money.get()).isEqualByComparingTo("10.00");
  }

  @Test
  void shouldHaveToString() {
    Money money = Money.of(10L, Currency.USD);
    assertThat(money).hasToString("$10.00");
  }

  @Test
  void shouldNotCompareWithNullOther() {
    Money money = Money.of(10L, Currency.USD);
    assertThatThrownBy(() -> money.add(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class);
  }
}
