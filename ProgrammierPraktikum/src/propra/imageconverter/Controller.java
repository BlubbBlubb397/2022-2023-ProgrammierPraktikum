package propra.imageconverter;

import java.io.IOException;

import propra.imageconverter.argument.ArgumentChecker;
import propra.imageconverter.argument.ArgumentExtractor;
import propra.imageconverter.consistancy.ConsistancyCheckerProPra;
import propra.imageconverter.consistancy.ConsistancyCheckerTGA;
import propra.imageconverter.enums.EFormat;
import propra.imageconverter.reader.header.HeaderReaderProPraInputFile;
import propra.imageconverter.reader.header.HeaderReaderTGAInputFile;
import propra.imageconverter.transportcoding.EncodeDecodeBase32;

/** Instanz dieser Klasse instanziiert über die jeweiligen Konstruktoren das
 * Datenmodel, den ArgumentExtractor und ArgumentChecker für die
 * Eingabeargumente des Nutzers, den ConsistancyChecker und den HeaderReader für
 * das Input-File und stößt mit Base32 die Transportkodierung bzw. die
 * Transformation ins Ausgabeformat an. <br>
 *
 * @author Martina Koch */
public class Controller {
	private Model model;
	private String[] excecutionArguments;
	private ArgumentExtractor argumentExctractor;
	private IHeaderReaderInputFile headerReaderInputFile;
	private IConsistancyChecker consistancyCheckerInputFile;
	private EFormat inputFormat;

	public Controller(String[] commandLineArguments) throws ImageConverterException, IOException {
		this.model = new Model();
		this.excecutionArguments = commandLineArguments;
		argumentExctractor = new ArgumentExtractor(this, model);
		new ArgumentChecker(model);

		this.inputFormat = model.getInputFormat();

		// starte Transportkodierung/-enkodierung Base32
		if (model.getEncodeBase32() || model.getDecodeBase32()) {
			// gibt Konsolen-Eingabeparameter für Benutzer aus
			System.out.println(model);
			initializeTransportCodingEncodingBase32();
		}

		// starte Konvertierung bzw. Transformation ins Output-Format
		else {
			initializeHeaderReaderForInputFile();
			initializeConsistancyCheckerForInputFile();
			assignInputOutputCompressionTypeToModel();
			printStatusToScreenForUser();

			initializeTransformationToOutputFile();

			checkConsistancyCheckSumIfProPraInputFile();
		}
		System.out.println("Konvertierung erfolgreich!");
	}

	/** Startet Transportkodierung/-enkodierung Base32
	 *
	 * @throws ImageConverterException
	 * @throws IOException */
	private void initializeTransportCodingEncodingBase32() throws ImageConverterException, IOException {
		new EncodeDecodeBase32(model);
	}

	private void initializeHeaderReaderForInputFile() throws ImageConverterException, IOException {
		this.headerReaderInputFile = inputFormat.equals(EFormat.TGA) ? new HeaderReaderTGAInputFile(model)
		        : new HeaderReaderProPraInputFile(model);
	}

	private void initializeConsistancyCheckerForInputFile() throws ImageConverterException, IOException {
		this.consistancyCheckerInputFile = inputFormat.equals(EFormat.TGA)
		        ? new ConsistancyCheckerTGA(model, headerReaderInputFile)
		        : new ConsistancyCheckerProPra(model, headerReaderInputFile);
	}

	private void assignInputOutputCompressionTypeToModel() throws ImageConverterException {
		model.setInputCompressionType(headerReaderInputFile.getCompressionType());
		model.setOutputCompressionType(argumentExctractor.getOutputCompressionTypeFromArgumentExtractor());
	}

	private void initializeTransformationToOutputFile() throws ImageConverterException, IOException {
		new ConverterToOutputFile(model, headerReaderInputFile);
	}

	private void checkConsistancyCheckSumIfProPraInputFile() throws ImageConverterException {
		if (inputFormat.equals(EFormat.PROPRA)) {
			((ConsistancyCheckerProPra) consistancyCheckerInputFile).checkCheckSum();
		}
	}

	public String[] getExecutionArguments() {
		return excecutionArguments;
	}

	private void printStatusToScreenForUser() {
		// Ausgabe der Eingabeparameter
		System.out.println(model);
		// Ausgabe der gewählten Konvertierung im Programm
		System.out.println(this);
	}

	@Override
	public String toString() {
		StringBuilder stringOutput = new StringBuilder();
		stringOutput.append("Konvertiere " + model.getInputFormat() + " (" + model.getInputCompressionType() + ") zu "
		        + model.getOutputFormat() + " (" + model.getOutputCompressionType() + ")");
		return stringOutput.toString();
	}
}
