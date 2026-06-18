package com.villamil.barberbooking.application.port.in;

import java.util.List;

import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;

public interface ListPublicBranchesUseCase {

	List<PublicBranchResponse> listByCompanySlug(String companySlug);
}
