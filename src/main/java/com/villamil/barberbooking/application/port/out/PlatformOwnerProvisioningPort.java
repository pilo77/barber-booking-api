package com.villamil.barberbooking.application.port.out;

public interface PlatformOwnerProvisioningPort {
    boolean createIfAbsent(String email, String fullName, String passwordHash);
}
