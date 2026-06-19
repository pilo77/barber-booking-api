package com.villamil.barberbooking.application.port.out;

import java.util.function.Supplier;

import com.villamil.barberbooking.application.tenant.TenantContext;

public interface TenantContextExecutor {

	<T> T withTenant(TenantContext tenantContext, Supplier<T> action);
}
