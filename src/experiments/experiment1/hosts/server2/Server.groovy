#!/usr/bin/env groovy

package experiments.experiment1.hosts.server2

import common.utils.Utils
import experiments.experiment1.stack.Stack
import java.util.regex.Matcher

/**
 * Ein einfacher HTTP-Server.<br/>
 * Liefert bei Übergabe des Dokumentsnamen "index.html" den Namen des angeforderten Dokuments zurück.<br/>
 * Soll bei Übergabe des Dokumentsnamens "daten" eine größere Datenmenge zu Testzwecken liefern.
 */
class Server {

    //========================================================================================================
    // Vereinbarungen ANFANG
    //========================================================================================================

    // Der Netzwerk-Protokoll-Stack
    Stack stack

    /** Konfigurations-Objekt */
    ConfigObject config

    /** Stoppen der Threads wenn false */
    Boolean run = true
    /** ID der TCP-Verbindung */
    int connId

    /** Der im HTTP-Request gelieferte Name */
    String name = ""

    /** Anwendungsprotokolldaten */
    String apdu

    /** Anwendungsprotokolldaten als String */
    String data

    /** Länge der gesendeten Daten */
    int dataLength = 0

    /** Antwort */
    GString reply1 =
            """\
HTTP/1.1 200 OK
Content-Length: ${->dataLength}
Content-Type: text/plain

"""

    GString reply2 =
            """\
Das Objekt ${->name} wurde angefragt!
"""

    /** Ein Matcher-Objekt zur Verwendung regulärer Ausdruecke */
    Matcher matcher

    /** Daten empfangen solange false */
    boolean ready = false


    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    //------------------------------------------------------------------------------
    /**
     * Start der Anwendung
     */
    static void main(String[] args) {
        // Client-Klasse instanziieren
        Server application = new Server()
        // und starten
        application.server()
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Ein HTTP-Server mit rudimentärer Implementierung des Protokolls HTTP (Hypertext Transfer Protocol)
     */
    void server() {

        // ------------------------------------------------------------

        // Konfiguration holen
        config = Utils.getConfig("experiment1", "server")

        // ------------------------------------------------------------

        // Netzwerkstack initialisieren
        stack = new Stack()
        stack.start(config)

        //------------------------------------------------

        Utils.writeLog("Server", "server", "startet", 1)


        //------------------------------------------------

        while (run) {
            // Auf das Öffnen einer TCP-Verbindung warten
            Map aidu = stack.tcpListen()

            // Verbindungskennung merken

            if(aidu== null){
                sleep(1000)
            }else {


                connId = aidu.connId

                while (run) {

                    // Auf Empfang warten
                    Map tidu = stack.tcpReceive(connId: connId)

                    // Es wurden längere Zeit keine Daten empfangen oder die Datenlänge ist 0
                    // -> die TCP-Verbindung wird als geschlossen angenommen
                    if (!tidu.sdu)
                    // Nein, innere while-Schleife abbrechen
                        break

                    // A-PDU uebernehmen
                    apdu = tidu.sdu

                    Utils.writeLog("Server", "server", "empfängt: ${new String(apdu)}", 1)

                    // Protokollkopf holen
                    data = new String(apdu)

                    // Parsen des HTTP-Kommandos
                    matcher = (data =~ /GET\s*\/(.*?)\s*HTTP\/1\.1/)

                    name = ""
                    // Wurde das Header-Feld gefunden?
                    if (matcher) {
                        // Ja
                        // Name des zu liefernden Objekts
                        name = (matcher[0] as List<String>)[1]

                        String reply = ""

                        switch (name) {
                            case "index.html":
                                // Antwort erzeugen
                                String temp = reply2 // name wird eingetragen
                                dataLength = reply2.bytes.size()
                                reply = reply1 + temp // dabei wird dataLength in reply1 eingetragen
                                break

                            case "daten":
                                // hier langen HTTP-body erzeugen um lang anhaltende Übertragung zu erreichen
                                data = "Blablablubberblubberfaselblablablubberblubberfaselblablablubberblubberfaselblablablubberblubberfasel"

                                dataLength = data.bytes.size()
                                reply = reply1 + data // dabei wird dataLength in reply1 eingetragen
                                break
                        }

                        Utils.writeLog("Server", "server", "sendet: ${new String(apdu)}", 11)

                        // Antwort senden
                        stack.tcpSend([connId: connId, sdu: reply])
                    }
                } // while

            } } // while
    }
}

//------------------------------------------------------------------------------
