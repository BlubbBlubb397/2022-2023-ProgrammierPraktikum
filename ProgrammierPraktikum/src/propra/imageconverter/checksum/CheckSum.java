package propra.imageconverter.checksum;

/** Instanzen dieser Klasse repräsentieren Checksumme einer Datei.<br>
 *
 * @author martina */
public class CheckSum {
	private long[] A_n_B_n = { 0, 1 };
	private long calculatedCheckSum;
	private long positionInImageSegement = 1;

	/** Instanz der Checksumme wird mit 0 oder -1 initiailisiert. <br>
	 * 0: Ein-/Ausgabedatei ist Propra-Datei <br>
	 * -1: Ein-/Ausgabedatei ist keine Propra-Datei
	 * @param i */
	public CheckSum(int i) {
		this.calculatedCheckSum = i;
	}

	/** CheckSumme wird berechnet, wenn calculatedChecksum >= 0, d.h. Ein- oder
	 * Ausgabedatei ist Propradatei.
	 * @param rbgArray */
	public void calculateCheckSumForByteArray(byte[] rbgArray) {
		if (calculatedCheckSum >= 0) {
			for (byte b : rbgArray) {
				A_n_B_n = calculateCheckSumForEachByte(b);
				positionInImageSegement++;
			}
		}
	}

	/** Berechnet byteweise die Checksumme
	 *
	 * @param b Byte b an Position position
	 * @param A_n_B_n Übergebene Zwischenwerte für A_n und B_n
	 * @param position Position im Bilddatensegment, Zähler von 1 bis Länge
	 * Bilddatensegment-1
	 * @return Zwischensumme vor finaler Berechnung Checksumme */
	public long[] calculateCheckSumForEachByte(byte b) {
		long position = positionInImageSegement;
		long A_n = A_n_B_n[0];
		long B_n = A_n_B_n[1];
		A_n = (A_n + (position + Byte.toUnsignedInt(b)) % 65521) % 65521;
		B_n = (B_n + A_n) % 65521;

		long[] A_n_B_n_next = { A_n, B_n };

		return A_n_B_n_next;
	}

	/** CheckSumme wird final berechnet, wenn calculatedChecksum>-1, d.h. Ein- oder
	 * Ausgabeformat eine Propradatei.
	 * @return */
	public long finallyCalculateChecksum() {
		if (calculatedCheckSum >= 0) {
			// Bitschieben (<<)nach links um 16 Stellen (Einfüllen Nullen) entspricht 2^16
			calculatedCheckSum = (A_n_B_n[0] << 16) + A_n_B_n[1];
		}
		return calculatedCheckSum;
	}
}