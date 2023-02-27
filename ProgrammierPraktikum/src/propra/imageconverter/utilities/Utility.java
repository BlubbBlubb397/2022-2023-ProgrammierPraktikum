package propra.imageconverter.utilities;

/** Klasse enthält statische Hilfsmethoden, um Coderedundanzen zu vermeiden und
 * Lesbarkeit zu erhöhen
 *
 * @author Martina Koch */

public class Utility {
	/** Bestimmt aus zwei Bytes int-Wert in LittleEndian-Order
	 *
	 * @return Gibt integer-Wert in Little Endian für zwei byte-Werte zurück */
	public static int getInt(byte byte1, byte byte2) {
		/* Maskieren mit 0xff(hex)/1111 1111(bin) und Bitschieben um 0 bzw. 8 Stellen
		 * nach links */
		int value = ((byte2 & 0xff) << 8) | ((byte1 & 0xff) << 0);
		return value;
	}

	/** Gibt aus Integer-Wert Byte-Array (little Endian) zurück. */
	public static byte[] getTwoBytesOutOfInt(int intValue) {
		byte[] twoBytes = new byte[2];
		/* ">>" Bitschieben nach rechts um 0 bzw. 8 Stellen und Maskieren mit
		 * 0xff(hex)/1111 1111(bin) */
		twoBytes[0] = (byte) ((intValue >> 0) & 0xff);
		twoBytes[1] = (byte) ((intValue >> 8) & 0xff);
		return twoBytes;
	}
}
