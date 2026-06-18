package com.villamil.barberbooking.application.tenant;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record TenantContext(Long companyId, Long branchId) {

	public static final Long DEFAULT_COMPANY_ID = 1L;
	public static final Long DEFAULT_BRANCH_ID = 1L;
	public static final TenantContext DEFAULT = new TenantContext(DEFAULT_COMPANY_ID, DEFAULT_BRANCH_ID);

	public TenantContext {
		companyId = requirePositive(companyId, "Company id is required");
		branchId = requirePositive(branchId, "Branch id is required");
	}

	private static Long requirePositive(Long value, String message) {
		if (value == null || value <= 0) {
			throw new BusinessRuleException(message);
		}
		return value;
	}
}
