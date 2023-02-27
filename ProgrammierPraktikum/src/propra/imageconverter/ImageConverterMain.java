package propra.imageconverter;

import java.io.IOException;

/**
 * Main-Methode startet über Konstruktor des Controllers das
 * Konvertierungsprogramm.<br>
 * <br>
 * Aufbau des Programms im MVC-Entwurfsmuster. Hierbei ist die Konsoleneingabe
 * als Benutzerschnittstelle Teil der View. Das MVC-Entwurfsmuster sowie die
 * Interfaces und Vererbungsbeziehungen ermöglichen eine einfache Anpassung und
 * Erweiterung der Komponenten. <br>
 * <br>
 * 
 * @author Martina Koch
 */

public class ImageConverterMain {
    public static void main(String[] args) {
        try {
            new Controller(args);
        } catch (ImageConverterException e) {
            ImageConverterException.abruptlyExitProgram(e);
        } catch (IOException e) {
            ImageConverterException.abruptlyExitProgram(e);
        } catch (Exception e) {
            ImageConverterException.abruptlyExitProgram(e);
        }
    }
}
