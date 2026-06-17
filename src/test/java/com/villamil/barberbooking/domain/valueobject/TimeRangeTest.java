package com.villamil.barberbooking.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

class TimeRangeTest {

	@Test
	void detectsOverlapUsingOpenEndRange() {
		TimeRange workingRange = new TimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0));
		TimeRange insideRange = new TimeRange(LocalTime.of(11, 30), LocalTime.of(12, 0));
		TimeRange nextRange = new TimeRange(LocalTime.of(12, 0), LocalTime.of(13, 0));

		assertThat(workingRange.overlaps(insideRange)).isTrue();
		assertThat(workingRange.overlaps(nextRange)).isFalse();
	}

	@Test
	void rejectsInvalidRange() {
		assertThatThrownBy(() -> new TimeRange(LocalTime.of(10, 0), LocalTime.of(10, 0)))
				.isInstanceOf(BusinessRuleException.class);
	}
}
