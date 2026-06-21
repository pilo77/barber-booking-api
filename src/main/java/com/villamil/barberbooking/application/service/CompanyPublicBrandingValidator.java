package com.villamil.barberbooking.application.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.dto.command.UpdateCompanyPublicBrandingCommand;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;

@Component
class CompanyPublicBrandingValidator {

	private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
	private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
	private static final Set<String> WHATSAPP_HOSTS = Set.of("wa.me", "api.whatsapp.com");

	NormalizedCompanyPublicBranding normalize(UpdateCompanyPublicBrandingCommand command) {
		if (command.themeMode() == null) {
			throw new BusinessRuleException("Theme mode is required");
		}
		String publicName = normalizeOptionalText(command.publicName(), "Public name", 120, true, false);
		String publicDescription = normalizeOptionalText(command.publicDescription(), "Public description", 1000, false, true);
		return new NormalizedCompanyPublicBranding(
				publicName,
				publicDescription,
				normalizeHttpsUrl(command.logoUrl(), "Logo URL"),
				normalizeHttpsUrl(command.coverImageUrl(), "Cover image URL"),
				normalizeColor(command.primaryColor(), "Primary color"),
				normalizeColor(command.secondaryColor(), "Secondary color"),
				normalizeColor(command.accentColor(), "Accent color"),
				command.themeMode(),
				normalizeOptionalText(command.contactPhone(), "Contact phone", 30, false, false),
				normalizeWhatsappUrl(command.whatsappUrl()),
				normalizeHttpsUrl(command.instagramUrl(), "Instagram URL"),
				normalizeHttpsUrl(command.facebookUrl(), "Facebook URL"),
				normalizeHttpsUrl(command.tiktokUrl(), "TikTok URL"),
				normalizeHttpsUrl(command.websiteUrl(), "Website URL")
		);
	}

	private String normalizeOptionalText(String value, String label, int maxLength, boolean rejectBlank, boolean rejectHtml) {
		if (value == null) {
			return null;
		}
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			if (rejectBlank) {
				throw new BusinessRuleException(label + " cannot be blank");
			}
			return null;
		}
		if (normalized.length() > maxLength) {
			throw new BusinessRuleException(label + " must be at most " + maxLength + " characters");
		}
		if (rejectHtml && HTML_TAG.matcher(normalized).find()) {
			throw new BusinessRuleException(label + " must be plain text");
		}
		return normalized;
	}

	private String normalizeColor(String value, String label) {
		String normalized = normalizeOptionalText(value, label, 7, false, false);
		if (normalized == null) {
			return null;
		}
		if (!HEX_COLOR.matcher(normalized).matches()) {
			throw new BusinessRuleException(label + " must use #RRGGBB format");
		}
		return normalized.toUpperCase();
	}

	private String normalizeHttpsUrl(String value, String label) {
		String normalized = normalizeOptionalText(value, label, 500, false, false);
		if (normalized == null) {
			return null;
		}
		URI uri = parseUri(normalized, label);
		if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().isBlank()) {
			throw new BusinessRuleException(label + " must use https");
		}
		return normalized;
	}

	private String normalizeWhatsappUrl(String value) {
		String normalized = normalizeOptionalText(value, "WhatsApp URL", 500, false, false);
		if (normalized == null) {
			return null;
		}
		URI uri = parseUri(normalized, "WhatsApp URL");
		if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
			throw new BusinessRuleException("WhatsApp URL must use https");
		}
		String host = uri.getHost().toLowerCase();
		if (!WHATSAPP_HOSTS.contains(host)) {
			throw new BusinessRuleException("WhatsApp URL must use an allowed WhatsApp host");
		}
		return normalized;
	}

	private URI parseUri(String value, String label) {
		try {
			return new URI(value);
		}
		catch (URISyntaxException exception) {
			throw new BusinessRuleException(label + " is invalid");
		}
	}

	record NormalizedCompanyPublicBranding(
			String publicName,
			String publicDescription,
			String logoUrl,
			String coverImageUrl,
			String primaryColor,
			String secondaryColor,
			String accentColor,
			com.villamil.barberbooking.domain.valueobject.ThemeMode themeMode,
			String contactPhone,
			String whatsappUrl,
			String instagramUrl,
			String facebookUrl,
			String tiktokUrl,
			String websiteUrl
	) {
	}
}
