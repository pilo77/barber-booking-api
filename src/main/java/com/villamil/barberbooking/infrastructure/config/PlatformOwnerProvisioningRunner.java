package com.villamil.barberbooking.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.villamil.barberbooking.application.service.PlatformOwnerProvisioningService;

@Component
@ConditionalOnProperty(name = "app.platform-owner-provisioning.enabled", havingValue = "true")
public class PlatformOwnerProvisioningRunner implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformOwnerProvisioningRunner.class);
    private final PlatformOwnerProvisioningService service;
    private final String email;
    private final String fullName;
    private final String password;

    public PlatformOwnerProvisioningRunner(PlatformOwnerProvisioningService service,
            @Value("${app.platform-owner-provisioning.email:}") String email,
            @Value("${app.platform-owner-provisioning.full-name:}") String fullName,
            @Value("${app.platform-owner-provisioning.password:}") String password) {
        this.service = service;
        this.email = email;
        this.fullName = fullName;
        this.password = password;
    }

    @Override public void run(ApplicationArguments args) {
        boolean created = service.provision(email, fullName, password);
        LOG.info("Platform owner provisioning completed: {}. Disable provisioning and remove its password configuration.",
                created ? "created" : "already exists; unchanged");
    }
}
