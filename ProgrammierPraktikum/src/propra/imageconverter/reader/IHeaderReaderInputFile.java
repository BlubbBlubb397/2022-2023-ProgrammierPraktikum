package propra.imageconverter;

import java.io.File;
import java.io.IOException;

import propra.imageconverter.enums.ECompressionType;

/** Interface ermöglicht Austausch/Erweiterung der benutzten Header-Reader für
 * das Input-File.
 *
 * @author Martina Koch */
public interface IHeaderReaderInputFile {

	void extractInformationOutOfHeaderInputFile() throws ImageConverterException, IOException;

	long getRealDataSegementSizeInFile();

	byte getPixelDepth();

	int getWidth();

	int getHeight();

	ECompressionType getCompressionType();

	File getFile();
}
