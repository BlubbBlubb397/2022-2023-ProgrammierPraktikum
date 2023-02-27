package propra.imageconverter.transportcoding;

import java.io.*;

import propra.imageconverter.ImageConverterException;
import propra.imageconverter.Model;

/** Instanzen dieser Klasse de-/enkodieren zu Base-32-hex.
 *
 * @author Martina Koch */
public class EncodeDecodeBase32 {
	private BufferedInputStream bufferedInputStream;
	private BufferedOutputStream bufferedOutputStream;
	private final String base32HexAlphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUV";

	/** Instanz dieser Klasse De-/Enkodiert zu Base-32-hex. */
	public EncodeDecodeBase32(Model model) throws ImageConverterException, IOException {
		try {
			File inputFile = new File(model.getInputFilePath());
			FileInputStream fileInputStream = new FileInputStream(inputFile);

			File outputFile = new File(model.getOutputFilePath());
			bufferedInputStream = new BufferedInputStream(fileInputStream);
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

			boolean encode = model.getEncodeBase32();
			boolean decode = model.getDecodeBase32();

			if (encode) {
				System.out.println("Enkodiere Datei...");
				encode();
			}
			if (decode) {
				System.out.println("Dekodiere Datei...");
				decode();
			}
			System.out.println("...erfolgreich abgeschlossen!");

			bufferedOutputStream.close();
			bufferedInputStream.close();
		} catch (FileNotFoundException e) {
			ImageConverterException.abruptlyExitProgram(e);
		}
	}

	/** Entschlüsseln der Datei aus Base-32-hex Alphabet in binäre Darstellung */
	private void decode() throws ImageConverterException, IOException {
		int inputRead;
		// Binäre Darstellung der zu schreibenden Daten
		int countEightBits = 0;
		int outputToWrite = 0;
		// Potenz 2^bitToPow
		int bitToPow;

		inputRead = bufferedInputStream.read();

		while (inputRead != -1) {
			// ermittle Buchstabe an Position inputRead
			inputRead = base32HexAlphabet.indexOf(inputRead);

			// lese jeweils 5 Bit, d.h. ein hex-32-Alphabet-Buchstaben
			for (int i = 0; i < 5; i++) {
				/* ">>>": schiebe 4 -i Nullen von links, maskiere zu lesendes erstes Bit mit
				 * logischem UND, gelesenes Bit 0 oder 1, 0x1 (hex) = 0000 0001(binär) */
				bitToPow = (inputRead >>> 4 - i) & 0x1;
				// addiere 2^Position
				if (bitToPow == 1) outputToWrite += 1 << 7 - countEightBits;
				// erhöhe Zähler-Position in Byte
				countEightBits++;
				// d.h. Byte vollständig gelesen für Schreiben in Output
				if (countEightBits == 8) {
					bufferedOutputStream.write(outputToWrite);
					outputToWrite = 0;
					countEightBits = 0;
				}
			}
			inputRead = bufferedInputStream.read();
		}
	}

	/** Verschlüsseln der Datei in Base-32-hex Alphabet aus binärer Darstellung */
	private void encode() throws ImageConverterException, IOException {
		// zu lesender binärer Input
		int inputRead;
		// Zähler für 5-Bit Base-32-hex Verschlüsselung
		int countFiveBits = 0;
		int outputToWrite = 0;
		// Potenz 2^bitToPow
		int bitToPow;

		inputRead = bufferedInputStream.read();
		while (inputRead != -1) {
			for (int i = 0; i < 8; i++) {
				/* ">>>": schiebe 7 -i Nullen von links, maskiere zu lesendes erstes Bit mit
				 * logischem UND, gelesenes Bit 0 oder 1, 0x1 (hex) = 0000 0001(binär) */
				bitToPow = (inputRead >>> 7 - i) & 0x1;
				// addiere 2^Position
				if (bitToPow == 1) outputToWrite += 1 << 4 - countFiveBits;

				// erhöhe Zählerposition in 5-Bit Base-32-hex-Darstellung
				countFiveBits++;

				// d.h. 5-Bit vollständig gelesen
				if (countFiveBits == 5) {
					bufferedOutputStream.write(base32HexAlphabet.charAt(outputToWrite));
					outputToWrite = 0;
					countFiveBits = 0;
				}
			}
			inputRead = bufferedInputStream.read();
		}

		// d.h. noch Zeichen zu schreiben, Byte nicht voll mit 5 Bits
		if (countFiveBits != 0) {
			// somit keine weitere Addition von 2^Position
			bufferedOutputStream.write(base32HexAlphabet.charAt(outputToWrite));
		}
	}
}
