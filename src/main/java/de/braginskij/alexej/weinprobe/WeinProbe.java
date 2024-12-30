package de.braginskij.alexej.weinprobe;

import java.io.Console;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.braginskij.alexej.util.NumberUtil;

public class WeinProbe {

	private static final String ANGABE_UNGUELTIG = "\n" + "Deine Angabe ist leider ungültig. Versuche es nochmal!\n";
	private static BigInteger anzahlFaesserMax;
	private String commandRegex;
	private BigInteger anzahlFaesser;
	private int anzahlVorkoster;
	private static String vorkosterDateiDir;
	private static String vorkosterDateiNameFormat;

	public WeinProbe(BigInteger anzahlFaesser) throws IOException {
		super();
		this.anzahlFaesser = anzahlFaesser;

		anzahlVorkoster = berechneAnzahlVorkoster(anzahlFaesser);

		buildUpCommandRegex();
	}

	public static int berechneAnzahlVorkoster(final BigInteger anzahlFaesser) {
		// Wenn man nur ein Fass testet, dann ist dieses laut Aufgabenstellung
		// vergiftet. Also sind keine Vorkoster notwendig
		// Bei Zweier-Potenzen muss dass nummernletzte Fass nicht getestet werden.
		// Wenn alle Vorkoster sich nicht vergiftet haben, dann ist es vergiftet
		return anzahlFaesser.subtract(BigInteger.ONE).bitLength();
	}

	public BigInteger getAnzahlFaesser() {
		return anzahlFaesser;
	}

	public int getAnzahlVorkoster() {
		return anzahlVorkoster;
	}

	public String getCommandRegex() {
		return commandRegex;
	}

	public static void main(String[] args) throws IOException {
		Console console = System.console();
		if (console != null) {
			String userName = console.readLine("Willkommen an diesem wunderbaren %s, gib deinen Namen ein: ",
					LocalDateTime.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()));
			console.writer().format("\n" + "Hallo %s\n\n", userName);

			console.writer().println("Ein König hat N Weinfässer.\n"
					+ "Eines enthält Wein, der nach wenigen Tagen einen hässlichen Hautausschlag verursacht.\n"
					+ "Nun möchte der König dennoch in wenigen Tagen ein Weinfest geben.\n"
					+ "Wie viele Verkoster benötigt der König, um das Fass mit dem schlechten Wein sicher zu finden?\n");

			BigInteger anzahlFaesser = leseAnzahlFaesserEin(console);

			final WeinProbe weinProbe = new WeinProbe(anzahlFaesser);
			final int anzahlVorkoster = weinProbe.getAnzahlVorkoster();
			console.writer().format("\n" + "Der König benötigt %d Verkoster!\n\n", anzahlVorkoster);

			boolean quit = false;

			while (!quit) {
				console.writer().format("Du hast folgende Möglichkeiten:\n\n"
						+ "vorkoster <nummer zwischen 0 und %d> : Liefert die Nummern der Fässer, aus den der Verkoster trinken muss." + "\n"
						+ "0 steht für alle Vorkoster. Die Listen werden in einer Datei abgelegt" + "\n"
						+ "\n"
						+ "fass <nummer zwischen 1 und %d> : Liefert die Nummern der Verkoster, die aus diesem Fass trinken müssen." + "\n"
						+ "Wenn sich alle diese Verkoster vergiften, dann ist das Fass vergiftet" + "\n"
						+ "\n"						
						+ "vergiftet <vergifteten Verkoster als Bitkombination, z. B. 01 für erster Vorkoster okay, zweiter vergiftet>: Liefert das vergiftete Fass." + "\n"
						+ "Wenn weniger Bits angegeben werden als es Vorkoster gibt, wird angenommen, dass die verbliebenen Vorkoster sich nicht vergiftet haben" + "\n"
						+ "\n"						
						+ "q : Beenden" 
						+ "\n"
						+ "\n" 
						+ "Gib jetzt deine Anfrage ein: ", anzahlVorkoster, anzahlFaesser);
				final String command = console.readLine().trim();

				quit = weinProbe.interpretCommand(command, console);
			}

			console.format("\n" + "Tschüss %s!\n\n", userName);

			try {
				Thread.sleep(3_000);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}

	protected boolean interpretCommand(final String command, final Console console) throws IOException {
		final boolean doQuit;

		final Matcher matcher = Pattern.compile(commandRegex).matcher(command);

		if (matcher.matches()) {
			final String quit = matcher.group("quit");
			final String vorkoster = matcher.group("vorkoster");
			final String fass = matcher.group("fass");
			final String vergiftet = matcher.group("vergiftet");
			if (quit != null) {
				doQuit = true;
			} else if (vorkoster != null) {
				handleVorkoster(console, matcher.group("vorkosterNummer"));
				doQuit = false;
			} else if (fass != null) {
				handleFass(console, matcher.group("fassNummer"));
				doQuit = false;
			} else if (vergiftet != null) {
				handleVergiftet(console, matcher.group("vergiftetKombination"));
				doQuit = false;
			} else {
				console.writer().println(ANGABE_UNGUELTIG);
				doQuit = false;
			}
		} else {
			console.writer().println(ANGABE_UNGUELTIG);
			doQuit = false;
		}

		return doQuit;
	}

	protected Set<BigInteger> mapVorkosterZuFaesser(final int vorkoster) {
		assert vorkoster > 0 && vorkoster <= anzahlVorkoster;

		final Set<BigInteger> vorkosterZuFaesser = new TreeSet<>();

		for (BigInteger fassNummer = BigInteger.ONE; fassNummer.compareTo(anzahlFaesser) <= 0; fassNummer = fassNummer
				.add(BigInteger.ONE)) {

			if (NumberUtil.bitSet(fassNummer, vorkoster - 1)) {
				vorkosterZuFaesser.add(fassNummer);
			}
		}

		return Collections.unmodifiableSet(vorkosterZuFaesser);
	}

	protected List<Set<BigInteger>> mapVorkosterZuFaesser() {

		final List<Set<BigInteger>> vorkosterZuFass = new ArrayList<Set<BigInteger>>(anzahlVorkoster);

		for (int vorkosterNummer = 0; vorkosterNummer < anzahlVorkoster; vorkosterNummer++) {
			vorkosterZuFass.add(new TreeSet<BigInteger>());
		}

		for (BigInteger fassNummer = BigInteger.ONE; fassNummer.compareTo(anzahlFaesser) <= 0; fassNummer = fassNummer
				.add(BigInteger.ONE)) {
			final Set<Integer> vorkosterNummern = mapFassZuVorkoster(fassNummer);

			for (final int vorkosterNummer : vorkosterNummern) {
				vorkosterZuFass.get(vorkosterNummer - 1).add(fassNummer);
			}
		}

		return vorkosterZuFass;
	}

	protected Set<Integer> mapFassZuVorkoster(BigInteger fass) {

		if (fass.compareTo(BigInteger.TWO.pow(anzahlVorkoster)) == 0) {
			return Collections.emptySet();
		} else {
			return NumberUtil.bitsSet(fass).stream().map(vk -> vk + 1).collect(Collectors.toCollection(TreeSet::new));
		}
	}

	protected BigInteger mapVorkosterKombinationZuFass(final String vergiftetKombinationText) {
		BigInteger intValueOfBooleanString = NumberUtil.valueOfBooleanString(vergiftetKombinationText);

		if (intValueOfBooleanString.compareTo(BigInteger.ZERO) == 0) {
			return BigInteger.TWO.pow(anzahlVorkoster);
		} else {
			return intValueOfBooleanString;
		}
	}

	private void buildUpCommandRegex() {
		commandRegex = String.format(
				"((?<quit>q)" + "|((?<vorkoster>vorkoster)[\\s]*(?<vorkosterNummer>[\\d]{1,%d}))"
						+ "|((?<fass>fass)[\\s]*(?<fassNummer>[\\d]{1,%d}))"
						+ "|((?<vergiftet>vergiftet)[\\s]*(?<vergiftetKombination>[01]{1,%d})))",
				BigInteger.valueOf(anzahlVorkoster).toString().length(), anzahlFaesser.toString().length(),
				anzahlVorkoster);
	}

	private void handleVorkoster(final Console console, final String vorkosterNummerText) throws IOException {
		if (vorkosterNummerText != null) {
			try {
				final int vorkosterNummer = Integer.valueOf(vorkosterNummerText);

				if (vorkosterNummer >= 0 && vorkosterNummer <= anzahlVorkoster) {
					handleRegularVorkoster(console, vorkosterNummer);
				} else {
					console.format("\n" + "Vorkosternummer %d liegt nicht zwischen 0 und %d\n\n", vorkosterNummer,
							anzahlVorkoster);

				}
			} catch (NumberFormatException e) {
				console.format("\n" + "Vorkoster-Nummer %s ist ungültig\n\n", vorkosterNummerText);
			}
		}
	}

	private void handleRegularVorkoster(final Console console, final int vorkosterNummer) throws IOException {

		if (vorkosterNummer == 0) {
			writeVorkosterFile(console, mapVorkosterZuFaesser());
		} else {
			final Set<BigInteger> vorkostersFaesser = mapVorkosterZuFaesser(vorkosterNummer);

			if (vorkostersFaesser.size() <= 100) {
				console.format("\n" + "Vorkosternummer %d muss aus folgenden Fässern probieren: %s\n\n",
						vorkosterNummer, vorkostersFaesser.toString());
			} else {
				writeVorkosterFile(console, vorkosterNummer, vorkostersFaesser);
			}
		}
	}

	private void writeVorkosterFile(final Console console, List<Set<BigInteger>> vorkosterZuFassMapping)
			throws IOException {
		final String vorkosterText = IntStream.range(0, vorkosterZuFassMapping.size())
				.mapToObj(i -> "Vorkoster " + (i + 1) + ": " + vorkosterZuFassMapping.get(i).toString() + "\n")
				.collect(Collectors.joining());

		final Path vorkosterDateiDirPfad = Paths.get(vorkosterDateiDir);
		Files.createDirectories(vorkosterDateiDirPfad);

		final String vorkosterDateiName = MessageFormat.format(vorkosterDateiNameFormat, "alle",
				Instant.now().toEpochMilli());
		final Path vorkosterDateiPfad = vorkosterDateiDirPfad.resolve(vorkosterDateiName);

		Files.writeString(vorkosterDateiPfad, vorkosterText, StandardOpenOption.CREATE);

		console.format("\n" + "Vorkoster wurden in die Datei %s geschrieben" + "\n\n", vorkosterDateiPfad);
	}

	private void writeVorkosterFile(final Console console, final int vorkosterNummer,
			final Set<BigInteger> vorkostersFaesser) throws IOException {
		final Path vorkosterDateiDirPfad = Paths.get(vorkosterDateiDir);
		Files.createDirectories(vorkosterDateiDirPfad);

		final String vorkosterDateiName = MessageFormat.format(vorkosterDateiNameFormat, vorkosterNummer,
				Instant.now().toEpochMilli());
		final Path vorkosterDateiPfad = vorkosterDateiDirPfad.resolve(vorkosterDateiName);

		Files.writeString(vorkosterDateiPfad,
				"Vorkoster %d %s".formatted(vorkosterNummer, vorkostersFaesser.toString()), StandardOpenOption.CREATE);

		console.format("\n" + "Vorkoster wurden in die Datei %s geschrieben" + "\n\n", vorkosterDateiPfad);
	}

	private void handleFass(final Console console, final String fassNummerText) {
		if (fassNummerText != null) {
			try {
				final BigInteger fassNummer = new BigInteger(fassNummerText);

				if (fassNummer.compareTo(BigInteger.ZERO) > 0 && fassNummer.compareTo(anzahlFaesser) <= 0) {
					console.format("\n" + "Fassnummer %d muss von folgenden Vorkostern probiert werden: %s.\n"
							+ "Wenn sich alle diese und nur diese Verkoster vergiften, dann ist dass Fass vergiftet\n\n",
							fassNummer, mapFassZuVorkoster(fassNummer).toString());
				} else {
					console.format("\n" + "Fassnummer %d liegt nicht zwischen 1 und %d\n\n", fassNummer, anzahlFaesser);

				}
			} catch (NumberFormatException e) {
				console.format("\n" + "Fassnummer %s ist ungültig\n\n", fassNummerText);
			}
		}
	}

	private void handleVergiftet(Console console, String vergiftetKombinationText) {
		if (vergiftetKombinationText != null) {

			if (vergiftetKombinationText.length() > anzahlVorkoster) {
				console.format("\n" + "Vorkoster-Kombination %s ist ungültig!\n"
						+ "Sie haben eine Kombination zu mehr Vorkostern angegeben als es Vorkoster gibt!" + "\n\n",
						vergiftetKombinationText);
			}

			try {
				final BigInteger vergiftetesFass = mapVorkosterKombinationZuFass(vergiftetKombinationText);

				if (vergiftetesFass.compareTo(BigInteger.ZERO) > 0 && vergiftetesFass.compareTo(anzahlFaesser) <= 0) {
					console.format("\n" + "Vorkoster-Kombination %s deutet auf das Fass %d hin!\n\n",
							vergiftetKombinationText, vergiftetesFass);
				} else {
					console.format("\n" + "Vorkoster-Kombination %s deutet auf das Fass %d hin.\n"
							+ "Dieses liegt nicht zwischen 1 und %d und diese Kombination ist somit ungültig!\n\n",
							vergiftetKombinationText, vergiftetesFass, anzahlFaesser);

				}
			} catch (NumberFormatException e) {
				console.format("\n" + "Vorkoster-Kombination %s ist ungültig!\n\n", vergiftetKombinationText);
			}
		}
	}

	private static BigInteger leseAnzahlFaesserEin(Console console) throws IOException {
		leseProperties();

		BigInteger anzahlFaesser;

		do {

			final String anzahlFaesserText = console.readLine(
					"Wieviele Fässer hat der König, geben Sie eine Ganzzahl zwischen 1 und %d ein: ", anzahlFaesserMax);

			anzahlFaesser = alsZahl(anzahlFaesserText);

			if (anzahlFaesser == null) {
				console.writer().format("%s ist keine Ganzzahl!\n\n", anzahlFaesserText);
			} else if (anzahlFaesser.compareTo(BigInteger.valueOf(1)) < 0
					|| anzahlFaesser.compareTo(anzahlFaesserMax) > 0) {
				console.writer().format("%s ist keine Ganzzahl zwischen 1 und %d!\n\n", anzahlFaesser,
						anzahlFaesserMax);
				anzahlFaesser = null;
			}
		} while (anzahlFaesser == null);

		return anzahlFaesser;
	}

	private static BigInteger alsZahl(final String string) {
		try {
			return new BigInteger(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void leseProperties() throws IOException {
		final Properties properties = new Properties();

		properties.load(WeinProbe.class.getClassLoader().getResourceAsStream("weinprobe.properties"));

		final BigInteger vorkosterAnzahlMax = new BigInteger(properties.getProperty("vorkoster.anzahl.max"));
		anzahlFaesserMax = BigInteger.TWO.pow(vorkosterAnzahlMax.intValue()).subtract(BigInteger.ONE);
		vorkosterDateiDir = properties.getProperty("vorkoster.datei.dir");
		vorkosterDateiNameFormat = properties.getProperty("vorkoster.datei.name");
	}
}
