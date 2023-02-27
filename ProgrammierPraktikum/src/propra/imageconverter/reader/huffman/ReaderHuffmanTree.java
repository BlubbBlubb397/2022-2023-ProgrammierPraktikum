package propra.imageconverter.reader.huffman;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;

import propra.imageconverter.Model;
import propra.imageconverter.checksum.CheckSum;
import propra.imageconverter.enums.EFormat;

/** Instanz dieser Klasse liest HuffmanTree aus InputFile und erstellt HashMap,
 * in der Keys und 8-Bit Blattwerte enthalten sind.
 *
 * @author Martina Koch */
public class ReaderHuffmanTree {
	private RandomAccessFile randomAccessInputFile;
	private CheckSum checkSumInput;
	private StringBuilder restBitStreamAfterConstructingHuffmanTree;
	private StringBuilder bitStringToReconstructHuffmanTree = new StringBuilder();
	private StringBuilder pathThroughTree = new StringBuilder();
	/** In hashMapHuffmanTree wird zu jedem Key des Knotens der entschlüsselte 8-bit
	 * Wert geschrieben */
	private HashMap<String, String> hashMapHuffmanTree = new HashMap<>();

	public ReaderHuffmanTree(Model model, RandomAccessFile randomAccessInputFile) throws IOException {
		this.randomAccessInputFile = randomAccessInputFile;
		this.checkSumInput = model.getInputFormat().equals(EFormat.PROPRA) ? new CheckSum(0) : new CheckSum(-1);
		randomAccessInputFile.seek(model.getInputFormat().getHeaderLength());

		/* Methode wird mit leerem String gestartet, da Pfad zur Wurzel dem leeren
		 * String entspricht */
		recursivelyReadHuffmanTree("");
		restBitStreamAfterConstructingHuffmanTree = bitStringToReconstructHuffmanTree;
	}

	/** Rekursive Funktion liest in Preorder (d.h. Wurzel - links - rechts) den
	 * Dekodierungsbaum/das Huffman-Wörterbuch ein, maximale theoretische Größe in
	 * Bytes: <br>
	 *
	 * 256 Blätter und 255 innere Knoten, somit <br>
	 * => je Blatt: 1 Bit für Codierung, ob innerer Knoten oder Blatt (innerNode: 0,
	 * leaf: 1) und 8 Bit für eigentliche Knoteninformation
	 *
	 * d.h. 256 Blätter (1 Bit + 8 Bit) + (255 innere Knoten x 1 Bit) = 2559 Bits,
	 * d.h. 320 Bytes
	 *
	 * @param branchLeftOrRight
	 * @throws IOException */
	void recursivelyReadHuffmanTree(String branchLeftOrRight) throws IOException {
		pathThroughTree.append(branchLeftOrRight);

		readNextBytesAndAddToBitStringIfSubstringLengthLessThenEight();

		boolean isFirstBitIndicatingLeaf = evaluateIfFirstBitOfBitStringIndicatesLeaf();

		// Blatt erreicht
		if (isFirstBitIndicatingLeaf) {
			readNextBytesAndAddToBitStringIfSubstringLengthLessThenEight();
			putLeafInformationToHashMapHuffmanTree();
		}

		// inneren Knoten erreicht
		else {
			readNextBytesAndAddToBitStringIfSubstringLengthLessThenEight();
			// linker Teilbaum
			recursivelyReadHuffmanTree("0");
			// rechter Teilbaum
			recursivelyReadHuffmanTree("1");
			// d.h. aktuelle Position im Baum ist nicht Wurzel
			if (pathThroughTree.length() > 0) {
				// beim Aufsteigen im Baum letztes PfadBit löschen, außer an der Wurzel
				pathThroughTree.deleteCharAt(pathThroughTree.length() - 1);
			}
		}
	}

	private void readNextBytesAndAddToBitStringIfSubstringLengthLessThenEight() throws IOException {
		int amountOfBytesToRead = 200;
		if (bitStringToReconstructHuffmanTree.length() <= 8) {
			int lastByteReadFromInputFile;
			int i = 0;
			/* an dieser Stelle ist die Reihenfolge der beiden Abfragen wichtig, da nur ein
			 * Byte gelesen werden darf, wenn die erste Bedingung zutrifft */
			while ((i < amountOfBytesToRead) && ((lastByteReadFromInputFile = randomAccessInputFile.read()) != -1)) {
				checkSumInput.calculateCheckSumForByteArray(
				        ByteBuffer.allocate(1).put((byte) lastByteReadFromInputFile).array());

				String toBinaryStringByteToRead = Integer.toBinaryString(lastByteReadFromInputFile);
				String stringFormatted = String.format("%08d", Integer.valueOf(toBinaryStringByteToRead));

				bitStringToReconstructHuffmanTree.append(stringFormatted);
				i++;
			}
		}
	}

	private void putLeafInformationToHashMapHuffmanTree() {
		String leafValue = determineLeafValueForHuffmanTreeHashMap();
		String determinePathThroughTreeAsHashMapKey = pathThroughTree.toString();
		String leafValueFormatted = String.format("%08d", Integer.valueOf(leafValue));
		hashMapHuffmanTree.put(determinePathThroughTreeAsHashMapKey, leafValueFormatted);
		pathThroughTree.deleteCharAt(pathThroughTree.length() - 1);
	}

	private String determineLeafValueForHuffmanTreeHashMap() {
		String leafSubstring = bitStringToReconstructHuffmanTree.substring(0, 8);
		bitStringToReconstructHuffmanTree.delete(0, 8);
		return leafSubstring;
	}

	/** Kodierung im vordersten Bit: Blatt: 1, innerer Knoten: 0
	 * @return */
	private boolean evaluateIfFirstBitOfBitStringIndicatesLeaf() {
		String valueAtNextPositionBitString = bitStringToReconstructHuffmanTree.substring(0, 1);
		bitStringToReconstructHuffmanTree.deleteCharAt(0);
		boolean isLeaf = valueAtNextPositionBitString.equals("1");
		return isLeaf;
	}

	public StringBuilder getRestBitStreamAfterConstructingHuffmanTree() {
		return this.restBitStreamAfterConstructingHuffmanTree;
	}

	public boolean lookUpKeyInHuffmanTreeHashMap(String bitString) {
		return hashMapHuffmanTree.containsKey(bitString);
	}

	public String getCodeForKey(String keyToEncode) {
		return hashMapHuffmanTree.get(keyToEncode);
	}

	public CheckSum getCheckSumInputAfterConstructingHuffmanTree() {
		return checkSumInput;
	}
}