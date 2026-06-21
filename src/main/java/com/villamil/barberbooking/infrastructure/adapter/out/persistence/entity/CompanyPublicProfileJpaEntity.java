package com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "company_public_profiles")
public class CompanyPublicProfileJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "company_id", nullable = false)
	private Long companyId;

	@Column(name = "public_name", length = 120)
	private String publicName;

	@Column(name = "public_description", length = 1000)
	private String publicDescription;

	@Column(name = "logo_url", length = 500)
	private String logoUrl;

	@Column(name = "cover_image_url", length = 500)
	private String coverImageUrl;

	@Column(name = "primary_color", length = 7)
	private String primaryColor;

	@Column(name = "secondary_color", length = 7)
	private String secondaryColor;

	@Column(name = "accent_color", length = 7)
	private String accentColor;

	@Column(name = "theme_mode", nullable = false, length = 20)
	private String themeMode;

	@Column(name = "contact_phone", length = 30)
	private String contactPhone;

	@Column(name = "contact_whatsapp_url", length = 500)
	private String contactWhatsappUrl;

	@Column(name = "contact_instagram_url", length = 500)
	private String contactInstagramUrl;

	@Column(name = "contact_facebook_url", length = 500)
	private String contactFacebookUrl;

	@Column(name = "contact_tiktok_url", length = 500)
	private String contactTiktokUrl;

	@Column(name = "contact_website_url", length = 500)
	private String contactWebsiteUrl;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP")
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
	private Instant updatedAt;

	protected CompanyPublicProfileJpaEntity() {
	}

	public CompanyPublicProfileJpaEntity(
			Long id,
			Long companyId,
			String publicName,
			String publicDescription,
			String logoUrl,
			String coverImageUrl,
			String primaryColor,
			String secondaryColor,
			String accentColor,
			String themeMode,
			String contactPhone,
			String contactWhatsappUrl,
			String contactInstagramUrl,
			String contactFacebookUrl,
			String contactTiktokUrl,
			String contactWebsiteUrl,
			Instant createdAt,
			Instant updatedAt
	) {
		this.id = id;
		this.companyId = companyId;
		this.publicName = publicName;
		this.publicDescription = publicDescription;
		this.logoUrl = logoUrl;
		this.coverImageUrl = coverImageUrl;
		this.primaryColor = primaryColor;
		this.secondaryColor = secondaryColor;
		this.accentColor = accentColor;
		this.themeMode = themeMode;
		this.contactPhone = contactPhone;
		this.contactWhatsappUrl = contactWhatsappUrl;
		this.contactInstagramUrl = contactInstagramUrl;
		this.contactFacebookUrl = contactFacebookUrl;
		this.contactTiktokUrl = contactTiktokUrl;
		this.contactWebsiteUrl = contactWebsiteUrl;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = createdAt;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getCompanyId() {
		return companyId;
	}

	public String getPublicName() {
		return publicName;
	}

	public String getPublicDescription() {
		return publicDescription;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public String getCoverImageUrl() {
		return coverImageUrl;
	}

	public String getPrimaryColor() {
		return primaryColor;
	}

	public String getSecondaryColor() {
		return secondaryColor;
	}

	public String getAccentColor() {
		return accentColor;
	}

	public String getThemeMode() {
		return themeMode;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public String getContactWhatsappUrl() {
		return contactWhatsappUrl;
	}

	public String getContactInstagramUrl() {
		return contactInstagramUrl;
	}

	public String getContactFacebookUrl() {
		return contactFacebookUrl;
	}

	public String getContactTiktokUrl() {
		return contactTiktokUrl;
	}

	public String getContactWebsiteUrl() {
		return contactWebsiteUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
