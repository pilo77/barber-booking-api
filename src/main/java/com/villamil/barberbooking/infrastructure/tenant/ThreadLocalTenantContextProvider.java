package com.villamil.barberbooking.infrastructure.tenant;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;

@Component
public class ThreadLocalTenantContextProvider implements TenantContextProvider {

	private static final ThreadLocal<TenantContext> CURRENT = ThreadLocal.withInitial(() -> TenantContext.DEFAULT);

	public void set(TenantContext tenantContext) {
		CURRENT.set(tenantContext);
	}

	public void clear() {
		CURRENT.remove();
	}

	@Override
	public TenantContext currentTenant() {
		return CURRENT.get();
	}
}
