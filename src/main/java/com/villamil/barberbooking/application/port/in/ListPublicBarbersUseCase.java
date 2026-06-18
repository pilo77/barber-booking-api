package com.villamil.barberbooking.application.port.in;

import java.util.List;

import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;

public interface ListPublicBarbersUseCase {

	List<PublicBarberResponse> listBarbersByBranchSlug(String companySlug, String branchSlug);
}
