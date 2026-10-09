package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import com.villamil.barberbooking.application.dto.command.ChangePasswordCommand;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.PasswordSecurityRepositoryPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

class PasswordSecurityServiceTest {
    private static final String OLD = "qa-existing-password-123!";
    private static final String NEW = "qa-new-password-456!";
    private CurrentUserResolver users;
    private UserAccountRepositoryPort accounts;
    private PasswordHasherPort passwords;
    private PasswordSecurityRepositoryPort security;
    private PasswordSecurityService service;

    @BeforeEach void setUp() {
        users = mock(CurrentUserResolver.class);
        accounts = mock(UserAccountRepositoryPort.class);
        passwords = mock(PasswordHasherPort.class);
        security = mock(PasswordSecurityRepositoryPort.class);
        service = new PasswordSecurityService(users, accounts, passwords, security);
        when(users.requireCurrentUser()).thenReturn(AuthenticatedUserResponse.from(account(true)));
    }

    @Test void validChangeUsesAuthenticatedAccountAndCompareAndSwap() {
        stubCurrentAccount();
        when(passwords.matches(OLD, "fixture-current-hash")).thenReturn(true);
        when(passwords.hash(NEW)).thenReturn("fixture-replacement-hash");
        when(security.compareAndChangePassword(1L, "fixture-current-hash", "fixture-replacement-hash")).thenReturn(true);
        service.changePassword(new ChangePasswordCommand(OLD, NEW));
        verify(accounts).findById(1L);
        verify(security).compareAndChangePassword(1L, "fixture-current-hash", "fixture-replacement-hash");
    }

    @Test void wrongCurrentPasswordDoesNotWriteOrHashReplacement() {
        stubCurrentAccount();
        var exception = assertThrows(BusinessRuleException.class,
                () -> service.changePassword(new ChangePasswordCommand("wrong-current", NEW)));
        assertEquals("Unable to change password with the supplied data", exception.getMessage());
        verifyNoInteractions(security);
        verify(passwords, never()).hash(anyString());
    }

    @Test void identicalPasswordCannotBeSavedAgain() {
        stubCurrentAccount();
        when(passwords.matches(OLD, "fixture-current-hash")).thenReturn(true);
        assertThrows(BusinessRuleException.class,
                () -> service.changePassword(new ChangePasswordCommand(OLD, OLD)));
        verifyNoInteractions(security);
        verify(passwords, never()).hash(anyString());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"short", "            "})
    void invalidNewPasswordIsRejectedBeforeReadingAccount(String password) {
        assertThrows(BusinessRuleException.class,
                () -> service.changePassword(new ChangePasswordCommand(OLD, password)));
        verifyNoInteractions(accounts, security, passwords);
    }

    @Test void maximumIsUtf8BytesNotOnlyJavaStringLength() {
        String overLimit = "á".repeat(37);
        assertTrue(overLimit.length() <= 72);
        assertThrows(BusinessRuleException.class,
                () -> service.changePassword(new ChangePasswordCommand(OLD, overLimit)));
        verifyNoInteractions(accounts, security, passwords);
    }

    @Test void passwordOverSeventyTwoAsciiBytesIsRejected() {
        assertThrows(BusinessRuleException.class,
                () -> service.changePassword(new ChangePasswordCommand(OLD, "a".repeat(73))));
        verifyNoInteractions(accounts, security, passwords);
    }

    @Test void inactiveAccountCannotChangePassword() {
        when(accounts.findById(1L)).thenReturn(Optional.of(account(false)));
        assertThrows(BusinessRuleException.class,
                () -> service.changePassword(new ChangePasswordCommand(OLD, NEW)));
        verify(passwords).mitigateMissingAccount(OLD);
        verifyNoInteractions(security);
    }

    @Test void losingConcurrentPasswordChangeDoesNotReportSuccess() {
        stubCurrentAccount();
        when(passwords.matches(OLD, "fixture-current-hash")).thenReturn(true);
        when(passwords.hash(NEW)).thenReturn("fixture-replacement-hash");
        when(security.compareAndChangePassword(1L, "fixture-current-hash", "fixture-replacement-hash")).thenReturn(false);
        assertThrows(BusinessRuleException.class,
                () -> service.changePassword(new ChangePasswordCommand(OLD, NEW)));
    }

    @Test void passwordCommandsNeverRevealValuesThroughToString() {
        assertEquals("ChangePasswordCommand[redacted]", new ChangePasswordCommand(OLD, NEW).toString());
    }

    private void stubCurrentAccount() {
        when(accounts.findById(1L)).thenReturn(Optional.of(account(true)));
    }

    private UserAccount account(boolean active) {
        return new UserAccount(1L, 10L, 20L, "qa@example.test", "fixture-current-hash", "QA User",
                null, null, active, Instant.EPOCH, Instant.EPOCH, Set.of(Role.COMPANY_OWNER));
    }
}
