package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;

public interface GetPublicBarberShopUseCase {

	PublicBarberShopResponse getBySlug(String companySlug);
}
