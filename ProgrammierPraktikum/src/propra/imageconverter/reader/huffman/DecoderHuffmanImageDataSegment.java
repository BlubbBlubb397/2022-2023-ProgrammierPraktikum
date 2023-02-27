package propra.imageconverter.reader.huffman;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import propra.imageconverter.Model;
import propra.imageconverter.checksum.CheckSum;
import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;
import propra.imageconverter.transformimage.TransformImageDataToRLE;

/**
 * Instanzen dieser Klasse dekodieren mit Hilfe des eingelesenen Huffman-Trees
 * das InputFile. Mit Huffman-Tree werden Bilddaten dekodiert und unkomprimiert
 * ins Output File geschrieben. Bei Konvertierung zu RLE werden diese
 * anschließend in ein temporäres File zu RLE konvertiert und abschließend in
 * die finale Output-Datei geschrieben.
 *
 * @author Martina Koch
 */
public class DecoderHuffmanImageDataSegment {
    private StringBuilder bitString;
    private ReaderHuffmanTree huffmanTreeReader;
    private RandomAccessFile randomAccessInputFile;
    private RandomAccessFile randomAccessOutputFile;
    private Model model;
    private byte[] inputRGBOrder;
    private byte[] outputRGBOrder;

    private CheckSum checkSumInput;
    private CheckSum checkSumOutput;

    public DecoderHuffmanImageDataSegment(RandomAccessFile randomAccessInputFile,
            RandomAccessFile randomAccessOutputFile, ReaderHuffmanTree huffmanTreeReader, Model model)
            throws IOException {
        this.randomAccessInputFile = randomAccessInputFile;
        this.randomAccessOutputFile = randomAccessOutputFile;
        this.model = model;

        this.huffmanTreeReader = huffmanTreeReader;
        this.bitString = huffmanTreeReader.getRestBitStreamAfterConstructingHuffmanTree();

        assignInputOutputRGBOrder();
        initializeCheckSumInputAndOutputFile();

        decodeInputFileToUncompressed();
        convertUncompressedImageDataSegmentToRle();
    }

    private void convertUncompressedImageDataSegmentToRle() throws IOException, FileNotFoundException {
        if (model.getOutputCompressionType() == ECompressionType.RLE) {
            /*
             * Unkomprimiertes Bilddatensegment des Output-Files wird jetzt als InputFile
             * genutzt und in einem temporären File zu RLE komprimiert
             */
            new TransformImageDataToRLE(model, randomAccessOutputFile).convertUncompressedToRle();

            // schreibe Daten aus temporärer Datei in eigentliches OutputFile
            FileInputStream temporaryFileInputStream = new FileInputStream(model.getTemporaryFilePath());
            BufferedInputStream temporaryBufferedInputStream = new BufferedInputStream(temporaryFileInputStream);

            // in temporärer Date sind RLE komprimierte Bilddaten abgelegt
            long temporaryFileBytesToBeRead = model.getTemporaryFilePath().length();
            System.out.println(temporaryFileBytesToBeRead);
            byte[] temporaryByteBuffer = new byte[1024 * 3];

            randomAccessOutputFile.seek(model.getOutputFormat().getHeaderLength());

            // Überschreibe Bilddaten im OutputFile mit Bilddaten aus temporärer Datei
            while (temporaryFileBytesToBeRead != 0) {
                int bytesToRead = temporaryFileBytesToBeRead > (1024 * 3) ? (1024 * 3)
                        : (int) temporaryFileBytesToBeRead;
                temporaryByteBuffer = temporaryBufferedInputStream.readNBytes(bytesToRead);
                randomAccessOutputFile.write(temporaryByteBuffer, 0, bytesToRead);
                temporaryFileBytesToBeRead -= bytesToRead;
            }
            temporaryBufferedInputStream.close();
            model.getTemporaryFilePath().delete();
        }
        // schneide Bytes ab, da unkomprimiertes OutputFile mehr Bytes enthielt
        randomAccessOutputFile.setLength(randomAccessOutputFile.getFilePointer());
    }

    private void assignInputOutputRGBOrder() {
        inputRGBOrder = model.getInputRGBOrder();
        outputRGBOrder = model.getOutputRGBOrder();
    }

    private void initializeCheckSumInputAndOutputFile() {
        checkSumInput = huffmanTreeReader.getCheckSumInputAfterConstructingHuffmanTree();
        /*
         * bei RLE-Output Kodierung wird Checksumme Output erst berechnet, wenn im
         * zweiten Schritt RLE Komprimierung erfolgt
         */
        checkSumOutput = (!model.getOutputCompressionType().equals(ECompressionType.RLE)
                && model.getOutputFormat().equals(EFormat.PROPRA)) ? new CheckSum(0) : new CheckSum(-1);
    }

    private void decodeInputFileToUncompressed() throws IOException {
        int i = 0;
        String value = "";
        int bitStringPositionFromLeft = 1;
        byte[] nextPixelToWrite = new byte[3];

        // starte Schreiben des Bilddatensegmentes nach Header Bytes
        randomAccessOutputFile.seek(model.getOutputFormat().getHeaderLength());

        /*
         * Abbruchkriterium, d.h. solange Bytes zu lesen oder bitString noch Einser
         * enthält, d.h. noch Bits aus bitStream zu lesen
         */
        while (randomAccessInputFile.getFilePointer() < randomAccessInputFile.length()
                || (bitString.indexOf("1") != -1)) {
            readNextBytesAndAddToPathStringIfSubstringLengthLessThenEight();

            /*
             * setze bitStringPosition nach rechts so lange, bis Key in HuffmanTreeHashMap
             * gefunden wird
             */
            while (!isSubstringAKeyInHuffmanTreeHashMap(bitStringPositionFromLeft)) {
                if (bitStringPositionFromLeft > bitString.length()) {
                    readNextBytesAndAddToPathStringIfSubstringLengthLessThenEight();
                }
                bitStringPositionFromLeft++;
            }

            if (isSubstringAKeyInHuffmanTreeHashMap(bitStringPositionFromLeft)) {
                value = lookUpCodeForKeyInHashMapHuffmanTree(bitStringPositionFromLeft);

                nextPixelToWrite[i] = (byte) Integer.parseInt(value, 2);
                i++;

                bitString.delete(0, bitStringPositionFromLeft);
                // reset Leseposition
                bitStringPositionFromLeft = 1;

                if (i == 3) {
                    if (!model.isInputOutputFormatEqual()) {
                        byte[] turnedRGBOrderPixel = turnRGBOrder(nextPixelToWrite);
                        nextPixelToWrite = Arrays.copyOf(turnedRGBOrderPixel, 3);
                    }
                    randomAccessOutputFile.write(nextPixelToWrite);
                    checkSumOutput.calculateCheckSumForByteArray(nextPixelToWrite);
                    i = 0;
                }
            }
        }
    }

    private String lookUpCodeForKeyInHashMapHuffmanTree(int bitStringPositionFromLeft) {
        String bitStringSubstringAsKey = bitString.substring(0, bitStringPositionFromLeft);
        String codeForKeyInHashMapHuffmanTree = huffmanTreeReader.getCodeForKey(bitStringSubstringAsKey);
        return codeForKeyInHashMapHuffmanTree;
    }

    private void readNextBytesAndAddToPathStringIfSubstringLengthLessThenEight() throws IOException {
        int amountOfBytesToRead = 300;
        if (bitString.length() <= 8) {
            int lastByteReadFromInputFile;
            int i = 0;
            /*
             * an dieser Stelle ist die Reihenfolge der beiden Abfragen wichtig, da nur ein
             * Byte gelesen werden darf, wenn die erste Bedingung zutrifft
             */
            while ((i < amountOfBytesToRead) && ((lastByteReadFromInputFile = randomAccessInputFile.read()) != -1)) {
                checkSumInput.calculateCheckSumForByteArray(
                        ByteBuffer.allocate(1).put((byte) lastByteReadFromInputFile).array());

                String byteToReadToBinaryString = Integer.toBinaryString(lastByteReadFromInputFile);
                String stringFormatted = String.format("%08d", Integer.valueOf(byteToReadToBinaryString));
                bitString.append(stringFormatted);
                i++;
            }
        }
    }

    private boolean isSubstringAKeyInHuffmanTreeHashMap(int bitStringPosition) {
        String testSubstringAsKey = bitString.substring(0, bitStringPosition);
        return huffmanTreeReader.lookUpKeyInHuffmanTreeHashMap(testSubstringAsKey);
    }

    public CheckSum getCheckSumInput() {
        return this.checkSumInput;
    }

    public CheckSum getCheckSumOutput() {
        return this.checkSumOutput;
    }

    /**
     * Dreht RGB-Bytes entsprechend der Vorgaben der Input-/Output-Formate.
     *
     * @param rbgArray
     * @return
     */
    public byte[] turnRGBOrder(byte[] rbgArray) {
        byte[] turnedRGB = new byte[3];
        for (int i = 0; i < 3; i++) {
            turnedRGB[outputRGBOrder[i]] = rbgArray[inputRGBOrder[i]];
        }
        return turnedRGB;
    }
}
