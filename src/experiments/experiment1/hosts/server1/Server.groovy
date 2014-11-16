#!/usr/bin/env groovy

package experiments.experiment1.hosts.server1

import common.utils.Utils
import experiments.experiment1.stack.Stack

import java.util.regex.Matcher

/**
 * Ein einfacher HTTP-Server.<br/>
 * Liefert bei Übergabe des Dokumentsnamen "index.html" den Namen des angeforderten Dokuments zurück.<br/>
 * Soll bei Übergabe des Dokumentsnamens "daten" eine größere Datenmenge zu Testzwecken liefern.<br/>
 * Zum Transport wird UDP verwendet. Der Server ist dadurch nicht für die Kommunikation mit realen HTTP-Clients
 * verwendbar, da diese stets TCP verwenden!
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

    /** Der im HTTP-Request gelieferte Name des angeforderten Objekts*/
    String name = ""

    /** IP-Adresse und Portnummer des client */
    String srcIpAddr
    int srcPort

    /** Eigene Portnummer */
    int ownPort

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
        ownPort = config.ownPort

        //------------------------------------------------

        Utils.writeLog("Server", "server", "startet", 1)


        //------------------------------------------------

        while (run) {

            // Auf Empfang warten
            (srcIpAddr, srcPort, data) = stack.udpReceive()

            Utils.writeLog("Server", "server", "empfängt: $data", 1)

            // Abbruch wenn Länge der empfangenen Daten == 0
            if (!data)
                break

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
                        dataLength = reply2.size()
                        reply = reply1 + temp // dabei wird dataLength in reply1 eingetragen
                        break

                    case "daten":
                        // hier langen HTTP-body (einige kByte) erzeugen um lang anhaltende Übertragung
                        // zu erreichen
                        data = "....."

                        dataLength = data.size()
                        reply = reply1 + data // dabei wird dataLength in reply1 eingetragen
                        break
                }
                Utils.writeLog("Server", "server", "sendet: $reply", 11)

                // Antwort senden
                stack.udpSend(dstIpAddr: srcIpAddr, dstPort: srcPort,
                        srcPort: ownPort, sdu: reply)
            }
        } // while
    }
}

//------------------------------------------------------------------------------
