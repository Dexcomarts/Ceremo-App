package io.ceremo.ceremobackend.shared.event.application;

import io.ceremo.ceremobackend.shared.event.domain.DomainEvent;

public interface DomainEventPublisher {

  void publish(DomainEvent event);

  void publishAll(Iterable<? extends DomainEvent> events);
}
