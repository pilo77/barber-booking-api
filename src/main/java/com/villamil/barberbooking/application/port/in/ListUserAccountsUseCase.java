package com.villamil.barberbooking.application.port.in;

import java.util.List;

import com.villamil.barberbooking.application.dto.response.UserAccountResponse;

public interface ListUserAccountsUseCase {

	List<UserAccountResponse> list();
}
