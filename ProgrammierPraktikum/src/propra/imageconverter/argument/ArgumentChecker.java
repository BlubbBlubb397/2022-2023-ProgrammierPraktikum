package propra.imageconverter.argument;

import propra.imageconverter.ImageConverterException;
import propra.imageconverter.Model;
import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;

/**
 * Instanz dieser Klasse überprüft, ob Konsolen-Eingabeparameter zulässig sind
 *
 * @author Martina Koch
 */
public class ArgumentChecker {
    private EFormat inputFormat;
    private EFormat outputFormat;
    private String inputFilePath;
    private String outputFilePath;
    private Model model;
    private ECompressionType outputCompression;

    /**
     * Instanz dieser Klasse überprüft, ob Konsolen-Eingabeparameter zulässig sind
     */
    public ArgumentChecker(Model model) throws ImageConverterException {
        this.model = model;
        outputCompression = model.getOutputCompressionType();
        inputFilePath = model.getInputFilePath();
        outputFilePath = model.getOutputFilePath();
        inputFormat = model.getInputFormat();
        outputFormat = model.getOutputFormat();

        checkInputFilePath();
        checkInputFormat();

        checkOutputFilePath();
        checkOutputFormat();

        checkOutputCompressionType();
    }

    /**
     * Überprüft, ob Anzahl der Konsolen-Eingabeparamter zulässig ist
     * (ArgumentExtractor in ArgumentChecker ausgelagert als statische Methode, da
     * diese Überprüfung stattfindet, bevor ArgumentChecker instanziiert wird)
     *
     * @param i Anzahl der Konsolen-Eingabeparamter, mind. zwei Argumente für
     *          Encode/Decode Base-32, max. drei Argumente bei Konvertierung
     */
    static void checkArgumentNumber(int i) throws ImageConverterException {
        if ((i > 3) || i < 2) {
            throw new ImageConverterException(
                    "ungültige Parameteranzahl, ggf. Dateipfad in Hochkommata \"< Dateipfad >\"setzen");
        }
    }

    private void checkInputFormat() throws ImageConverterException {
        if ((!model.getDecodeBase32() && !model.getEncodeBase32()) && (inputFormat == EFormat.OTHER)) {
            throw new ImageConverterException("Input Format konnte nicht zugeordnet werden");
        }
    }

    private void checkOutputFormat() throws ImageConverterException {
        if ((!model.getDecodeBase32() && !model.getEncodeBase32()) && (outputFormat == EFormat.OTHER)) {
            throw new ImageConverterException("OuputFormat konnte nicht zugeordnet werden");
        }
    }

    private void checkInputFilePath() throws ImageConverterException {
        if (inputFilePath == null) {
            throw new ImageConverterException("InputFilePath konnte nicht erkannt werden");
        }
    }

    private void checkOutputFilePath() throws ImageConverterException {
        if (outputFilePath == null) {
            throw new ImageConverterException("OuputFilePath konnte nicht erkannt werden");
        }
    }

    private void checkOutputCompressionType() throws ImageConverterException {
        if (outputCompression != null && (!outputCompression.equals(ECompressionType.UNCOMPRESSED)
                && !outputCompression.equals(ECompressionType.RLE))) {
            throw new ImageConverterException("Output-Kompression nicht zulässig");
        }
    }
}
