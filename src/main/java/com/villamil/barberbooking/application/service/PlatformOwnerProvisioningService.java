package com.villamil.barberbooking.application.service;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.PlatformOwnerProvisioningPort;

/** Invoked only by the explicitly enabled startup provisioner, never a public controller. */
@Service
public class PlatformOwnerProvisioningService {
    private final PlatformOwnerProvisioningPort owners;
    private final PasswordHasherPort passwords;

    public PlatformOwnerProvisioningService(PlatformOwnerProvisioningPort owners, PasswordHasherPort passwords) {
        this.owners = owners;
        this.passwords = passwords;
    }

    @Transactional
    public boolean provision(String email, String fullName, String password) {
        if (email == null || email.length() > 120 || !email.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || fullName == null || fullName.isBlank() || fullName.strip().length() > 120) {
            throw new IllegalStateException("Invalid platform owner provisioning configuration");
        }
        return owners.createIfAbsent(email.strip().toLowerCase(Locale.ROOT), fullName.strip(), passwords.hash(password));
    }
}
