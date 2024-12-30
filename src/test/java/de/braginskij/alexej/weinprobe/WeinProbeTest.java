package de.braginskij.alexej.weinprobe;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.braginskij.alexej.util.NumberUtil;

class WeinProbeTest {

	@ParameterizedTest
	@MethodSource
	void berechneAnzahlVorkoster(final BigInteger anzahlFaesser, final int anzahlVorkoster) {
		assertThat(WeinProbe.berechneAnzahlVorkoster(anzahlFaesser), is(anzahlVorkoster));
	}

	static Stream<Arguments> berechneAnzahlVorkoster() {
		return Stream.of(Arguments.of(BigInteger.ONE, 0), Arguments.of(BigInteger.valueOf(2), 1), Arguments.of(BigInteger.valueOf(3), 2), Arguments.of(BigInteger.valueOf(4), 2),
				Arguments.of(BigInteger.valueOf(100), 7), Arguments.of(BigInteger.valueOf(128), 7), Arguments.of(BigInteger.valueOf(255), 8), Arguments.of(BigInteger.valueOf(256), 8));
	}

	@ParameterizedTest
	@MethodSource
	void getCommandRegex(final BigInteger anzahlFaesser, final String regex) throws Exception {
		final WeinProbe weinProbe = new WeinProbe(anzahlFaesser);

		assertThat(weinProbe.getCommandRegex(), is(regex));
	}

	static Stream<Arguments> getCommandRegex() {
		return Stream.of(Arguments.of(BigInteger.valueOf(100), "((?<quit>q)" + "|((?<vorkoster>vorkoster)[\\s]*(?<vorkosterNummer>[\\d]{1,1}))"
				+ "|((?<fass>fass)[\\s]*(?<fassNummer>[\\d]{1,3}))"
				+ "|((?<vergiftet>vergiftet)[\\s]*(?<vergiftetKombination>[01]{1,7})))"),
				Arguments.of(BigInteger.valueOf(128), "((?<quit>q)" + "|((?<vorkoster>vorkoster)[\\s]*(?<vorkosterNummer>[\\d]{1,1}))"
						+ "|((?<fass>fass)[\\s]*(?<fassNummer>[\\d]{1,3}))"
						+ "|((?<vergiftet>vergiftet)[\\s]*(?<vergiftetKombination>[01]{1,7})))"));
	}

	@ParameterizedTest
	@MethodSource
	void mapFassZuVorkoster(final BigInteger fass, final Set<BigInteger> vorkosterKombination) throws IOException {
		final WeinProbe weinProbe = new WeinProbe(BigInteger.valueOf(100));

		assertThat(weinProbe.mapFassZuVorkoster(fass), containsInAnyOrder(vorkosterKombination.toArray()));
	}

	static Stream<Arguments> mapFassZuVorkoster() {
		return Stream.of(Arguments.of(BigInteger.ONE, Set.of(1)), Arguments.of(BigInteger.valueOf(2), Set.of(2)), Arguments.of(BigInteger.valueOf(3), Set.of(1, 2)),
				Arguments.of(BigInteger.valueOf(4), Set.of(3)), Arguments.of(BigInteger.valueOf(100), Set.of(3, 6, 7)), Arguments.of(BigInteger.valueOf(128), Set.of()));
	}

	@ParameterizedTest
	@MethodSource
	void mapVorkosterZuFaesser(final int vorkoster, final Set<BigInteger> vorkosterZuFass) throws IOException {

		final WeinProbe weinProbe = new WeinProbe(BigInteger.valueOf(100));

		assertThat(weinProbe.mapVorkosterZuFaesser(vorkoster), containsInAnyOrder(vorkosterZuFass.toArray()));
	}

	static Stream<Arguments> mapVorkosterZuFaesser() {
		return Stream.of(Arguments.of(1, Stream.iterate(BigInteger.ONE, i -> i.add(BigInteger.valueOf(2))).limit(50).collect(Collectors.toSet())),
				Arguments.of(7, Stream.iterate(BigInteger.valueOf(64), i -> i.add(BigInteger.valueOf(1))).limit(37).collect(Collectors.toSet())));
	}

	@ParameterizedTest
	@MethodSource
	void valueOfBooleanString_Erfolg(final String vorkosterKombination, final int fass) throws IOException {
		assertThat(NumberUtil.valueOfBooleanString(vorkosterKombination), is(BigInteger.valueOf(fass)));
	}

	static Stream<Arguments> valueOfBooleanString_Erfolg() {
		return Stream.of(Arguments.of("0", 0), Arguments.of("1", 1), Arguments.of("01", 2), Arguments.of("11", 3),
				Arguments.of("111000", 7));
	}

	@Test
	void valueOfBooleanString_Fehler() throws Exception {
		assertThrows(NumberFormatException.class, () -> NumberUtil.valueOfBooleanString("1010x"),
				"Der Text hat nicht den Format einer Binärzahl!");
	}

	@Test
	void mapAlleVorkosterZuFaesser() throws IOException {
		final WeinProbe weinProbe = new WeinProbe(BigInteger.valueOf(100));

		assertThat(weinProbe.mapVorkosterZuFaesser().size(), is(weinProbe.getAnzahlVorkoster()));
		weinProbe.mapVorkosterZuFaesser().forEach(vkf -> assertThat(vkf.size(), lessThanOrEqualTo(50)));
	}

	@Nested
	@DisplayName("interpretCommand")
	static class CommandInterpreteer {

		private static final String ANGABE_UNGUELTIG = "\n"
				+ "Deine Angabe ist leider ungültig. Versuche es nochmal!\n";

		@Test
		void vorkoster() throws Exception {
			final Console console = mock(Console.class);

			when(console.format("\n" + "Vorkosternummer %d muss aus folgenden Fässern probieren: %s\n\n", 1,
					"[1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 37, 39, 41, 43, 45, 47, 49, 51, 53, 55, 57, 59, 61, 63, 65, 67, 69, 71, 73, 75, 77, 79, 81, 83, 85, 87, 89, 91, 93, 95, 97, 99]"))
					.thenReturn(console);

			final WeinProbe weinProbe = new WeinProbe(BigInteger.valueOf(100));

			weinProbe.interpretCommand("vorkoster 1", console);

			verify(console, only()).format("\n" + "Vorkosternummer %d muss aus folgenden Fässern probieren: %s\n\n", 1,
					"[1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 37, 39, 41, 43, 45, 47, 49, 51, 53, 55, 57, 59, 61, 63, 65, 67, 69, 71, 73, 75, 77, 79, 81, 83, 85, 87, 89, 91, 93, 95, 97, 99]");

		}

		static class Fass {
			@Test
			@DisplayName("Fassnummer widerspricht dem Regex bzgl. Dezimal-Stellen-Anzahl")
			void ungueltig() throws Exception {
				final Console console = mock(Console.class);
				final PrintWriter writer = mock(PrintWriter.class);
				when(console.writer()).thenReturn(writer);

				doNothing().when(writer).println(ANGABE_UNGUELTIG);

				final WeinProbe weinProbe = new WeinProbe(BigInteger.valueOf(99));

				weinProbe.interpretCommand("fass 100", console);

				verify(writer, only()).println(ANGABE_UNGUELTIG);
			}

			@Test
			void gueltig() throws Exception {
				final Console console = mock(Console.class);
				when(console.format("\n" + "Fassnummer %d muss von folgenden Vorkostern probiert werden: %s.\n"
						+ "Wenn sich alle diese und nur diese Verkoster vergiften, dann ist dass Fass vergiftet\n\n",
						100, "[3, 6, 7]")).thenReturn(console);

				final WeinProbe weinProbe = new WeinProbe(BigInteger.valueOf(100));

				weinProbe.interpretCommand("fass 100", console);

				verify(console, only()).format("\n"
						+ "Fassnummer %d muss von folgenden Vorkostern probiert werden: %s.\n"
						+ "Wenn sich alle diese und nur diese Verkoster vergiften, dann ist dass Fass vergiftet\n\n",
						100, "[3, 6, 7]");
			}
		}

		@Test
		void vergiftet() throws Exception {
			final Console console = mock(Console.class);

			when(console.format(
					"\n" + "Vorkoster-Kombination %s deutet auf das Fass %d hin.\n"
							+ "Dieses liegt nicht zwischen 1 und %d und diese Kombination ist somit ungültig!\n\n",
					"1111111", 127, 100)).thenReturn(console);

			final WeinProbe weinProbe = new WeinProbe(BigInteger.valueOf(100));

			weinProbe.interpretCommand("vergiftet 1111111", console);

			verify(console, only()).format(
					"\n" + "Vorkoster-Kombination %s deutet auf das Fass %d hin.\n"
							+ "Dieses liegt nicht zwischen 1 und %d und diese Kombination ist somit ungültig!\n\n",
					"1111111", 127, 100);

		}
	}
}
