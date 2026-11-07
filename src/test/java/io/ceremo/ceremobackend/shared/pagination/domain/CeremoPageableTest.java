package io.ceremo.ceremobackend.shared.pagination.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import io.ceremo.ceremobackend.shared.error.domain.NumberValueTooHighException;
import io.ceremo.ceremobackend.shared.error.domain.NumberValueTooLowException;

class CeremoPageableTest {

  @Test
  void shouldNotBuildWithNegativePage() {
    assertThatThrownBy(() -> new CeremoPageable(-1, 10))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("page");
  }

  @Test
  void shouldNotBuildWithPageSizeAtZero() {
    assertThatThrownBy(() -> new CeremoPageable(0, 0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("pageSize");
  }

  @Test
  void shouldNotBuildWithPageSizeOverHundred() {
    assertThatThrownBy(() -> new CeremoPageable(0, 101))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("pageSize");
  }

  @Test
  void shouldGetFirstPageInformation() {
    var pageable = new CeremoPageable(0, 15);

    assertThat(pageable.page()).isZero();
    assertThat(pageable.pageSize()).isEqualTo(15);
    assertThat(pageable.offset()).isZero();
  }

  @Test
  void shouldGetPageableInformation() {
    var pageable = new CeremoPageable(2, 15);

    assertThat(pageable.page()).isEqualTo(2);
    assertThat(pageable.pageSize()).isEqualTo(15);
    assertThat(pageable.offset()).isEqualTo(30);
  }

  @Test
  void shouldBeEqualWhenPageAndSizeAreSame() {
    var pageable = new CeremoPageable(2, 15);
    var samePageable = new CeremoPageable(2, 15);

    assertThat(pageable)
      .isEqualTo(pageable)
      .isEqualTo(samePageable)
      .hasSameHashCodeAs(samePageable);
  }

  @Test
  void shouldNotBeEqualWhenPageOrSizeAreDifferent() {
    var pageable = new CeremoPageable(2, 15);

    assertThat(pageable)
      .isNotEqualTo(null)
      .isNotEqualTo("pageable")
      .isNotEqualTo(new CeremoPageable(1, 15))
      .isNotEqualTo(new CeremoPageable(2, 10));
  }
}
