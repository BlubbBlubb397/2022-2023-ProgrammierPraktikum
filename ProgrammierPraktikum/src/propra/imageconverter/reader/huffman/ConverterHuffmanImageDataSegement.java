package propra.imageconverter.reader.huffman;

import java.io.IOException;
import java.io.RandomAccessFile;

import propra.imageconverter.Model;
import propra.imageconverter.checksum.CheckSum;
import propra.imageconverter.enums.EFormat;

/** Instanz dieser Klasse initialisiert HuffmanTreeReader und stößt Dekodierung
 * Bilddatensegment an. Hiermit werden aus dem Huffman-kodierten
 * Bilddatensegment die entsprechenden Output-Daten geschrieben.
 *
 * @author Martina Koch */
public class ConverterHuffmanImageDataSegement {
	private CheckSum checkSumInput;
	private CheckSum checkSumOutput;
	private Model model;
	private RandomAccessFile randomAccessInputFile;
	private RandomAccessFile randomAccessOutputFile;
	private ReaderHuffmanTree huffmanTreeReader;
	private DecoderHuffmanImageDataSegment huffmanDecoder;

	public ConverterHuffmanImageDataSegement(Model model) throws IOException {
		this.model = model;
		randomAccessInputFile = new RandomAccessFile(model.getInputFilePath(), "r");
		randomAccessOutputFile = new RandomAccessFile(model.getOutputFilePath(), "rw");

		initializeCheckSumInputOutputFile();

		readHuffmanTree();
		decodeHuffmanImageDataSegment();

		finallyCalculateCheckSum();
	}

	/** Intitalisiere CheckSumInput/Output mit 0, wenn Propra-Datei
	 * eingelesen/ausgegeben wird, sonst -1; */
	private void initializeCheckSumInputOutputFile() {
		checkSumInput = model.getInputFormat().equals(EFormat.PROPRA) ? new CheckSum(0) : new CheckSum(-1);
		checkSumOutput = model.getOutputFormat().equals(EFormat.PROPRA) ? new CheckSum(0) : new CheckSum(-1);
	}

	private void finallyCalculateCheckSum() {
		checkSumInput = huffmanDecoder.getCheckSumInput();
		checkSumOutput = huffmanDecoder.getCheckSumOutput();

		model.setCheckSumInputFile(checkSumInput.finallyCalculateChecksum());
		model.setCheckSumOutputFile(checkSumOutput.finallyCalculateChecksum());
	}

	private void readHuffmanTree() throws IOException {
		huffmanTreeReader = new ReaderHuffmanTree(model, randomAccessInputFile);
		checkSumInput = huffmanTreeReader.getCheckSumInputAfterConstructingHuffmanTree();
	}

	private void decodeHuffmanImageDataSegment() throws IOException {
		huffmanDecoder = new DecoderHuffmanImageDataSegment(randomAccessInputFile, randomAccessOutputFile, huffmanTreeReader, model);
	}
}
