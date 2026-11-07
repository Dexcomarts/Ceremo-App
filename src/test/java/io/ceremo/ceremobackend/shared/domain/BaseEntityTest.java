package io.ceremo.ceremobackend.shared.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;


class BaseEntityTest {

  @Test
  void shouldCreateEntityWithId() {
    UUID id = UUID.randomUUID();
    TestEntity entity = new TestEntity(id);

    assertThat(entity.getId()).isEqualTo(id);
  }

  @Test
  void shouldReturnNullForAuditFieldsWhenNotSet() {
    UUID id = UUID.randomUUID();
    TestEntity entity = new TestEntity(id);

    assertThat(entity.getCreatedAt()).isNull();
    assertThat(entity.getUpdatedAt()).isNull();
    assertThat(entity.getCreatedBy()).isNull();
    assertThat(entity.getUpdatedBy()).isNull();
    assertThat(entity.getVersion()).isNull();
  }

  @Test
  void shouldBeEqualWhenSameId() {
    UUID id = UUID.randomUUID();
    TestEntity entity1 = new TestEntity(id);
    TestEntity entity2 = new TestEntity(id);

    assertThat(entity1).isEqualTo(entity2).hasSameHashCodeAs(entity2);
  }

  @Test
  void shouldNotBeEqualWhenDifferentId() {
    TestEntity entity1 = new TestEntity(UUID.randomUUID());
    TestEntity entity2 = new TestEntity(UUID.randomUUID());

    assertThat(entity1).isNotEqualTo(entity2);
  }

  @Test
  void shouldBeEqualToItself() {
    TestEntity entity = new TestEntity(UUID.randomUUID());

    assertThat(entity).isEqualTo(entity);
  }

  @Test
  void shouldNotBeEqualToNull() {
    TestEntity entity = new TestEntity(UUID.randomUUID());

    assertThat(entity).isNotEqualTo(null);
  }

  @Test
  void shouldNotBeEqualToDifferentClass() {
    UUID id = UUID.randomUUID();
    TestEntity entity = new TestEntity(id);

    assertThat(entity).isNotEqualTo("not an entity");
  }

  @Test
  void shouldHandleNullIdInEquals() {
    TestEntity entity1 = new TestEntity();
    TestEntity entity2 = new TestEntity();

    assertThat(entity1).isEqualTo(entity2).hasSameHashCodeAs(entity2);
  }

  @Test
  void shouldNotBeEqualWhenOneIdIsNull() {
    TestEntity entity1 = new TestEntity();
    TestEntity entity2 = new TestEntity(UUID.randomUUID());

    assertThat(entity1).isNotEqualTo(entity2);
  }

  @Test
  void shouldAllowSettingId() {
    TestEntity entity = new TestEntity();
    UUID id = UUID.randomUUID();
    entity.setIdPublic(id);

    assertThat(entity.getId()).isEqualTo(id);
  }

  private static class TestEntity extends BaseEntity {
    TestEntity() {
      super();
    }

    TestEntity(UUID id) {
      super(id);
    }

    void setIdPublic(UUID id) {
      setId(id);
    }
  }
}
