package io.ceremo.ceremobackend.shared.multitenancy.application;

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;


class SystemOperationTest {

  @Test
  void shouldBeRetainedAtRuntime() {
    Retention retention = SystemOperation.class.getAnnotation(Retention.class);

    assertThat(retention).isNotNull();
    assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
  }

  @Test
  void shouldTargetMethods() {
    Target target = SystemOperation.class.getAnnotation(Target.class);

    assertThat(target).isNotNull();
    assertThat(target.value()).contains(ElementType.METHOD);
  }

  @Test
  void shouldBeDocumented() {
    Documented documented = SystemOperation.class.getAnnotation(Documented.class);

    assertThat(documented).isNotNull();
  }

  @Test
  void shouldHaveReasonAttribute() throws NoSuchMethodException {
    Method reasonMethod = SystemOperation.class.getDeclaredMethod("reason");

    assertThat(reasonMethod).isNotNull();
    assertThat(reasonMethod.getReturnType()).isEqualTo(String.class);
  }

  @Test
  void shouldBeAccessibleOnAnnotatedMethod() throws NoSuchMethodException {
    Method annotatedMethod = AnnotatedClass.class.getDeclaredMethod("systemMethod");
    SystemOperation annotation = annotatedMethod.getAnnotation(SystemOperation.class);

    assertThat(annotation).isNotNull();
    assertThat(annotation.reason()).isEqualTo("Test reason");
  }

  private static final class AnnotatedClass {
    @SystemOperation(reason = "Test reason")
    public void systemMethod() {
      // Empty method - annotation holder for testing @SystemOperation annotation retrieval
    }
  }
}
