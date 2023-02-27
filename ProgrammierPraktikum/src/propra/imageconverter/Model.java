package propra.imageconverter;

import java.io.File;

import propra.imageconverter.enums.ECompressionType;
import propra.imageconverter.enums.EFormat;

/** Instanz dieser Klasse definiert Datenmodel für Input- und Output-Datei und
 * Kompressionsparameter.
 *
 * @author Martina Koch */
public class Model {
	private String inputFilePath;
	private String outputFilePath;
	private ECompressionType inputCompressionType;
	private ECompressionType outputCompressionType;
	private boolean encodeBase32;
	private boolean decodeBase32;
	private EFormat inputFormat;
	private EFormat outputFormat;
	private long checkSumInputFile;
	private long checkSumOutputFile;
	private long realImageDataSegmentInputFile;
	private File temporaryFilePath;

	public void setInputFilePath(String inputFilePath) {
		this.inputFilePath = inputFilePath;
	}

	public String getInputFilePath() {
		return inputFilePath;
	}

	public void setInputFormat(EFormat inputFormat) {
		this.inputFormat = inputFormat;
	}

	public EFormat getInputFormat() {
		return inputFormat;
	}

	public String getOutputFilePath() {
		return outputFilePath;
	}

	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

	public EFormat getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(EFormat outputFormat) {
		this.outputFormat = outputFormat;
	}

	public void setInputCompressionType(ECompressionType compressionType) {
		this.inputCompressionType = compressionType;
	}

	public ECompressionType getInputCompressionType() {
		return inputCompressionType;
	}

	public ECompressionType getOutputCompressionType() {
		return outputCompressionType;
	}

	public void setOutputCompressionType(ECompressionType outputCompressionType) throws ImageConverterException {
		this.outputCompressionType = outputCompressionType;
	}

	public void setEncodeBase32(boolean encodeBase32) {
		this.encodeBase32 = encodeBase32;
	}

	public void setDecodeBase32(boolean decodeBase32) {
		this.decodeBase32 = decodeBase32;
	}

	public boolean getEncodeBase32() {
		return encodeBase32;
	}

	public boolean getDecodeBase32() {
		return decodeBase32;
	}

	public byte[] getInputRGBOrder() {
		return EFormat.getRGBOrder(inputFormat);
	}

	public byte[] getOutputRGBOrder() {
		return EFormat.getRGBOrder(outputFormat);
	}

	public boolean isInputOutputFormatEqual() {
		return inputFormat.equals(outputFormat);
	}

	/** Setzt Checksumm für InputFile, diese entspricht der berechneten Checksumme,
	 * wenn InputFile vom Format Propra, sonst -1;
	 * @param checkSumInputFile */
	public void setCheckSumInputFile(long checkSumInputFile) {
		this.checkSumInputFile = checkSumInputFile;
	}

	/** Gibt Checksumme aus, wenn InputFile vom Format Propra, sonst -1;
	 * @param calculatedCheckSumInputPropra */
	public long getCheckSumInputFile() {
		return checkSumInputFile;
	}

	/** Setzt Checksumm für OutputFile, diese entspricht der berechneten Checksumme,
	 * wenn OutputFile vom Format Propra, sonst -1;
	 * @param checkSumInputFile */
	public void setCheckSumOutputFile(long checkSumOutput) {
		checkSumOutputFile = checkSumOutput;
	}

	/** Gibt Checksumme aus, wenn OutputFile vom Format Propra, sonst -1;
	 * @param calculatedCheckSumInputPropra */
	public long getCheckSumOutputFile() {
		return checkSumOutputFile;
	}

	public void setRealImageDataSegmentInputFile(long realImageDataSegmentInputFile) {
		this.realImageDataSegmentInputFile = realImageDataSegmentInputFile;
	}

	public long getRealDataSegmentSizeInputFile() {
		return realImageDataSegmentInputFile;
	}

	public void setTemporaryFile(File temporaryOutputFile) {
		this.temporaryFilePath = temporaryOutputFile;
	}

	public File getTemporaryFilePath() {
		return this.temporaryFilePath;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Eingabeparameter: \n");
		builder.append("--input=" + this.inputFilePath + "\n");
		builder.append("--output=" + this.outputFilePath);
		if (outputCompressionType != null) {
			builder.append("\n--compression=" + this.outputCompressionType);
		} else if (encodeBase32) {
			builder.append("\n" + "encodeBase32: " + this.encodeBase32);
		} else {
			builder.append("\n" + "decodeBase32: " + this.decodeBase32);
		}
		return builder.toString();
	}
}
