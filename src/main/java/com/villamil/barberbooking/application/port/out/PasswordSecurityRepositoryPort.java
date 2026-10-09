package com.villamil.barberbooking.application.port.out;

public interface PasswordSecurityRepositoryPort {
    /** Updates the active account using CAS and increments its session version in one transaction. */
    boolean compareAndChangePassword(Long userId, String expectedPasswordHash, String newPasswordHash);
}
