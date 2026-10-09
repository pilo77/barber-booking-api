package com.villamil.barberbooking.application.dto.command;

public record ChangePasswordCommand(String oldPassword, String newPassword) {
    @Override
    public String toString() {
        return "ChangePasswordCommand[redacted]";
    }
}
