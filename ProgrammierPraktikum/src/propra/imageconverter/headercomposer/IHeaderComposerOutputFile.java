package propra.imageconverter;

import java.io.IOException;

/** Interface ermöglicht Austausch/Erweiterung der benutzten HeaderComposer,
 * welche für Ausgabedatei den Header erstellen.
 *
 * @author Martina Koch */
public interface IHeaderComposerOutputFile {
	void composeHeaderInformationForOutputFile() throws IOException;

	byte[] getHeader();
}
