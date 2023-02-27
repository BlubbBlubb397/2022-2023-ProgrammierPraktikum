package propra.imageconverter.enums;

/** In Enum-Klasse RGBOrder ist RGB-Order und Header-Länge hinterlegt.
 *
 * @author Martina Koch */
public enum EFormat {
	TGA(2, 1, 0), PROPRA(0, 2, 1), OTHER(0, 0, 0);

	int positionRed;
	int positionGreen;
	int positionBlue;

	EFormat(int r, int g, int b) {
		positionRed = r;
		positionGreen = g;
		positionBlue = b;
	}

	/** Gibt RGB-Order des Formates zurück. */
	public static byte[] getRGBOrder(EFormat format) {
		byte[] RGBOrderFormat = new byte[3];

		switch (format) {
		case PROPRA: {
			RGBOrderFormat[0] = (byte) PROPRA.positionRed;
			RGBOrderFormat[1] = (byte) PROPRA.positionGreen;
			RGBOrderFormat[2] = (byte) PROPRA.positionBlue;
			return RGBOrderFormat;
		}
		case TGA: {
			RGBOrderFormat[0] = (byte) TGA.positionRed;
			RGBOrderFormat[1] = (byte) TGA.positionGreen;
			RGBOrderFormat[2] = (byte) TGA.positionBlue;
			return RGBOrderFormat;
		}
		default:
			throw new IllegalArgumentException("Unerwartetes Dateiformat: " + format);
		}
	}

	/** Gibt Dateikopflänge des Formates zurück.
	 * @return */
	public int getHeaderLength() {
		switch (this) {
		case TGA:
			return 18;
		case PROPRA:
			return 30;
		default:
			throw new IllegalArgumentException("Unexpected value: " + this);
		}
	}

	/** toString-Methode überschrieben für schönere Ausgabe des Formates auf
	 * Konsole. */
	@Override
	public String toString() {
		return this.name().toLowerCase();
	}
}
