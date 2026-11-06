package io.ceremo.ceremobackend.shared.money.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;

class CurrencyTest {

  @Test
  void defaultAmountsUseGhanaCedisWithPesewaPrecision() {
    assertThat(Currency.defaultCurrency()).isEqualTo(Currency.GHS);
    assertThat(Currency.defaultCurrency().decimalPlaces()).isEqualTo(2);
  }

  @Test
  void shouldHaveCorrectSymbolForUSD() {
    assertThat(Currency.USD.symbol()).isEqualTo("$");
  }

  @Test
  void shouldHaveCorrectDecimalPlacesForUSD() {
    assertThat(Currency.USD.decimalPlaces()).isEqualTo(2);
  }

  @Test
  void shouldHaveCorrectDecimalPlacesForJPY() {
    assertThat(Currency.JPY.decimalPlaces()).isZero();
  }

  @Test
  void shouldGetCurrencyFromCode() {
    assertThat(Currency.from("USD")).isEqualTo(Currency.USD);
    assertThat(Currency.from("eur")).isEqualTo(Currency.EUR);
  }

  @Test
  void shouldHaveCorrectSymbolAndDecimalPlacesForGHS() {
    assertThat(Currency.GHS.symbol()).isEqualTo("GH\u20B5");
    assertThat(Currency.GHS.decimalPlaces()).isEqualTo(2);
    assertThat(Currency.from("GHS")).isEqualTo(Currency.GHS);
    assertThat(Currency.from("ghs")).isEqualTo(Currency.GHS);
  }

  @Test
  void shouldNotGetCurrencyFromNullCode() {
    assertThatThrownBy(() -> Currency.from(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldNotGetCurrencyFromBlankCode() {
    assertThatThrownBy(() -> Currency.from(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldNotGetCurrencyFromUnknownCode() {
    assertThatThrownBy(() -> Currency.from("XYZ"))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unknown currency code: XYZ");
  }
}
