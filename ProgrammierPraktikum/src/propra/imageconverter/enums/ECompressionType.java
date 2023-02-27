package propra.imageconverter.enums;

/** Zentrale Verwaltung der Kompressionseigenschaft
 *
 * @author Martina Koch */
public enum ECompressionType {
	UNCOMPRESSED, RLE, HUFFMAN;

	/** toString-Methode überschrieben für schönere Ausgabe der gewählten
	 * Komprimierungsoption auf Konsole. */
	@Override
	public String toString() {
		return this.name().toLowerCase();
	}
}
