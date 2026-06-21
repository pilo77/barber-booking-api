package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.UpdateCompanyPublicBrandingCommand;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.CompanyPublicProfileRepositoryPort;
import com.villamil.barberbooking.application.port.out.CurrentUserProvider;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.CompanyPublicProfile;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;

@ExtendWith(MockitoExtension.class)
class CompanyPublicBrandingServiceTest {

	@Mock
	private CompanyPublicProfileRepositoryPort companyPublicProfileRepositoryPort;

	@Mock
	private CurrentUserProvider currentUserProvider;

	@Test
	void ownerReadsCurrentBranding() {
		CompanyPublicBrandingService service = service();
		when(currentUserProvider.currentUser()).thenReturn(Optional.of(companyOwner()));
		when(companyPublicProfileRepositoryPort.findByCompanyId(7L)).thenReturn(Optional.of(profile()));

		var response = service.getCurrent();

		assertThat(response.publicName()).isEqualTo("Ponte Perro");
		assertThat(response.themeMode()).isEqualTo(ThemeMode.SYSTEM);
	}

	@Test
	void ownerUpdatesCurrentBranding() {
		CompanyPublicBrandingService service = service();
		when(currentUserProvider.currentUser()).thenReturn(Optional.of(companyOwner()));
		when(companyPublicProfileRepositoryPort.findByCompanyId(7L)).thenReturn(Optional.of(profile()));
		when(companyPublicProfileRepositoryPort.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.update(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro Premium",
				"Branding nuevo",
				"https://cdn.example.com/logo.png",
				null,
				"#111111",
				null,
				"#D4AF37",
				ThemeMode.DARK,
				"3001234567",
				"https://wa.me/573001234567",
				null,
				null,
				null,
				"https://ponteperro.example.com"
		));

		assertThat(response.publicName()).isEqualTo("Ponte Perro Premium");
		assertThat(response.themeMode()).isEqualTo(ThemeMode.DARK);
		verify(companyPublicProfileRepositoryPort).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void userWithoutPermissionIsRejected() {
		CompanyPublicBrandingService service = service();
		when(currentUserProvider.currentUser()).thenReturn(Optional.of(new AuthenticatedUserResponse(
				2L,
				"barber@example.com",
				"Barber",
				7L,
				3L,
				4L,
				Set.of(Role.BARBER)
		)));

		assertThatThrownBy(service::getCurrent)
				.isInstanceOf(ForbiddenOperationException.class)
				.hasMessage("User role cannot manage company public branding");
	}

	private CompanyPublicBrandingService service() {
		return new CompanyPublicBrandingService(
				companyPublicProfileRepositoryPort,
				new CurrentUserResolver(currentUserProvider),
				new CompanyPublicBrandingAuthorizationPolicy(),
				new CompanyPublicBrandingValidator()
		);
	}

	private AuthenticatedUserResponse companyOwner() {
		return new AuthenticatedUserResponse(
				1L,
				"owner@example.com",
				"Owner",
				7L,
				2L,
				null,
				Set.of(Role.COMPANY_OWNER)
		);
	}

	private CompanyPublicProfile profile() {
		Instant now = Instant.parse("2026-06-21T12:00:00Z");
		return new CompanyPublicProfile(
				1L,
				7L,
				"Ponte Perro",
				"Cortes modernos",
				"https://cdn.example.com/logo-old.png",
				null,
				null,
				null,
				null,
				ThemeMode.SYSTEM,
				null,
				null,
				null,
				null,
				null,
				null,
				now,
				now
		);
	}
}
