package com.villamil.barberbooking.infrastructure.tenant;

import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.TenantContextExecutor;
import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;

@Component
public class ThreadLocalTenantContextProvider implements TenantContextProvider, TenantContextExecutor {

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

	@Override
	public <T> T withTenant(TenantContext tenantContext, Supplier<T> action) {
		TenantContext previous = CURRENT.get();
		CURRENT.set(Objects.requireNonNull(tenantContext, "Tenant context is required"));
		try {
			return Objects.requireNonNull(action, "Tenant action is required").get();
		}
		finally {
			CURRENT.set(previous);
		}
	}
}
