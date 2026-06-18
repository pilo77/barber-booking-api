package com.villamil.barberbooking.application.port.in;

import java.util.List;

import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;

public interface ListServiceOfferingsUseCase {

	List<ServiceOfferingResponse> list();
}
