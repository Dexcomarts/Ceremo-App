package io.ceremo.ceremobackend.shared.pagination.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class RestCeremoPageTest {
  @Test
  void bindsPagingParametersAndValidatesThemBeforeConversion() {
    var rest = new RestCeremoPageable();
    var request = new org.springframework.mock.web.MockHttpServletRequest();
    request.addParameter("page", "2");
    request.addParameter("pageSize", "25");
    new org.springframework.web.bind.ServletRequestDataBinder(rest).bind(request);
    assertThat(rest.getPage()).isEqualTo(2);
    assertThat(rest.getPageSize()).isEqualTo(25);
    assertThat(rest.toPageable()).isEqualTo(new CeremoPageable(2, 25));
    rest.setPageSize(101);
    assertThatThrownBy(rest::toPageable).isInstanceOf(AssertionException.class);
  }
  @Test
  void serializesPageContentAndMetadata() {
    var page = RestCeremoPage.from(
        CeremoPage.of(List.of("caterer", "photographer", "decorator"), new CeremoPageable(1, 2)),
        String::toUpperCase);
    var json = new ObjectMapper().valueToTree(page);
    assertThat(json.get("content").get(0).asString()).isEqualTo("DECORATOR");
    assertThat(json.get("currentPage").asInt()).isEqualTo(1);
    assertThat(json.get("pageSize").asInt()).isEqualTo(2);
    assertThat(json.get("totalElementsCount").asLong()).isEqualTo(3);
    assertThat(json.get("pagesCount").asInt()).isEqualTo(2);
    assertThat(json.get("hasPrevious").asBoolean()).isTrue();
    assertThat(json.get("hasNext").asBoolean()).isFalse();
  }

  @Test
  void validatesPagingBoundsAndPreventsOffsetOverflow() {
    var rest = new RestCeremoPageable();
    assertThat(rest.toPageable().page()).isZero();
    assertThat(rest.toPageable().pageSize()).isEqualTo(10);
    assertThatThrownBy(() -> new RestCeremoPageable(-1, 10).toPageable())
        .isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new RestCeremoPageable(0, 101).toPageable())
        .isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new CeremoPageable(Integer.MAX_VALUE, 100))
        .isInstanceOf(AssertionException.class);
    assertThat(CeremoPage.of(List.of("provider"), new CeremoPageable(Integer.MAX_VALUE, 1)).content())
        .isEmpty();
  }
}
