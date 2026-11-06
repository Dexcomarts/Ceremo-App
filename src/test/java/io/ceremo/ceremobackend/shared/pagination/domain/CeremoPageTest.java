package io.ceremo.ceremobackend.shared.pagination.domain;

import static org.assertj.core.api.Assertions.*;
import static io.ceremo.ceremobackend.shared.pagination.domain.CeremoPagesFixture.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;

class CeremoPageTest {

  @Test
  void shouldGetEmptySinglePageWithoutContent() {
    CeremoPage<String> page = CeremoPage.singlePage(null);

    assertEmptyPage(page);
  }

  @Test
  void shouldGetEmptySinglePageFromBuilderWithoutContent() {
    CeremoPage<?> page = CeremoPage.builder(null).build();

    assertEmptyPage(page);
  }

  private void assertEmptyPage(CeremoPage<?> page) {
    assertThat(page.content()).isEmpty();
    assertThat(page.currentPage()).isZero();
    assertThat(page.pageSize()).isZero();
    assertThat(page.totalElementsCount()).isZero();
  }

  @Test
  void shouldGetSinglePage() {
    CeremoPage<String> page = CeremoPage.singlePage(List.of("test", "dummy"));

    assertSinglePage(page);
  }

  @Test
  void shouldGetSinglePageFromBuilderWithContentOnly() {
    CeremoPage<String> page = CeremoPage.builder(List.of("test", "dummy")).build();

    assertSinglePage(page);
  }

  private void assertSinglePage(CeremoPage<String> page) {
    assertThat(page.content()).containsExactly("test", "dummy");
    assertThat(page.currentPage()).isZero();
    assertThat(page.pageSize()).isEqualTo(2);
    assertThat(page.totalElementsCount()).isEqualTo(2);
    assertThat(page.pageCount()).isEqualTo(1);
  }

  @Test
  void shouldGetFullPage() {
    CeremoPage<String> page = pageBuilder().build();

    assertThat(page.content()).containsExactly("test");
    assertThat(page.currentPage()).isEqualTo(2);
    assertThat(page.pageSize()).isEqualTo(10);
    assertThat(page.totalElementsCount()).isEqualTo(21);
    assertThat(page.pageCount()).isEqualTo(3);
  }

  @Test
  void shouldNotMapWithoutMapper() {
    assertThatThrownBy(() -> pageBuilder().build().map(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("mapper");
  }

  @Test
  void shouldMapPage() {
    CeremoPage<String> page = pageBuilder().build().map(entry -> "hey");

    assertThat(page.content()).containsExactly("hey");
    assertThat(page.currentPage()).isEqualTo(2);
    assertThat(page.pageSize()).isEqualTo(10);
    assertThat(page.totalElementsCount()).isEqualTo(21);
    assertThat(page.pageCount()).isEqualTo(3);
  }

  @Test
  void shouldNotBeLastForFirstPage() {
    assertThat(pageBuilder().currentPage(0).build().isNotLast()).isTrue();
  }

  @Test
  void shouldBeLastWithOnePage() {
    assertThat(CeremoPage.singlePage(List.of("d")).isNotLast()).isFalse();
  }

  @Test
  void shouldBeLastPageWithoutContent() {
    CeremoPage<Object> page = CeremoPage.builder(List.of())
      .currentPage(0)
      .pageSize(1)
      .totalElementsCount(0)
      .build();
    assertThat(page.isNotLast()).isFalse();
  }

  @Test
  void shouldBeLastForLastPage() {
    assertThat(pageBuilder().currentPage(2).build().isNotLast()).isFalse();
  }

  @Test
  void shouldGetPageFromElements() {
    CeremoPage<String> page = CeremoPage.of(
      List.of("hello", "java", "world"),
      new CeremoPageable(1, 1)
    );

    assertThat(page.currentPage()).isEqualTo(1);
    assertThat(page.hasNext()).isTrue();
    assertThat(page.hasPrevious()).isTrue();
    assertThat(page.pageCount()).isEqualTo(3);
    assertThat(page.pageSize()).isEqualTo(1);
    assertThat(page.content()).containsExactly("java");
  }

  @Test
  void shouldGetEmptyPageFromOutOfBoundElements() {
    CeremoPage<String> page = CeremoPage.of(
      List.of("hello", "java", "world"),
      new CeremoPageable(4, 1)
    );

    assertThat(page.currentPage()).isEqualTo(4);
    assertThat(page.hasNext()).isFalse();
    assertThat(page.hasPrevious()).isTrue();
    assertThat(page.pageCount()).isEqualTo(3);
    assertThat(page.pageSize()).isEqualTo(1);
    assertThat(page.content()).isEmpty();
  }

  @Test
  void shouldGetPageWithLessThanExpectedElements() {
    CeremoPage<String> page = CeremoPage.of(
      List.of("hello", "java", "world"),
      new CeremoPageable(0, 4)
    );

    assertThat(page.currentPage()).isZero();
    assertThat(page.hasNext()).isFalse();
    assertThat(page.hasPrevious()).isFalse();
    assertThat(page.pageCount()).isEqualTo(1);
    assertThat(page.pageSize()).isEqualTo(4);
    assertThat(page.content()).hasSize(3);
  }

  @Test
  void shouldRejectNullElementsWhenCreatingPage() {
    assertThatThrownBy(() -> CeremoPage.of(null, new CeremoPageable(0, 1)))
        .isInstanceOf(MissingMandatoryValueException.class)
        .hasMessageContaining("elements");
  }

  @Test
  void shouldRejectNullPaginationWhenCreatingPage() {
    assertThatThrownBy(() -> CeremoPage.of(List.of("hello"), null))
        .isInstanceOf(MissingMandatoryValueException.class)
        .hasMessageContaining("pagination");
  }
}
