package com.villamil.barberbooking.application.tenant;

public record PublicTenantContext(Long companyId, Long branchId) {

	public TenantContext toTenantContext() {
		return new TenantContext(companyId, branchId);
	}
}
