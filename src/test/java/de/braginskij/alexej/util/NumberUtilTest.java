package de.braginskij.alexej.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NumberUtilTest {

	@ParameterizedTest
	@MethodSource
	void bitLength(final BigInteger number, final int numberOfPlaces) {
		assertThat(number.bitLength(), is(numberOfPlaces));
	}

	static Stream<Arguments> bitLength() {
		return Stream.of(Arguments.of(BigInteger.ZERO, 0), Arguments.of(BigInteger.ONE, 1),
				Arguments.of(BigInteger.valueOf(4), 3),
				Arguments.of(BigInteger.valueOf(63), 6),
				Arguments.of(BigInteger.valueOf(64), 7));
	}
	
	@ParameterizedTest
	@MethodSource
	void bitSet(final BigInteger number, final int bit, final boolean isSet) {
		assertThat(NumberUtil.bitSet(number, bit), is(isSet));
	}

	static Stream<Arguments> bitSet() {
		return Stream.of(Arguments.of(BigInteger.ZERO, 0, false), Arguments.of(BigInteger.ONE, 0, true),
				Arguments.of(BigInteger.valueOf(4), 2, true),
				Arguments.of(BigInteger.valueOf(63), 5, true),
				Arguments.of(BigInteger.valueOf(63), 6, false),
				Arguments.of(BigInteger.valueOf(64), 6, true));
	}

}
