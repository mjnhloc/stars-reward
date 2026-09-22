package com.sr.auth.service;

import java.util.UUID;

public interface AuthorizationService {

    void requireView(UUID user, UUID classId);
    void requireAdjust(UUID user, UUID classId);
}
