package com.villamil.barberbooking.application.port.out;

import com.villamil.barberbooking.application.tenant.TenantContext;

public interface TenantContextProvider {

	TenantContext currentTenant();
}
