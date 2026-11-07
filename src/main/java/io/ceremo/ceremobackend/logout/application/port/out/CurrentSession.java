package io.ceremo.ceremobackend.logout.application.port.out;

import io.ceremo.ceremobackend.logout.domain.model.UserSession;

public interface CurrentSession {
  UserSession requireSession();
}
