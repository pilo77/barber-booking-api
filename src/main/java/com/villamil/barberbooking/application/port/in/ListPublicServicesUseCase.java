package com.villamil.barberbooking.application.port.in;

import java.util.List;

import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;

public interface ListPublicServicesUseCase {

	List<PublicServiceOfferingResponse> listServicesByBranchSlug(String companySlug, String branchSlug);
}
