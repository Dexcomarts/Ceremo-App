package io.ceremo.ceremobackend.shared.pagination.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import io.ceremo.ceremobackend.shared.error.domain.Assert;

public class CeremoPageable {

  private final int page;
  private final int pageSize;
  private final int offset;

  public CeremoPageable(int page, int pageSize) {
    Assert.field("page", page).min(0);
    Assert.field("pageSize", pageSize).min(1).max(100);
    Assert.field("page", page).max(Integer.MAX_VALUE / pageSize);

    this.page = page;
    this.pageSize = pageSize;
    offset = Math.multiplyExact(page, pageSize);
  }

  public int page() {
    return page;
  }

  public int pageSize() {
    return pageSize;
  }

  public int offset() {
    return offset;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(page).append(pageSize).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    CeremoPageable other = (CeremoPageable) obj;
    return new EqualsBuilder().append(page, other.page).append(pageSize, other.pageSize).build();
  }
}
