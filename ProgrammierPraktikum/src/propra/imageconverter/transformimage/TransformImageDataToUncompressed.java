package propra.imageconverter.transformimage;

import java.io.*;
import java.nio.ByteBuffer;

import propra.imageconverter.Model;
import propra.imageconverter.checksum.CheckSum;
import propra.imageconverter.enums.EFormat;

/** Instanz dieser Klasse transformiert die Bilddaten zu uncompressed. <br>
 * Für eine höhere Verarbeitungsgeschwindigkeit wird das Bild nur einmal
 * eingelesen und ausgegeben und hierbei die Checksumme - wenn nötig - berechnet
 * und ggf. die Pixeldaten gedreht.
 *
 * @author Martina Koch */
public class TransformImageDataToUncompressed {
	private Model model;
	private byte[] RGBOrderInput;
	private byte[] RGBOrderOutput;
	private long realDataSegmentSizeInFileToRead;

	private BufferedOutputStream bufferedOutputStream;
	private BufferedInputStream bufferedInputStream;
	private RandomAccessFile randomAccessFileInput;
	private RandomAccessFile randomAccessFileOutput;

	private CheckSum checkSumInput;
	private CheckSum checkSumOutput;

	public TransformImageDataToUncompressed(Model model) throws IOException {
		this.model = model;

		randomAccessFileInput = new RandomAccessFile(model.getInputFilePath(), "r");
		randomAccessFileOutput = new RandomAccessFile(model.getOutputFilePath(), "rw");
		bufferedInputStream = new BufferedInputStream(new FileInputStream(randomAccessFileInput.getFD()));
		bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(randomAccessFileOutput.getFD()));

		randomAccessFileOutput.seek(model.getOutputFormat().getHeaderLength());
		randomAccessFileInput.seek(model.getInputFormat().getHeaderLength());

		realDataSegmentSizeInFileToRead = model.getRealDataSegmentSizeInputFile();

		initializeCheckSumInputOutputFile();
		assignInputOutputRGBOrder();
		chooseImageDataSegementTransformation();

		bufferedInputStream.close();
		bufferedOutputStream.close();
	}

	private void chooseImageDataSegementTransformation() throws IOException {
		switch (model.getInputCompressionType()) {
		case UNCOMPRESSED:
			convertUncompressedToUncompressed();
			break;
		case RLE:
			convertRleToUncompressed();
			break;
		case HUFFMAN:
			break;
		default:
			break;
		}
	}

	private void assignInputOutputRGBOrder() {
		RGBOrderInput = model.getInputRGBOrder();
		RGBOrderOutput = model.getOutputRGBOrder();
	}

	/** Intitalisiere CheckSumInput/Output mit 0, wenn Propra-Datei
	 * eingelesen/ausgegeben wird, sonst -1; */
	private void initializeCheckSumInputOutputFile() {
		checkSumInput = model.getInputFormat().equals(EFormat.PROPRA) ? new CheckSum(0) : new CheckSum(-1);
		checkSumOutput = model.getOutputFormat().equals(EFormat.PROPRA) ? new CheckSum(0) : new CheckSum(-1);
	}

	/** Konvertiert unkomprimierte zu unkomprimierter Datei.
	 * @param model */

	public void convertUncompressedToUncompressed() throws IOException {
		byte[] bytesRead;

		CheckSum checkSumInputFile = new CheckSum(0);
		CheckSum checkSumOuptputFile = new CheckSum(0);
		randomAccessFileInput.seek(model.getInputFormat().getHeaderLength());
		randomAccessFileOutput.seek(model.getOutputFormat().getHeaderLength());

		while (realDataSegmentSizeInFileToRead > 0) {
			bytesRead = readBytePackagesFromInputStream();
			checkSumInputFile.calculateCheckSumForByteArray(bytesRead);

			byte[] bytesTurned = turnToOutputRGBOrder(bytesRead);
			checkSumOuptputFile.calculateCheckSumForByteArray(bytesTurned);

			bufferedOutputStream.write(bytesTurned);

			realDataSegmentSizeInFileToRead -= bytesRead.length;
		}

		model.setCheckSumInputFile(checkSumInputFile.finallyCalculateChecksum());
		model.setCheckSumOutputFile(checkSumOuptputFile.finallyCalculateChecksum());
	}

	/** Konvertiert RLE-komprimierte Datei zu unkomprimiert */
	public void convertRleToUncompressed() throws IOException {
		long bytesToRead = realDataSegmentSizeInFileToRead;
		int packetHeader;
		while (bytesToRead >= 3) {
			// lese Paket-Header
			packetHeader = bufferedInputStream.read();
			checkSumInput.calculateCheckSumForByteArray(ByteBuffer.allocate(1).put((byte) packetHeader).array());
			bytesToRead--;
			// bestimme Pakettyp: Raw (0), RLE(1)
			byte bitID = (byte) (packetHeader >>> 7);
			/* über logische UND-Verknüpfung mit 0x7f, d.h. 01111 1111 Anzahl Wdh. RLE-Paket
			 * RGB-Werte bzw. Anzahl Raw-Paket unterschiedliche RGB-Werte ermitteln */
			short repetitions = (short) ((packetHeader & 0x7f));
			byte[] rbgOutOfInputFile;

			// IDbyte: RLE-Packet
			if (bitID == 1 && bytesToRead >= 3) {
				/* erstelle entsprechend großes Array für zu schreibende RGB-Werte der RLE- bzw.
				 * Raw-Pakete */
				byte[] rbgRepetitionArray = new byte[(repetitions + 1) * 3];
				// liest drei Folgebytes für RGB in rgb-Array
				rbgOutOfInputFile = bufferedInputStream.readNBytes(3);
				checkSumInput.calculateCheckSumForByteArray(rbgOutOfInputFile);
				bytesToRead -= 3;

				byte[] turnedRGB = turnToOutputRGBOrder(rbgOutOfInputFile);
				// Anzahl Wdh. im Header plus erstes RGB Information
				for (int j = 0; j < repetitions + 1; j++) {
					writeRGBPaketInformationToArray(rbgRepetitionArray, turnedRGB, j);
				}

				bufferedOutputStream.write(rbgRepetitionArray);
				checkSumOutput.calculateCheckSumForByteArray(rbgRepetitionArray);
			}

			// IDbyte: Raw-Paket
			else {
				// erstelle Array für zu schreibende RGB-Werte Raw-Paket
				byte[] rbgRepetitionArray = new byte[(repetitions + 1) * 3];

				// liest für jede repetition jeweils drei Folgebytes für RGB in rgb-Array
				for (int j = 0; j < (repetitions + 1); j++) {
					rbgOutOfInputFile = bufferedInputStream.readNBytes(3);
					bytesToRead -= 3;
					checkSumInput.calculateCheckSumForByteArray(rbgOutOfInputFile);

					byte[] turnedRGB = turnToOutputRGBOrder(rbgOutOfInputFile);
					for (int l = j; l <= j; l++) {
						writeRGBPaketInformationToArray(rbgRepetitionArray, turnedRGB, l);
					}
				}
				bufferedOutputStream.write(rbgRepetitionArray);
				checkSumOutput.calculateCheckSumForByteArray(rbgRepetitionArray);
			}
		}
		model.setCheckSumInputFile(checkSumInput.finallyCalculateChecksum());
		model.setCheckSumOutputFile(checkSumOutput.finallyCalculateChecksum());
	}

	/** Dreht Reihenfolge der RGB-Bytes entsprechend der Vorgaben der
	 * Input-/Output-Formate.
	 *
	 * @param rbgArray
	 * @return */
	private byte[] turnToOutputRGBOrder(byte[] arrayToBeTurned) {
		byte[] turnedRGB = new byte[arrayToBeTurned.length];
		if (!model.getInputRGBOrder().equals(model.getOutputRGBOrder())) {
			for (int l = 0; l < arrayToBeTurned.length; l += 3) {
				for (int i = 0; i < 3; i++) {
					turnedRGB[RGBOrderOutput[i] + l] = arrayToBeTurned[RGBOrderInput[i] + l];
				}
			}
		}
		return turnedRGB;
	}

	private void writeRGBPaketInformationToArray(byte[] rbgRepetitionArray, byte[] turnedRGB, int l) {
		rbgRepetitionArray[l * 3 + 0] = turnedRGB[0];
		rbgRepetitionArray[l * 3 + 1] = turnedRGB[1];
		rbgRepetitionArray[l * 3 + 2] = turnedRGB[2];
	}

	private byte[] readBytePackagesFromInputStream() throws IOException {
		int bytesToRead = (realDataSegmentSizeInFileToRead > 1024 * 3) ? (1024 * 3)
		        : (int) realDataSegmentSizeInFileToRead;
		byte[] bytesReadToArray = new byte[bytesToRead];
		bufferedInputStream.read(bytesReadToArray);

		return bytesReadToArray;
	}
}
