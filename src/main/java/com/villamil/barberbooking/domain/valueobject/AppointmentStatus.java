package com.villamil.barberbooking.domain.valueobject;

public enum AppointmentStatus {
	SCHEDULED,
	IN_PROGRESS,
	COMPLETED,
	CANCELLED,
	NO_SHOW;

	public boolean blocksAvailability() {
		return this == SCHEDULED || this == IN_PROGRESS;
	}
}
