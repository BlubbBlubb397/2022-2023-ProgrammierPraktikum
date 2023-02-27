package propra.imageconverter.reader.header;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import propra.imageconverter.*;
import propra.imageconverter.consistancy.ConsistancyCheckerProPra;
import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;
import propra.imageconverter.utilities.Utility;

/** Instanzen dieser Klasse extrahieren Header-Informationen aus
 * ProPra-Eingabe-Datei. Header-Elemente größer 1 Byte im LittleEndian.
 *
 * @author Martina Koch */
public class HeaderReaderProPraInputFile extends HeaderReaderInputFile implements IHeaderReaderInputFile {
	private BufferedInputStream bufferedInputStream;
	private RandomAccessFile randomAccessInputFile;
	private final int headerLengthPropra = EFormat.PROPRA.getHeaderLength();
	private byte[] formatCode = new byte[12];
	private long dataSegmentSizeInHeader;
	private long checkSumAusHeader;
	/** Kompressionstyp (uncompressed: 0, rle: 1, huffman: 2) */
	private byte compressionInHeader;
	private long segmentSizeImageDataInFilePropra;
	private long dataSegmentSizeInFile;

	public HeaderReaderProPraInputFile(Model model) throws ImageConverterException, IOException {
		super(model);
		headerInputFile = new byte[headerLengthPropra];

		try {
			randomAccessInputFile = new RandomAccessFile(file, "r");
			fileInputStream = new FileInputStream(randomAccessInputFile.getFD());
			bufferedInputStream = new BufferedInputStream(fileInputStream);

			extractInformationOutOfHeaderInputFile();

			this.dataSegmentSizeInFile = file.length() - headerInputFile.length;

			assignCompression();
			assignSegmentSizeImageDataInFile();

			checkConsistancyDataSegmentSizeIfUncompressedInputFile();

			bufferedInputStream.close();
		} catch (FileNotFoundException e) {
			ImageConverterException.abruptlyExitProgram(e);
		} catch (IOException e) {
			ImageConverterException.abruptlyExitProgram(e);
		}
	}

	@Override
	public void extractInformationOutOfHeaderInputFile() throws ImageConverterException, IOException {
		// liest HeaderBytes aus InputFile ein (Propra 18 Bytes)
		randomAccessInputFile.read(headerInputFile);
		// ProPra-Kennung auslesen
		formatCode = Arrays.copyOfRange(headerInputFile, 0, 12);

		compressionInHeader = headerInputFile[12];

		imageWidth = Utility.getInt(headerInputFile[13], headerInputFile[14]);
		imageHeight = Utility.getInt(headerInputFile[15], headerInputFile[16]);

		pixelDepth = headerInputFile[17];
		this.dataSegmentSizeInHeader = ByteBuffer.wrap(headerInputFile, 18, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();

		byte[] checkSumOutOfHeaderArray = { headerInputFile[26], headerInputFile[27], headerInputFile[28],
		        headerInputFile[29], 0, 0, 0, 0 };
		checkSumAusHeader = ByteBuffer.wrap(checkSumOutOfHeaderArray).order(ByteOrder.LITTLE_ENDIAN).getLong();
	}

	private void assignCompression() throws ImageConverterException {
		switch (compressionInHeader) {
		case 0:
			compressionType = ECompressionType.UNCOMPRESSED;
			break;
		case 1:
			compressionType = ECompressionType.RLE;
			break;
		case 2:
			compressionType = ECompressionType.HUFFMAN;
			break;
		default:
			throw new ImageConverterException("Kompressionstyp nicht zulässig");
		}
	}

	/** Legt Bilddaten-Segmentgröße fest. */
	private void assignSegmentSizeImageDataInFile() {
		if (compressionType.equals(ECompressionType.UNCOMPRESSED) || compressionType.equals(ECompressionType.HUFFMAN)) {
			segmentSizeImageDataInFilePropra = (imageWidth * imageHeight) * 3;
			model.setRealImageDataSegmentInputFile(segmentSizeImageDataInFilePropra);
		}
		if (compressionType.equals(ECompressionType.RLE)) {
			segmentSizeImageDataInFilePropra = file.length() - headerLengthPropra;
			model.setRealImageDataSegmentInputFile(segmentSizeImageDataInFilePropra);
		}
	}

	private void checkConsistancyDataSegmentSizeIfUncompressedInputFile() throws ImageConverterException {
		if (compressionType.equals(ECompressionType.UNCOMPRESSED)) {
			ConsistancyCheckerProPra.checkDataSegmentSize(dataSegmentSizeInHeader, dataSegmentSizeInFile);
		}
	}

	public long getHeaderDataSegmentSize() {
		return dataSegmentSizeInHeader;
	}

	public byte[] getFormatCode() {
		return formatCode;
	}

	public long getFileLength() {
		return file.length();
	}

	public long getCheckSumAusHeader() {
		return checkSumAusHeader;
	}

	@Override
	public long getRealDataSegementSizeInFile() {
		return segmentSizeImageDataInFilePropra;
	}

	@Override
	public byte getPixelDepth() {
		return pixelDepth;
	}
}
