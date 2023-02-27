package propra.imageconverter.consistancy;

import propra.imageconverter.*;
import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.reader.header.HeaderReaderTGAInputFile;

/** Instanzen dieser Klasse überprüfen die Konsistenz der TGA-Eingabe-Datei.
 *
 * @author Martina Koch */
public class ConsistancyCheckerTGA extends ConsistancyChecker implements IConsistancyChecker {
	private IHeaderReaderInputFile format;

	public ConsistancyCheckerTGA(Model model, IHeaderReaderInputFile format) throws ImageConverterException {
		// ruft über Super-Konstruktor auch allgemeine Check-Methoden der InputFiles auf
		super(model, format);
		this.format = format;

		checkDataSegmentSize();
		// TGA-spezifische Tests
		checkImageIDLength();
		// hier auch Kompression beinhaltet
		checkImageTypeCode();
		checkImageDescriptor();
	}

	@Override
	public void checkCompression() throws ImageConverterException {
		// Komprimiert: Typen 9,10,11,32,33; Unkomprimiert: 1,2,3 (keine Bilddaten: 0)
		if (!inputFormat.getCompressionType().equals(ECompressionType.UNCOMPRESSED)
		        && !inputFormat.getCompressionType().equals(ECompressionType.RLE)) {
			throw new ImageConverterException("Kompressionstyp wird nicht unterstützt");
		}
	}

	private void checkImageIDLength() throws ImageConverterException {
		if (((HeaderReaderTGAInputFile) inputFormat).getIdLength() != 0)
		    throw new ImageConverterException("idLength nicht 0");
	}

	private void checkImageTypeCode() throws ImageConverterException {
		if (!(((HeaderReaderTGAInputFile) inputFormat).getImageTypeCode() != 2)
		        && !(((HeaderReaderTGAInputFile) inputFormat).getImageTypeCode() != 10))
		    throw new ImageConverterException(
		            "kein zulässiger ImageTyp (d.h. nicht ImageType 2 (Unmapped RGB) oder ImageType 10 (Run length encoded, unmapped RGB ))");
	}

	private void checkImageDescriptor() throws ImageConverterException {
		if (((HeaderReaderTGAInputFile) inputFormat).getImageDescriptor() != 32)
		    throw new ImageConverterException("Nullpunkt nicht links oben");
	}

	/** Methode wird in FormatCheckerTGA übernommen, in FormatCheckerPropra
	 * überschrieben
	 * @throws ImageConverterException */
	public void checkDataSegmentSize() throws ImageConverterException {
		if (format.getCompressionType().equals(ECompressionType.UNCOMPRESSED) && (inputFormat
		        .getRealDataSegementSizeInFile() > ((HeaderReaderTGAInputFile) inputFormat).FileLengthWithFooter())) {
			throw new ImageConverterException("Zu wenig Bilddaten in Datei");
		} else if (inputFormat.getRealDataSegementSizeInFile() % 3 != 0) {
			throw new ImageConverterException("Zu wenig Bilddaten in Datei");
		}
	}
}
