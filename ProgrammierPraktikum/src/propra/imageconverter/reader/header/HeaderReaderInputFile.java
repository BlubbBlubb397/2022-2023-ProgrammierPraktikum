package propra.imageconverter.reader.header;

import java.io.*;

import propra.imageconverter.Model;
import propra.imageconverter.enums.ECompressionType;

/** Es werden keine Instanzen der Klasse erstellt, diese vererbt aber an
 * Subklassen seine Attribute und Methoden, um Code-Redundanzen zu vermeiden. *
 *
 * @author Martina Koch */
public abstract class HeaderReaderInputFile {
	public Model model;
	public File file;
	public FileInputStream fileInputStream;
	public byte[] headerInputFile;
	public int imageWidth;
	public int imageHeight;
	public byte pixelDepth;
	public ECompressionType compressionType;

	HeaderReaderInputFile(Model model) throws IOException {
		this.model = model;
		file = new File(model.getInputFilePath());
	}

	public File getFile() {
		return file;
	}

	public int getWidth() {
		return imageWidth;
	}

	public int getHeight() {
		return imageHeight;
	}

	public ECompressionType getCompressionType() {
		return compressionType;
	}
}
