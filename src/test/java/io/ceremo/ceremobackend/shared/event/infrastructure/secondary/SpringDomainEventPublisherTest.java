package io.ceremo.ceremobackend.shared.event.infrastructure.secondary;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.ceremo.ceremobackend.shared.event.domain.DomainEvent;

@ExtendWith(MockitoExtension.class)
class SpringDomainEventPublisherTest {

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  private SpringDomainEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new SpringDomainEventPublisher(applicationEventPublisher);
  }

  @Test
  void shouldPublishEvent() {
    TestEvent event = new TestEvent();

    publisher.publish(event);

    verify(applicationEventPublisher).publishEvent(event);
  }

  @Test
  void shouldNotPublishNullEvent() {
    publisher.publish(null);

    verifyNoInteractions(applicationEventPublisher);
  }

  @Test
  void shouldPublishAllEvents() {
    TestEvent event1 = new TestEvent();
    TestEvent event2 = new TestEvent();
    List<TestEvent> events = Arrays.asList(event1, event2);

    publisher.publishAll(events);

    verify(applicationEventPublisher).publishEvent(event1);
    verify(applicationEventPublisher).publishEvent(event2);
  }

  @Test
  void shouldNotPublishWhenEventsIterableIsNull() {
    publisher.publishAll(null);

    verifyNoInteractions(applicationEventPublisher);
  }

  @Test
  void shouldHandleEmptyEventsList() {
    publisher.publishAll(Collections.emptyList());

    verifyNoInteractions(applicationEventPublisher);
  }

  @Test
  void shouldSkipNullEventsInIterable() {
    TestEvent event1 = new TestEvent();
    List<TestEvent> events = Arrays.asList(event1, null);

    publisher.publishAll(events);

    verify(applicationEventPublisher, times(1)).publishEvent(event1);
  }

  private static final class TestEvent implements DomainEvent {
    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredOn = Instant.now();

    @Override
    public UUID eventId() {
      return eventId;
    }

    @Override
    public Instant occurredOn() {
      return occurredOn;
    }

    @Override
    public String eventType() {
      return "test.event";
    }
  }
}
