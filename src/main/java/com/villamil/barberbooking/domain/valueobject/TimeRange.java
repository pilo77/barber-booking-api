package com.villamil.barberbooking.domain.valueobject;

import java.time.LocalTime;
import java.util.Objects;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record TimeRange(LocalTime start, LocalTime end) {

	public TimeRange {
		Objects.requireNonNull(start, "Start time is required");
		Objects.requireNonNull(end, "End time is required");
		if (!start.isBefore(end)) {
			throw new BusinessRuleException("Start time must be before end time");
		}
	}

	public boolean contains(LocalTime value) {
		Objects.requireNonNull(value, "Time value is required");
		return !value.isBefore(start) && value.isBefore(end);
	}

	public boolean overlaps(TimeRange other) {
		Objects.requireNonNull(other, "Other range is required");
		return start.isBefore(other.end()) && end.isAfter(other.start());
	}
}
