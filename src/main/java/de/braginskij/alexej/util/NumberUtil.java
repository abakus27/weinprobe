package de.braginskij.alexej.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import de.braginskij.alexej.math.BigDecimalMath;

public class NumberUtil {

	public static Set<Integer> bitsSet(BigInteger number) {
		final Set<Integer> bits = new TreeSet<Integer>();
	
		for (int i = 0; number.compareTo(BigInteger.ZERO) > 0; i++, number = number.divide(BigInteger.valueOf(2))) {
			if (number.mod(BigInteger.valueOf(2)).compareTo(BigInteger.ONE) == 0) {
				bits.add(i);
			}
		}
	
		return Collections.unmodifiableSet(bits);
	}

	public static BigInteger valueOfBooleanString(final String vergiftetKombinationText) {
		char[] bits = vergiftetKombinationText.toCharArray();
	
		BigInteger wert = BigInteger.ZERO;
		BigInteger stellenWert = BigInteger.ONE;
		for (final char bit : bits) {
			if (bit == '1') {
				wert = wert.add(stellenWert);
			} else if (bit == '0') {
				// do nothing
			} else {
				throw new NumberFormatException();
			}
	
			stellenWert = stellenWert.multiply(BigInteger.valueOf(2));
		}
	
		return wert;
	}

	public static int computeNumberOfPlaces(final BigInteger number, final int numberBase) {
		if (number.compareTo(BigInteger.ZERO) <= 0) {
			return 0;
		} else if (number.compareTo(BigInteger.ONE) == 0) {
			return 1;
		} else {
			return BigDecimalMath.log(new BigDecimal(number)).divide(BigDecimalMath.log(new BigDecimal(numberBase)), 0, RoundingMode.FLOOR).add(new BigDecimal(1)).intValue();
		}
	}

}
