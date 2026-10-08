package com.villamil.barberbooking.application.dto.command;

public record RegisterCompanyCommand(String companyName, String companySlug, String branchName,
        String fullName, String email, String password) {
    @Override public String toString() { return "RegisterCompanyCommand[redacted]"; }
}
