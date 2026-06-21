package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.villamil.barberbooking.application.dto.command.UpdateCompanyPublicBrandingCommand;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;

class CompanyPublicBrandingValidatorTest {

	private final CompanyPublicBrandingValidator validator = new CompanyPublicBrandingValidator();

	@Test
	void normalizeValidPayloadTrimsAndNullifiesOptionalFields() {
		var normalized = validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"  Ponte Perro  ",
				"Descripcion publica",
				"https://cdn.example.com/logo.png",
				"   ",
				"#111111",
				null,
				"#d4af37",
				ThemeMode.SYSTEM,
				" 3001234567 ",
				"https://wa.me/573001234567",
				"https://instagram.com/ponteperro",
				null,
				null,
				"https://ponteperro.example.com"
		));

		assertThat(normalized.publicName()).isEqualTo("Ponte Perro");
		assertThat(normalized.coverImageUrl()).isNull();
		assertThat(normalized.accentColor()).isEqualTo("#D4AF37");
		assertThat(normalized.contactPhone()).isEqualTo("3001234567");
	}

	@Test
	void rejectsInvalidColor() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				null,
				null,
				null,
				"111111",
				null,
				null,
				ThemeMode.SYSTEM,
				null,
				null,
				null,
				null,
				null,
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Primary color must use #RRGGBB format");
	}

	@Test
	void rejectsNonHttpsUrl() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				null,
				"http://cdn.example.com/logo.png",
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
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Logo URL must use https");
	}

	@Test
	void rejectsJavascriptUrl() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				null,
				"javascript:alert(1)",
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
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Logo URL must use https");
	}

	@Test
	void rejectsDataUrl() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				null,
				"data:text/plain;base64,QQ==",
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
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Logo URL must use https");
	}

	@Test
	void rejectsFileUrl() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				null,
				"file:///tmp/logo.png",
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
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Logo URL must use https");
	}

	@Test
	void rejectsInvalidWhatsappHost() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				null,
				null,
				null,
				null,
				null,
				null,
				ThemeMode.SYSTEM,
				null,
				"https://example.com/wa",
				null,
				null,
				null,
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("WhatsApp URL must use an allowed WhatsApp host");
	}

	@Test
	void rejectsNamedColor() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				null,
				null,
				null,
				"red",
				null,
				null,
				ThemeMode.SYSTEM,
				null,
				null,
				null,
				null,
				null,
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Primary color must use #RRGGBB format");
	}

	@Test
	void rejectsShortHexColor() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				null,
				null,
				null,
				"#FFF",
				null,
				null,
				ThemeMode.SYSTEM,
				null,
				null,
				null,
				null,
				null,
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Primary color must use #RRGGBB format");
	}

	@Test
	void rejectsLongColorString() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				null,
				null,
				null,
				"#1234567",
				null,
				null,
				ThemeMode.SYSTEM,
				null,
				null,
				null,
				null,
				null,
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Primary color must be at most 7 characters");
	}

	@Test
	void rejectsHtmlInDescription() {
		assertThatThrownBy(() -> validator.normalize(new UpdateCompanyPublicBrandingCommand(
				"Ponte Perro",
				"<script>alert('x')</script>",
				null,
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
				null
		)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Public description must be plain text");
	}
}
