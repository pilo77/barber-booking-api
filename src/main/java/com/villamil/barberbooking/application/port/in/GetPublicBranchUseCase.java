package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;

public interface GetPublicBranchUseCase {

	PublicBranchResponse getBySlug(String companySlug, String branchSlug);
}
