package propra.imageconverter.reader.header;

import java.io.*;

import propra.imageconverter.*;
import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;
import propra.imageconverter.utilities.Utility;

/** Instanzen dieser Klasse entnehmen Datei-Informationen aus TGA-Eingabe-Datei.
 * Header-Elemente größer 1 Byte im LittleEndian.
 *
 * @author Martina Koch */
public class HeaderReaderTGAInputFile extends HeaderReaderInputFile implements IHeaderReaderInputFile {
	private byte idLength;
	/** hier ist Bildtyp und Kompression, z.B. Bildtyp 2 (unkomprimiert) und 10
	 * (RLE-encoded) verschlüsselt */
	private byte imageTypeCode;
	// ImageSpecification
	private int xOrigin;
	private int yOrigin;
	/** hier u.a. Lage Nullpunkt verschlüsselt */
	private byte imageDescriptor;
	private long realImageDataSegmentSizeTGA;
	private final int headerLengthTga = EFormat.TGA.getHeaderLength();

	public HeaderReaderTGAInputFile(Model model) throws ImageConverterException, IOException {
		super(model);
		this.headerInputFile = new byte[headerLengthTga];

		try {
			fileInputStream = new FileInputStream(file);

			extractInformationOutOfHeaderInputFile();
			assignCompressionType();
			assignRealSegmentSizeImageDataToModel();

			fileInputStream.close();
		} catch (FileNotFoundException e) {
			ImageConverterException.abruptlyExitProgram(e);
		} catch (IOException e) {
			ImageConverterException.abruptlyExitProgram(e);
		}
	}

	/** Liest aus Header der TGA-Eingabedatei die Bildinformationen aus. Hierbei
	 * sind alle Header-Elemente größer 1 Byte im LittleEndian-Format. */
	@Override
	public void extractInformationOutOfHeaderInputFile() throws ImageConverterException, IOException {
		fileInputStream.read(headerInputFile);

		idLength = headerInputFile[0];
		imageTypeCode = headerInputFile[2];

		xOrigin = Utility.getInt(headerInputFile[8], headerInputFile[9]);
		yOrigin = Utility.getInt(headerInputFile[10], headerInputFile[11]);

		imageWidth = Utility.getInt(headerInputFile[12], headerInputFile[13]);
		imageHeight = Utility.getInt(headerInputFile[14], headerInputFile[15]);

		pixelDepth = headerInputFile[16];
		imageDescriptor = headerInputFile[17];
	}

	private void assignCompressionType() throws ImageConverterException {
		switch (imageTypeCode) {
		case 2:
			compressionType = ECompressionType.UNCOMPRESSED;
			break;
		case 10:
			compressionType = ECompressionType.RLE;
			break;
		default:
			throw new ImageConverterException("Kompressionstyp nicht zulässig");
		}
	}

	/** Berechnet tatsächlich vorhandene Bilddatensegmentlänge. Diese ist bei einem
	 * unkomprimierten File Höhe x Breite x 3 Bytes und bei einem komprimierten File
	 * entspricht diese der Dateilänge ohne Header. */
	private void assignRealSegmentSizeImageDataToModel() {
		// ggf. vorhandener Dateifuß wird verworfen
		if (compressionType.equals(ECompressionType.UNCOMPRESSED)) {
			model.setRealImageDataSegmentInputFile(imageWidth * imageHeight * 3);
		} else {
			model.setRealImageDataSegmentInputFile(file.length() - headerLengthTga);
		}
	}

	/** Gibt abhängig von Kompression tatsächlich zu lesende Bilddatenmenge
	 * zurück. */
	@Override
	public long getRealDataSegementSizeInFile() {
		return realImageDataSegmentSizeTGA;
	}

	public byte getIdLength() {
		return idLength;
	}

	public byte getImageDescriptor() {
		return imageDescriptor;
	}

	public byte getImageTypeCode() {
		return imageTypeCode;
	}

	public long FileLengthWithFooter() {
		return file.length() - headerInputFile.length;
	}

	@Override
	public byte getPixelDepth() {
		return pixelDepth;
	}
}
