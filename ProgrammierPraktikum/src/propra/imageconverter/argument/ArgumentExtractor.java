package propra.imageconverter.argument;

import propra.imageconverter.*;
import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;

/** Instanzen dieser Klasse extrahieren aus Benutzer-Eingabeparametern die
 * Attribute des Models und melden Fehler bei zu vielen Eingabeparametern.
 *
 * @author Martina Koch */

public class ArgumentExtractor {
	private String[] executionArguments;
	private String inputFilePath;
	private EFormat inputFormat;

	private String outputFilePath;
	private EFormat outputFormat;
	private ECompressionType outputCompressionType;

	private boolean encodeBase32;
	private boolean decodeBase32;

	public ArgumentExtractor(Controller controller, Model model) throws ImageConverterException {
		this.executionArguments = controller.getExecutionArguments();

		ArgumentChecker.checkArgumentNumber(executionArguments.length);
		extractArguments();

		assignInputOutputFilePathToModel(model);
		assignInputOutputFormatToModel(model);
		assignOutputCompressionTypeToModel(model);
		assignEncodeDecodeBase32ToModel(model);
	}

	/** Methode extrahiert die Kommandozeilenparameter aus der Benutzereingabe
	 *
	 * @param commandLineArguments
	 * @throws ImageConverterException */
	private void extractArguments() throws ImageConverterException {
		String[] commandLineArguments = executionArguments;
		for (String argument : commandLineArguments) {
			if (argument.contains("=")) {
				String[] argumentSplit = argument.split("=");
				// d.h. Datei ist Base-32 dekodiert
				if (argument.endsWith(".base-32")) {
					assignInputFilePathAndFormat(argumentSplit);
					outputFilePath = inputFilePath.substring(0, argumentSplit[1].lastIndexOf("."));
					outputFormat = assignFormat(outputFilePath);
				}

				// Zuordnung In-/Output Pfad und Dateiformat
				else if (argumentSplit[0].equals("--input")) {
					assignInputFilePathAndFormat(argumentSplit);
				}

				else if (argumentSplit[0].equals("--output")) {
					outputFilePath = argumentSplit[1];
					outputFormat = assignFormat(outputFilePath);
				}

				/* legt Kompressionstyp fest. Nach Review-Rückmeldung aus KE2 wurde
				 * Ausnahmebehandlung hinzugefügt, falls Kompressionstyp nicht existiert */
				else if (argumentSplit[0].equals("--compression")) {
					try {
						if (argumentSplit.length == 1) {
							throw new ImageConverterException("Fehlendes Kompressions-Argument");
						}
						outputCompressionType = ECompressionType.valueOf(argumentSplit[1].toUpperCase());
					} catch (Exception e) {
						ImageConverterException.abruptlyExitProgram(e);
					}
				}
			}

			// Lese Argumente Kodierung/Dekodierung
			else if (argument.startsWith("--encode-base-32")) {
				encodeBase32 = true;
				// füge Dateiendung ".base-32" hinzu
				outputFilePath = inputFilePath + ".base-32";
				outputFormat = assignFormat(outputFilePath);
			}

			else if (argument.startsWith("--decode-base-32")) {
				decodeBase32 = true;
				// entferne Dateiendung ".base-32
				outputFilePath = inputFilePath.substring(0, inputFilePath.length() - 8);
				outputFormat = assignFormat(outputFilePath);
			}
		}
	}

	private void assignInputFilePathAndFormat(String[] argumentSplit) throws ImageConverterException {
		inputFilePath = argumentSplit[1];
		inputFormat = assignFormat(inputFilePath);
	}

	private void assignInputOutputFilePathToModel(Model model) {
		model.setInputFilePath(inputFilePath);
		model.setOutputFilePath(outputFilePath);
	}

	private void assignInputOutputFormatToModel(Model model) {
		model.setInputFormat(inputFormat);
		model.setOutputFormat(outputFormat);
	}

	/** Methode setzt OutputCompression im Model, hier bei ist OutputCompression
	 * uncompressed, falls keine Angabe über Terminal-Benutzereingabe erfolgte keine
	 * En-/Dekodierung erfolgen soll
	 *
	 * @param model
	 * @throws ImageConverterException */
	private void assignOutputCompressionTypeToModel(Model model) throws ImageConverterException {
		if (!encodeBase32 && !decodeBase32) {
			if (outputCompressionType != null) {
				model.setOutputCompressionType(outputCompressionType);
			} else {
				model.setOutputCompressionType(ECompressionType.UNCOMPRESSED);
				outputCompressionType = model.getOutputCompressionType();
			}
		}
	}

	/** Legt für Ein-/Ausgabedatei das Dateiformat fest. */
	private EFormat assignFormat(String filePathString) throws ImageConverterException {
		EFormat fileFormat = null;

		if (filePathString.toLowerCase().endsWith(".tga")) {
			fileFormat = EFormat.TGA;
		} else if (filePathString.toLowerCase().endsWith(".propra")) {
			fileFormat = EFormat.PROPRA;
		} else {
			/* Fehlerbehandlung bei nicht bekanntem Dateiformat und nicht
			 * Base-Transformation in ArgumentChecker */
			fileFormat = EFormat.OTHER;
		}
		return fileFormat;
	}

	private void assignEncodeDecodeBase32ToModel(Model model) {
		model.setEncodeBase32(encodeBase32);
		model.setDecodeBase32(decodeBase32);
	}

	public ECompressionType getOutputCompressionTypeFromArgumentExtractor() {
		return outputCompressionType;
	}
}
