package propra.imageconverter.headercomposer;

import java.io.IOException;
import java.util.Arrays;

import propra.imageconverter.*;
import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;
import propra.imageconverter.utilities.Utility;

/** Instanzen erstellen die Header-Infomformationen für eine TGA Ausgabedatei.
 * Alle Informationen größer als 1 Byte in LittleEndian.
 *
 * @author Martina Koch */
public class HeaderComposerForTGAOutputFile implements IHeaderComposerOutputFile {
	private IHeaderReaderInputFile inputFormatHeaderReader;
	private byte[] headerOutputFileTGA = new byte[EFormat.TGA.getHeaderLength()];
	private Model model;

	public HeaderComposerForTGAOutputFile(Model model, IHeaderReaderInputFile inputFormatHeaderReader)
	        throws ImageConverterException, IOException {
		this.inputFormatHeaderReader = inputFormatHeaderReader;
		this.model = model;
		composeHeaderInformationForOutputFile();
	}

	/** Stellt Header-Informationen für Output-File zusammen. */
	@Override
	public void composeHeaderInformationForOutputFile() {
		// IDlenght=0 (1Byte)
		headerOutputFileTGA[0] = 0;
		// ColorMap=0 (1Byte)
		headerOutputFileTGA[1] = 0;
		// schreibe Output-ImageType und somit Output-Kompression in Header
		headerOutputFileTGA[2] = (byte) (model.getOutputCompressionType().equals(ECompressionType.UNCOMPRESSED) ? 2
		        : 10);
		// ColorMapSpecification=0 (5Byte)
		Arrays.fill(headerOutputFileTGA, 3, 6, (byte) 0);

		// schreibe ImageSpecification (10Bytes)
		// 2 Bytes xOriginArray
		headerOutputFileTGA[8] = 0;
		headerOutputFileTGA[9] = 0;
		// yOrigin (2 Bytes), entspricht Height
		System.arraycopy(Utility.getTwoBytesOutOfInt(inputFormatHeaderReader.getHeight()), 0, headerOutputFileTGA, 10,
		        2);
		// Breite (2 Bytes)
		System.arraycopy(Utility.getTwoBytesOutOfInt(inputFormatHeaderReader.getWidth()), 0, headerOutputFileTGA, 12,
		        2);
		// Höhe (2 Bytes)
		System.arraycopy(Utility.getTwoBytesOutOfInt(inputFormatHeaderReader.getHeight()), 0, headerOutputFileTGA, 14,
		        2);
		// Farbtiefe (1 Byte)
		headerOutputFileTGA[16] = inputFormatHeaderReader.getPixelDepth();
		// d.h. Nullpunkt links oben , 0x20 hex
		headerOutputFileTGA[17] = 32;
	}

	@Override
	public byte[] getHeader() {
		return headerOutputFileTGA;
	}
}
