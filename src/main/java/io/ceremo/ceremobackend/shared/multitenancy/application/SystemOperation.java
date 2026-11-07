package io.ceremo.ceremobackend.shared.multitenancy.application;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a system operation that requires cross-tenant access. When applied, the tenant
 * filter will be disabled for the duration of the method execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SystemOperation {

  /** Reason why this operation needs cross-tenant access. */
  String reason();
}
