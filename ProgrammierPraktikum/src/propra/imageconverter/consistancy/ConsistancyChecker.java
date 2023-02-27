package propra.imageconverter.consistancy;

import propra.imageconverter.*;

/** Abstrakte Klasse zum Überprüfen der Konsistenz der Datei.
 *
 * @author martina */
public abstract class ConsistancyChecker implements IConsistancyChecker {
	public Model model;
	public IHeaderReaderInputFile inputFormat;

	public ConsistancyChecker(Model model, IHeaderReaderInputFile format) throws ImageConverterException {
		this.model = model;
		this.inputFormat = format;

		// allgemeine Tests der Bilddateien
		checkCompression();
		checkHeightWidth();
		checkPixelDepth();
	}

	@Override
	public void checkPixelDepth() throws ImageConverterException {
		if (inputFormat.getPixelDepth() != 24) {
			throw new ImageConverterException("Bits pro Bildpunkt nicht zulässig");
		}
	}

	@Override
	public void checkHeightWidth() throws ImageConverterException {
		if (inputFormat.getWidth() == 0) throw new ImageConverterException("Nullbreite");
		if (inputFormat.getHeight() == 0) throw new ImageConverterException("Nullhöhe");
	}

	@Override
	public void checkCompression() throws ImageConverterException {
		if (inputFormat.getCompressionType().ordinal() > 3) {
			throw new ImageConverterException("Kompressionstyp wird nicht unterstützt");
		}
	}
}
