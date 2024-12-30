package de.braginskij.alexej.util;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class NumberUtil {

	/**
	 * 
	 * @param number Die Ausgangzahl, für die das Bit überprüft werden soll
	 * @param bit    Die Stellennummer des Bits von 0 bis Integer.MAX_VALUE
	 * @return
	 */
	public static boolean bitSet(final BigInteger number, final int bit) {
		assert bit >= 0 : "Negative bit number!";

		if (bit >= number.bitLength()) {
			return false;
		} else {
			final BigInteger bitInt = number.divide(BigInteger.TWO.pow(bit)).mod(BigInteger.TWO);

			return bitInt.compareTo(BigInteger.ONE) == 0;
		}
	}

	/**
	 * 
	 * @param number Die Ausgangszahl, für die die gesetzten Bitnummern bestimmt
	 *               werden sollen
	 * @return die gesetzten Bitnummern als sortiertes Set
	 */
	public static Set<Integer> bitsSet(BigInteger number) {
		final Set<Integer> bits = new TreeSet<Integer>();

		for (int i = 0; number.compareTo(BigInteger.ZERO) > 0; i++, number = number.divide(BigInteger.TWO)) {
			if (number.mod(BigInteger.TWO).compareTo(BigInteger.ONE) == 0) {
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
}
