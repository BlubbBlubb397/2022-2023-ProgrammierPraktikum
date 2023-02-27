package propra.imageconverter;

/** Instanzen dieser Klasse schreiben Nachricht auf Standardfehlerausgabe für
 * Benutzer sowie Statuscode beim abrupten Programmende.
 *
 * @author Martina Koch */
public class ImageConverterException extends Exception {
	public ImageConverterException(String error) {
		System.err.println("\n" + "Programmabbruch: " + error);
		System.exit(123);
	}

	/** Statische Methode generiert Nachricht auf Standardfehlerausgabe für Benutzer
	 * und Statuscode beim abrupten Programmende */
	public static void abruptlyExitProgram(Throwable exception) {
		System.err.println("\n" + "Programmabbruch: " + exception.getMessage());
		System.exit(123);
	}
}
