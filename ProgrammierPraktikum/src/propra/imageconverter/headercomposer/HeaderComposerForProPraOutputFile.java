package propra.imageconverter.headercomposer;

import java.io.IOException;

import propra.imageconverter.*;
import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;
import propra.imageconverter.utilities.Utility;

/** Instanzen erstellen die Header-Infomformationen für eine ProPra
 * Ausgabedatei. Alle Informationen größer als 1 Byte in LittleEndian.
 *
 * @author Martina Koch */
public class HeaderComposerForProPraOutputFile implements IHeaderComposerOutputFile {
	private IHeaderReaderInputFile inputFormatHeaderReader;
	private byte[] headerOutputFileProPra = new byte[EFormat.PROPRA.getHeaderLength()];
	// entspricht Propra-Formatkennung "ProPraWiSe22" (12 Bytes)
	private final byte[] FORMATKENNUNG = { 80, 114, 111, 80, 114, 97, 87, 105, 83, 101, 50, 50 };
	private Model model;

	public HeaderComposerForProPraOutputFile(Model model, IHeaderReaderInputFile inputFormatHeaderReader)
	        throws ImageConverterException, IOException {
		this.model = model;
		this.inputFormatHeaderReader = inputFormatHeaderReader;

		composeHeaderInformationForOutputFile();
	}

	/** Stellt Header-Informationen für Output-File zusammen. */
	@Override
	public void composeHeaderInformationForOutputFile() throws IOException {
		// Formatkennung
		System.arraycopy(FORMATKENNUNG, 0, headerOutputFileProPra, 0, FORMATKENNUNG.length);
		// Kompression
		headerOutputFileProPra[12] = (byte) ((model.getOutputCompressionType().equals(ECompressionType.UNCOMPRESSED))
		        ? 0
		        : 1);
		// Breite
		System.arraycopy(Utility.getTwoBytesOutOfInt(inputFormatHeaderReader.getWidth()), 0, headerOutputFileProPra, 13,
		        2);
		// Höhe
		System.arraycopy(Utility.getTwoBytesOutOfInt(inputFormatHeaderReader.getHeight()), 0, headerOutputFileProPra,
		        15, 2);
		// PixelDepth
		headerOutputFileProPra[17] = inputFormatHeaderReader.getPixelDepth();

		/* Byte 18-29 (Bilddatensegmentgröße und Checksumme) werden erst gesetzt, wenn
		 * Bilddatensegment geschrieben wurde */
	}

	/** Bilddatensegmentgröße wird nachträglich aus OutputFile bestimmt und in
	 * Header-Array geschrieben. */
	public void setImageDataSegmentSize(long writtenImageDataSegmentSizeOutput) {
		headerOutputFileProPra[18] = (byte) ((writtenImageDataSegmentSizeOutput >> 0) & 0xff);
		headerOutputFileProPra[19] = (byte) ((writtenImageDataSegmentSizeOutput >> 8) & 0xff);
		headerOutputFileProPra[20] = (byte) ((writtenImageDataSegmentSizeOutput >> 16) & 0xff);
		headerOutputFileProPra[21] = (byte) ((writtenImageDataSegmentSizeOutput >> 24) & 0xff);
		headerOutputFileProPra[22] = (byte) ((writtenImageDataSegmentSizeOutput >> 32) & 0xff);
		headerOutputFileProPra[23] = (byte) ((writtenImageDataSegmentSizeOutput >> 40) & 0xff);
		headerOutputFileProPra[24] = (byte) ((writtenImageDataSegmentSizeOutput >> 48) & 0xff);
		headerOutputFileProPra[25] = (byte) ((writtenImageDataSegmentSizeOutput >> 56) & 0xff);
	}

	/** Checksumme wird nach Schreiben der Bilddaten aus OutputFile bestimmt und in
	 * Header-Array nachgetragen. */
	public void setCheckSum() {
		long checkSumPropraOutputFile = model.getCheckSumOutputFile();
		headerOutputFileProPra[26] = (byte) ((checkSumPropraOutputFile >> 0) & 0xff);
		headerOutputFileProPra[27] = (byte) ((checkSumPropraOutputFile >> 8) & 0xff);
		headerOutputFileProPra[28] = (byte) ((checkSumPropraOutputFile >> 16) & 0xff);
		headerOutputFileProPra[29] = (byte) ((checkSumPropraOutputFile >> 24) & 0xff);
	}

	@Override
	public byte[] getHeader() {
		return headerOutputFileProPra;
	}
}
