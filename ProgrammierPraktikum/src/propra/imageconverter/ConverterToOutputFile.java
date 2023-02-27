package propra.imageconverter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;
import propra.imageconverter.headercomposer.HeaderComposerForProPraOutputFile;
import propra.imageconverter.headercomposer.HeaderComposerForTGAOutputFile;
import propra.imageconverter.reader.huffman.ConverterHuffmanImageDataSegement;
import propra.imageconverter.transformimage.TransformImageDataToRLE;
import propra.imageconverter.transformimage.TransformImageDataToUncompressed;

/** Instanzen dieser Klasse transformieren die Eingabebilddatei in eine
 * Ausgabedatei. Hierbei werden zuerst die Output-Header-Daten mit dem
 * HeaderComposer in ein Array geschrieben. Anschließend werden zuerst die
 * transformierten Bilddaten in das Output-Format geschrieben. <br>
 * Für eine Propra-Ausgabedatei werden aus diesen Bilddaten die
 * Bilddatensegmentlänge und die Checksumme in das Header-Output-Array
 * geschrieben. <br>
 * Abschließend wird der Header in die Output-Datei geschrieben.
 *
 * @author Martina Koch */
public class ConverterToOutputFile {
	private Model model;
	private IHeaderReaderInputFile headerReaderInputFormat;
	private byte[] headerOutputFile;
	private IHeaderComposerOutputFile headerComposerForOutputFile;

	private BufferedInputStream bufferedInputStream;
	private BufferedOutputStream bufferedOutputStream;
	private RandomAccessFile randomAccessFileInput;
	private RandomAccessFile randomAccessFileOutput;

	public ConverterToOutputFile(Model model, IHeaderReaderInputFile headerReaderInputFile)
	        throws ImageConverterException, IOException {
		this.model = model;
		this.headerReaderInputFormat = headerReaderInputFile;
		this.headerOutputFile = new byte[model.getOutputFormat().getHeaderLength()];

		// ggf. vorhandene OutputDatei wird vorab gelöscht
		Path outputPath = Paths.get(model.getOutputFilePath());
		if (outputPath.toFile().isFile()) {
			new File(model.getOutputFilePath()).delete();
		}

		try {
			/* RandomAccessFile und FileInputStream zeigen auf gleiche Position, lesen von
			 * FileInputStream oder RandomAccessFile bewegen den Filepointer bei beiden
			 * Objekten */
			randomAccessFileInput = new RandomAccessFile(model.getInputFilePath(), "r");
			randomAccessFileOutput = new RandomAccessFile(model.getOutputFilePath(), "rw");
			bufferedInputStream = new BufferedInputStream(new FileInputStream(randomAccessFileInput.getFD()));
			bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(randomAccessFileOutput.getFD()));

			composeHeaderOutputFile();
			transformAndWriteImageData();

			assignImageDataSegmentSizeAndCheckSumIfPropraOutputFile();
			writeHeaderToOutputFile();

			bufferedInputStream.close();
			bufferedOutputStream.close();
		} catch (FileNotFoundException e) {
			ImageConverterException.abruptlyExitProgram(e);
		} catch (IOException e) {
			ImageConverterException.abruptlyExitProgram(e);
		}
	}

	private void composeHeaderOutputFile() throws ImageConverterException, IOException {
		boolean isInputOutputFormatEqual = model.isInputOutputFormatEqual();
		switch (model.getInputFormat()) {
		case TGA:
			headerComposerForOutputFile = isInputOutputFormatEqual
			        ? new HeaderComposerForTGAOutputFile(model, headerReaderInputFormat)
			        : new HeaderComposerForProPraOutputFile(model, headerReaderInputFormat);
			break;
		case PROPRA:
			headerComposerForOutputFile = isInputOutputFormatEqual
			        ? new HeaderComposerForProPraOutputFile(model, headerReaderInputFormat)
			        : new HeaderComposerForTGAOutputFile(model, headerReaderInputFormat);
			break;
		case OTHER:
			throw new ImageConverterException("Format für Konvertierung nicht zulässig.");
		}
	}

	/** Transformiert und schreibt zuerst Output-ImageDaten, damit hieraus ggf. die
	 * Datensegmentlänge und Checksumme berechnet und dann in Header geschrieben
	 * werden kann */
	public void transformAndWriteImageData() throws IOException {
		if (model.getInputCompressionType().equals(ECompressionType.HUFFMAN)) {
			new ConverterHuffmanImageDataSegement(model);
		}

		else {
			switch (model.getOutputCompressionType()) {
			case UNCOMPRESSED:
				new TransformImageDataToUncompressed(model);
				break;
			case RLE:
				new TransformImageDataToRLE(model);
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + model.getOutputCompressionType());
			}
		}
	}

	private void assignImageDataSegmentSizeAndCheckSumIfPropraOutputFile() throws IOException {
		long writtenImageDataSegmentSize_Output;
		if (model.getOutputFormat().equals(EFormat.PROPRA)) {
			writtenImageDataSegmentSize_Output = randomAccessFileOutput.length()
			        - model.getOutputFormat().getHeaderLength();
			((HeaderComposerForProPraOutputFile) headerComposerForOutputFile)
			        .setImageDataSegmentSize(writtenImageDataSegmentSize_Output);
			((HeaderComposerForProPraOutputFile) headerComposerForOutputFile).setCheckSum();
		}
	}

	public void writeHeaderToOutputFile() throws IOException {
		headerOutputFile = headerComposerForOutputFile.getHeader();
		randomAccessFileOutput.seek(0L);
		randomAccessFileOutput.write(headerOutputFile);
	}
}
