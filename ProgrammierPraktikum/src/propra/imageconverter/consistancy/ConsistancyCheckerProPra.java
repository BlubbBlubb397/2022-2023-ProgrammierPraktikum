package propra.imageconverter.consistancy;

import propra.imageconverter.*;
import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;
import propra.imageconverter.reader.header.HeaderReaderProPraInputFile;

/**
 * Instanzen dieser Klasse überprüfen die Konsistenz einer ProPra-Eingabe-Datei.
 *
 * @author Martina Koch
 */
public class ConsistancyCheckerProPra extends ConsistancyChecker implements IConsistancyChecker {
    private IHeaderReaderInputFile format;
    // enspricht "ProPraWiSe22"
    private final byte[] originalProPraFormatCode = { 80, 114, 111, 80, 114, 97, 87, 105, 83, 101, 50, 50 };

    public ConsistancyCheckerProPra(Model model, IHeaderReaderInputFile format) throws ImageConverterException {
        // ruft über Super-Konstruktor auch allg. Check-Methoden der InputFiles auf
        super(model, format);
        this.format = format;

        checkDataSegmentSize();
        // Propra-spezifische Tests
        checkFormatCode();
    }

    private void checkFormatCode() throws ImageConverterException {
        // byteweiser Vergleich formatCode
        for (int i = 0; i < originalProPraFormatCode.length; i++) {
            if (((HeaderReaderProPraInputFile) inputFormat).getFormatCode()[i] != originalProPraFormatCode[i]) {
                throw new ImageConverterException("Falscher Formatcode in ProPra-Datei");
            }
        }
    }

    public void checkCheckSum() throws ImageConverterException {
        boolean isInputOutputCompressionEqual = model.getInputCompressionType()
                .equals(model.getOutputCompressionType());
        boolean isInputOutputFormatEqual = model.isInputOutputFormatEqual();

        if (!(model.getInputFormat().equals(EFormat.PROPRA) && isInputOutputFormatEqual
                && isInputOutputCompressionEqual)) {
            if (model.getCheckSumInputFile() != -1 && ((HeaderReaderProPraInputFile) inputFormat)
                    .getCheckSumAusHeader() != model.getCheckSumInputFile()) {
                throw new ImageConverterException("Checksumme nicht korrekt");
            }
        }
    }

    /**
     * Statische Methode aus Checker-Klasse überprüft, ob Datensegmentgröße/3 != 0,
     * somit genug Informationen pro Pixel vorhanden
     *
     * @param dataSegmentSizeAusHeader
     * @param dataSegmentSizeInFile
     * @throws ImageConverterException
     */
    public static void checkDataSegmentSize(long dataSegmentSizeAusHeader, long dataSegmentSizeInFile)
            throws ImageConverterException {
        // Test, ob 3 Bytes pro Bildpunkt zur Verfügung stehen
        if (dataSegmentSizeInFile % 3 != 0) {
            throw new ImageConverterException("Zu wenig Bilddaten in Datei");
        }
        if (dataSegmentSizeAusHeader % 3 != 0) {
            throw new ImageConverterException("Im Header angegebene Segementgröße nicht zulässig");
        }
    }

    public void checkDataSegmentSize() throws ImageConverterException {
        if (format.getCompressionType().equals(ECompressionType.UNCOMPRESSED)) {
            long dataSegmentSizeBerechnet = inputFormat.getRealDataSegementSizeInFile();

            // Test, ob 3 Bytes pro Bildpunkt zur Verfügung stehen
            if (inputFormat.getRealDataSegementSizeInFile() % 3 != 0) {
                throw new ImageConverterException("Zu wenig Bilddaten in Datei");
            }

            if (((HeaderReaderProPraInputFile) inputFormat).getHeaderDataSegmentSize() % 3 != 0) {
                throw new ImageConverterException("Im Header angegebene Segementgröße nicht zulässig");
            }
            // Test, ob angegebene Datensegmentgröße im Header und berechnete übereinstimmen
            if (((HeaderReaderProPraInputFile) inputFormat).getHeaderDataSegmentSize() != (dataSegmentSizeBerechnet)) {
                throw new ImageConverterException(
                        "Datensegmentgröße im Header stimmt nicht benötigter Datensegmentgröße überein");
            }
            /*
             * Test, ob im Header angegebene und tatsächlich vorhandene Datensegmentgröße
             * übereinstimmen
             */
            if (((HeaderReaderProPraInputFile) inputFormat).getFileLength()
                    - 30 < ((HeaderReaderProPraInputFile) inputFormat).getHeaderDataSegmentSize()) {
                throw new ImageConverterException("Zu wenig Bilddaten im Datensegment");
            }

            if (((HeaderReaderProPraInputFile) inputFormat).getFileLength()
                    - 30 > ((HeaderReaderProPraInputFile) inputFormat).getHeaderDataSegmentSize()) {
                throw new ImageConverterException("Zu viele Bilddaten im Datensegment");
            }
        }
    }
}
