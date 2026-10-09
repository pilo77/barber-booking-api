package com.villamil.barberbooking.application.port.out;

import java.util.OptionalLong;

public interface SessionVersionRepositoryPort {
    long currentVersion(Long userId);

    /** Binds token issuance to the password that was actually validated during login. */
    OptionalLong currentVersionForPasswordSnapshot(Long userId, String passwordHash);
}
