package io.ceremo.ceremobackend.shared.pagination.domain;

import java.util.List;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage.CeremoPageBuilder;

public final class CeremoPagesFixture {

  private CeremoPagesFixture() {}

  public static CeremoPage<String> page() {
    return pageBuilder().build();
  }

  public static CeremoPageBuilder<String> pageBuilder() {
    return CeremoPage.builder(List.of("test")).currentPage(2).pageSize(10).totalElementsCount(21);
  }
}
