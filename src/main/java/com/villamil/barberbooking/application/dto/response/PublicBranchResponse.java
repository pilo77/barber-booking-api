package com.villamil.barberbooking.application.dto.response;

public record PublicBranchResponse(
		String slug,
		String name,
		String address,
		String phone
) {
}
