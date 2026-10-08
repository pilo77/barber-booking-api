package com.villamil.barberbooking.application.port.out;

import com.villamil.barberbooking.application.dto.response.CompanyContextResponse;

public interface CompanyRegistrationPort {
    CompanyContextResponse create(String companyName, String slug, String branchName,
            String ownerName, String email, String passwordHash);
    CompanyContextResponse context(Long companyId, Long branchId);
}
