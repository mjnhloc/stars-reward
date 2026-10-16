package com.sr.auth.service;

import com.sr.auth.dto.AccessToken;
import com.sr.auth.dto.RefreshTokenSession;

public interface AccessTokenService {
    AccessToken create(RefreshTokenSession session);
}
