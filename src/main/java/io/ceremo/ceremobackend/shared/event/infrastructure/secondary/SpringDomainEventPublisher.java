package io.ceremo.ceremobackend.shared.event.infrastructure.secondary;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import io.ceremo.ceremobackend.shared.event.application.DomainEventPublisher;
import io.ceremo.ceremobackend.shared.event.domain.DomainEvent;

@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

  private final ApplicationEventPublisher publisher;

  SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(DomainEvent event) {
    if (event != null) {
      publisher.publishEvent(event);
    }
  }

  @Override
  public void publishAll(Iterable<? extends DomainEvent> events) {
    if (events != null) {
      events.forEach(this::publish);
    }
  }
}
