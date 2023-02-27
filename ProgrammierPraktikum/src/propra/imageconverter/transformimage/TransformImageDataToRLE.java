package propra.imageconverter.transformimage;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import propra.imageconverter.Model;
import propra.imageconverter.checksum.CheckSum;
import propra.imageconverter.enums.EFormat;

/** Instanz dieser Klasse transformiert die Bilddaten zu RLE. <br>
 * Für eine höhere Verarbeitungsgeschwindigkeit wird das Bild nur einmal *
 * eingelesen und ausgegeben und hierbei die Checksumme - wenn nötig - berechnet
 * und ggf. die Pixeldaten gedreht.
 *
 * @author Martina Koch */
public class TransformImageDataToRLE {
	private Model model;
	private byte[] RGBOrderInput;
	private byte[] RGBOrderOutput;
	private CheckSum checkSumInput;
	private CheckSum checkSumOutput;
	private long realDataSegmentSizeInFileToRead;
	private long pixelToRead;
	private long pixelToCompressInOutputFile;

	private BufferedOutputStream bufferedOutputStream;
	private BufferedInputStream bufferedInputStream;
	private RandomAccessFile randomAccessFileInput;
	private RandomAccessFile randomAccessFileOutput;

	/** Normaler Konstruktor, wenn kein Huffman-Bild zu RLE transformiert werden
	 * soll. */
	public TransformImageDataToRLE(Model model) throws IOException {
		this.model = model;

		randomAccessFileInput = new RandomAccessFile(model.getInputFilePath(), "r");
		randomAccessFileOutput = new RandomAccessFile(model.getOutputFilePath(), "rw");
		bufferedInputStream = new BufferedInputStream(new FileInputStream(randomAccessFileInput.getFD()));
		bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(randomAccessFileOutput.getFD()));

		randomAccessFileOutput.seek(model.getOutputFormat().getHeaderLength());
		randomAccessFileInput.seek(model.getInputFormat().getHeaderLength());

		realDataSegmentSizeInFileToRead = model.getRealDataSegmentSizeInputFile();
		pixelToRead = realDataSegmentSizeInFileToRead / 3;

		initializeCheckSumInputOutputFile();
		assignInputOutputRGBOrder();
		chooseImageDataSegementTransformation();

		bufferedInputStream.close();
		bufferedOutputStream.close();
	}

	/** Konstruktor, um aus unkomprimierten Daten einer bereits im ersten Schritt
	 * dekodierten Huffman-Eingabedatei eine RLE Komprimierung zu erstellen. */
	public TransformImageDataToRLE(Model model, RandomAccessFile randomAccessInputFile) throws IOException {
		this.model = model;
		// nutze bereits geschriebene OutputDaten als InputFile
		this.randomAccessFileInput = randomAccessInputFile;
		bufferedInputStream = new BufferedInputStream(new FileInputStream(randomAccessInputFile.getFD()));

		// erstelle temporäres File, in dem Bilddaten zu RLE komprimiert werden
		File temporaryOutputFile = File.createTempFile("imageConverterTempFile", null);
		model.setTemporaryFile(temporaryOutputFile);
		randomAccessFileOutput = new RandomAccessFile(temporaryOutputFile, "rw");
		bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(randomAccessFileOutput.getFD()));

		/* da randomAccessInputFile bereits in Output-Zielformat mit Platz für Header
		 * geschrieben ist, muss Header-Länge des Output-Formates abgezogen werden */
		realDataSegmentSizeInFileToRead = randomAccessInputFile.length() - model.getOutputFormat().getHeaderLength();
		pixelToRead = realDataSegmentSizeInFileToRead / 3;

		// Checksumme InputFile wurde bereits bei Dekodierung Huffman-Datei berechnet
		checkSumInput = new CheckSum(-1);
		checkSumOutput = model.getOutputFormat().equals(EFormat.PROPRA) ? new CheckSum(0) : new CheckSum(-1);

		RGBOrderInput = model.getOutputRGBOrder();
		RGBOrderOutput = model.getOutputRGBOrder();

		randomAccessFileInput.seek(model.getOutputFormat().getHeaderLength());
		randomAccessFileOutput.seek(0L);
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

	private void chooseImageDataSegementTransformation() throws IOException {
		switch (model.getInputCompressionType()) {
		case UNCOMPRESSED:
			convertUncompressedToRle();
			break;
		case RLE:
			convertRleToRle();
			break;
		case HUFFMAN:
			break;
		default:
			break;
		}
	}

	public void convertRleToRle() throws IOException {
		long bytesToRead = realDataSegmentSizeInFileToRead;
		byte[] rbgOutOfInputFile = new byte[3];
		byte packetHeader;
		byte[] turnedRGB = new byte[3];

		while (bytesToRead >= 1) {
			// lese Paket-Header
			packetHeader = randomAccessFileInput.readByte();
			checkSumInput.calculateCheckSumForByteArray(ByteBuffer.allocate(1).put(packetHeader).array());
			bytesToRead--;

			/* bestimme Pakettyp: Raw (0), RLE(1), maskieren mit 0x1, Bitschieben mit ">>>"
			 * um 7 Stellen */
			int bitID = ((packetHeader >>> 7) & 0x1);
			/* über logische UND-Verknüpfung mit 0x7f, d.h. 01111 1111 Anzahl Wdh. RLE-Paket
			 * RGB-Werte bzw. Anzahl Raw-Paket unterschiedliche RGB-Werte ermitteln */
			short repetitions = (short) ((packetHeader & 0x7f));

			// IDbyte: RLE-Packet
			if (bitID == 1) {
				// Header + 3 Bytes für RGB
				byte[] paketArray = new byte[1 + 3];
				paketArray[0] = packetHeader;

				// liest drei Folgebytes für RGB in rgb-Array
				randomAccessFileInput.read(rbgOutOfInputFile);
				checkSumInput.calculateCheckSumForByteArray(rbgOutOfInputFile);
				bytesToRead -= 3;
				turnedRGB = turnToOutputRGBOrder(rbgOutOfInputFile);

				// Schreibe Header plus erstes RGB Information in PaketArray
				for (int i = 0; i < turnedRGB.length; i++) {
					paketArray[i + 1] = turnedRGB[i];
				}

				randomAccessFileOutput.write(paketArray);
				checkSumOutput.calculateCheckSumForByteArray(paketArray);
			}

			// IDbyte: Raw-Paket
			else {
				// Header und zu schreibende RGB-Werte
				byte[] paketArray = new byte[1 + (repetitions + 1) * 3];
				paketArray[0] = packetHeader;

				for (int j = 0; j < (repetitions + 1); j++) {
					// jeweils drei Folgebytes für RGB in paketArray
					randomAccessFileInput.read(rbgOutOfInputFile);
					checkSumInput.calculateCheckSumForByteArray(rbgOutOfInputFile);
					bytesToRead -= 3;
					// drehen RGB-Reihenfolge
					turnedRGB = turnToOutputRGBOrder(rbgOutOfInputFile);

					paketArray[j * 3 + 1] = turnedRGB[0];
					paketArray[j * 3 + 2] = turnedRGB[1];
					paketArray[j * 3 + 3] = turnedRGB[2];
				}
				randomAccessFileOutput.write(paketArray);
				checkSumOutput.calculateCheckSumForByteArray(paketArray);
			}
		}
		model.setCheckSumInputFile(checkSumInput.finallyCalculateChecksum());
		model.setCheckSumOutputFile(checkSumOutput.finallyCalculateChecksum());
	}

	public void convertUncompressedToRle() throws IOException {
		System.out.println("convert uncompressed to rle");
		// Array mit Länge drei für RGB-Repräsentation eines Pixels im Bild
		byte[] pixelToCompareTo = new byte[3];
		byte[] nextPixel = new byte[3];
		/* 1 Byte Header, 3 Byte für Pixel, maximal 128 gleiche Pixel im Paket, (0x80
		 * hex für 1000 0000 bin) */
		byte[] rlePaket = new byte[4];
		// maximal 127+1 Pixel
		byte rlePaketRepetitionCounter = 0;
		/* maximale Größe: 1 Byte Header, 128*3 Bytes für nachfolgende, unterschiedliche
		 * Pixel */
		byte[] rawPaket = new byte[(128 * 3) + 1];
		// zählt nachfolgende unterschiedliche Pixel-RGB-Werte, maximal 127 +1
		int rawPaketPixelCounter = 0;
		byte[] actualReadByteFromInputStream = new byte[3];
		this.pixelToCompressInOutputFile = realDataSegmentSizeInFileToRead / 3;
		this.pixelToRead = realDataSegmentSizeInFileToRead / 3;

		// Lese erstes Pixel vor while-Schleife
		actualReadByteFromInputStream = readNextPixelFromInputStream();
		checkSumInput.calculateCheckSumForByteArray(actualReadByteFromInputStream);
		pixelToCompareTo = turnToOutputRGBOrder(actualReadByteFromInputStream);

		while (pixelToCompressInOutputFile > 0) {
			rlePaketRepetitionCounter = 0;
			rawPaketPixelCounter = 0;

			// I. Behandlung Sonderfälle
			/* nur noch ein Pixel zu schreiben (entweder Datei mit nur einem Pixel oder am
			 * Ende) */
			if (pixelToRead == 0) {
				if (pixelToCompressInOutputFile == 1) {
					writeToRawPaketArray(rawPaket, pixelToCompareTo, rawPaketPixelCounter);
					rawPaketPixelCounter++;
					pixelToCompressInOutputFile--;
					// Schreibe Raw-Paket
					rawPaket[0] = (byte) (rawPaketPixelCounter - 1);
					randomAccessFileOutput.write(rawPaket, 0, rawPaketPixelCounter * 3 + 1);
					// pixelToCompressInOutputFile -= rawPaketPixelCounter + 1;
					checkSumOutput.calculateCheckSumForByteArray(
					        Arrays.copyOfRange(rawPaket, 0, rawPaketPixelCounter * 3 + 1));
				}

				/* d.h. noch zwei unterschiedliche Pixel zu schreiben */
				else if (pixelToCompressInOutputFile == 2) {
					if (!Arrays.equals(pixelToCompareTo, nextPixel)) { /* Raw-Paket */
						writeToRawPaketArray(rawPaket, pixelToCompareTo, rawPaketPixelCounter);
						rawPaketPixelCounter++;
						writeToRawPaketArray(rawPaket, nextPixel, rawPaketPixelCounter);
						rawPaketPixelCounter++;
						pixelToCompressInOutputFile -= 2;
						// Schreibe Raw-Paket
						rawPaket[0] = (byte) (rawPaketPixelCounter - 1);
						randomAccessFileOutput.write(rawPaket, 0, rawPaketPixelCounter * 3 + 1);
						// pixelToCompressInOutputFile -= rawPaketPixelCounter + 1;
						checkSumOutput.calculateCheckSumForByteArray(
						        Arrays.copyOfRange(rawPaket, 0, rawPaketPixelCounter * 3 + 1));
					} else { /* RLE-Paket */
						rlePaket[0] = (byte) 0x81; /* für 1000 0001 */
						System.arraycopy(pixelToCompareTo, 0, rlePaket, 1, 3);
						writeRLEPaketArrayToOutputFile(rlePaket);
						checkSumOutput.calculateCheckSumForByteArray(rlePaket);
					}
				}
			}

			// II. Normaler Ablauf
			// mindestens ein Pixel noch in Datei zu lesen
			else if (pixelToRead >= 1) {
				if (!(pixelToCompressInOutputFile - 2 == pixelToRead)) {
					// lese zweites, nachfolgendes Pixel und drehe in richtige Reihenfolge
					actualReadByteFromInputStream = readNextPixelFromInputStream();
					checkSumInput.calculateCheckSumForByteArray(actualReadByteFromInputStream);
					nextPixel = turnToOutputRGBOrder(actualReadByteFromInputStream);
					/* jetzt sind zwei Pixel pixelToCompareTo und nextPixel eingelesen, die
					 * verglichen werden */
				}

				// RLE-Paket startet
				/* FALL 1: Pixel pixelToCompareTo und nextPixel sind gleich, also Anfang eines
				 * Rle-Paketes */
				if (Arrays.equals(pixelToCompareTo, nextPixel)) {
					System.arraycopy(pixelToCompareTo, 0, rlePaket, 1, 3);
					while (Arrays.equals(pixelToCompareTo, nextPixel) && (pixelToRead > 0)
					        && rlePaketRepetitionCounter <= 126) {
						/* erhöhe Wdh. Counter, da ein weiteres Pixel mit erstem übereinstimmt, erste
						 * Wiederholung wird mit 1 gezählt, letzte maximal 127 */
						// erster zu Wiederholender Pixel hat Counter Wert 1
						rlePaketRepetitionCounter++;

						// lese nachfolgenden Pixel
						actualReadByteFromInputStream = readNextPixelFromInputStream();
						checkSumInput.calculateCheckSumForByteArray(actualReadByteFromInputStream);
						nextPixel = turnToOutputRGBOrder(actualReadByteFromInputStream);
					}

					/* Wenn Datei am Ende muss noch PixelNext geschrieben werden, dieses wird in
					 * vorhandenes Paket angehängt, wenn pixelNext gleich PixelToCompareTo und
					 * counter <127 <br> */
					if (pixelToRead == 0 && Arrays.equals(pixelToCompareTo, nextPixel)) {
						if (rlePaketRepetitionCounter <= 126) {
							rlePaketRepetitionCounter++;
						}
					}

					// schreibe RLE-Paket in OutputStream
					rlePaket[0] = (byte) (128 + rlePaketRepetitionCounter);
					writeRLEPaketArrayToOutputFile(rlePaket);
					checkSumOutput.calculateCheckSumForByteArray(rlePaket);

					/* wenn Datei noch nicht am Ende, ist pixelNext noch nicht geschrieben und noch
					 * mind. 1 Pixel zusätzlich zu lesen */
					pixelToCompareTo = nextPixel;
				}

				// RAW-Paket startet
				/* FALL 2: pixelToCompareTo und nextPixel sind Anfang eines Raw-Paketes */
				else {
					rawPaket[0] = 0;
					while ((!Arrays.equals(pixelToCompareTo, nextPixel)) && (rawPaketPixelCounter <= 127)
					        && (pixelToRead > 0)) {
						/* schreibt ersten und weitere RGB-Werte in Array nach Header, letzter
						 * unterschiedlicher Wert wird am Ende ggf. nicht geschrieben */
						writeToRawPaketArray(rawPaket, pixelToCompareTo, rawPaketPixelCounter);
						pixelToCompressInOutputFile--;
						// zähle ersten Wert (rbgBytesToCompareTo) mit 1
						rawPaketPixelCounter++;

						pixelToCompareTo = nextPixel;
						actualReadByteFromInputStream = readNextPixelFromInputStream();
						checkSumInput.calculateCheckSumForByteArray(actualReadByteFromInputStream);
						// lese nächste drei RGB-Werte
						nextPixel = turnToOutputRGBOrder(actualReadByteFromInputStream);
					}

					// hänge letzten beide Pixel noch in Raw-Paket, wenn noch nicht voll
					if (pixelToRead == 0 && !Arrays.equals(pixelToCompareTo, nextPixel)) {
						if (rawPaketPixelCounter <= 126) {
							writeToRawPaketArray(rawPaket, pixelToCompareTo, rawPaketPixelCounter);
							rawPaketPixelCounter++;
							writeToRawPaketArray(rawPaket, nextPixel, rawPaketPixelCounter);
							rawPaketPixelCounter++;
							pixelToCompressInOutputFile -= 2;
						}
					}

					// Schreibe Raw-Paket
					rawPaket[0] = (byte) (rawPaketPixelCounter - 1);
					randomAccessFileOutput.write(rawPaket, 0, rawPaketPixelCounter * 3 + 1);
					checkSumOutput.calculateCheckSumForByteArray(
					        Arrays.copyOfRange(rawPaket, 0, rawPaketPixelCounter * 3 + 1));
				}
			}
		}
		model.setCheckSumInputFile(checkSumInput.finallyCalculateChecksum());
		model.setCheckSumOutputFile(checkSumOutput.finallyCalculateChecksum());
	}

	/** Dreht RGB-Bytes entsprechend der Vorgaben der Input-/Output-Formate.
	 *
	 * @param rbgArray
	 * @return */
	byte[] turnToOutputRGBOrder(byte[] rbgArray) {
		if (!RGBOrderInput.equals(RGBOrderOutput)) {
			byte[] turnedRGB = new byte[3];
			for (int i = 0; i < 3; i++) {
				turnedRGB[RGBOrderOutput[i]] = rbgArray[RGBOrderInput[i]];
			}
			return turnedRGB;
		} else {
			return rbgArray;
		}
	}

	private void writeRLEPaketArrayToOutputFile(byte[] rlePaket) throws IOException {
		randomAccessFileOutput.write(rlePaket);
		pixelToCompressInOutputFile -= (rlePaket[0] & 0x7f) + 1;
	}

	private byte[] readNextPixelFromInputStream() throws IOException {
		byte[] readByteFromInputStream = new byte[3];
		bufferedInputStream.read(readByteFromInputStream);
		pixelToRead--;
		return readByteFromInputStream;
	}

	private void writeToRawPaketArray(byte[] arrayToWriteTo, byte[] array, int offset) {
		arrayToWriteTo[offset * 3 + 1] = array[0];
		arrayToWriteTo[offset * 3 + 2] = array[1];
		arrayToWriteTo[offset * 3 + 3] = array[2];
	}
}
