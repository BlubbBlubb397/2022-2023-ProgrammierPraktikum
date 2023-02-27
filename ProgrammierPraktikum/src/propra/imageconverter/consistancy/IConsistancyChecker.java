package propra.imageconverter;

/** Interface ermöglicht Austausch/Erweiterung der benutzten
 * Datei-Format-Checker
 *
 * @author Martina Koch */
public interface IConsistancyChecker {

	void checkCompression() throws ImageConverterException;

	void checkHeightWidth() throws ImageConverterException;

	void checkPixelDepth() throws ImageConverterException;
}
