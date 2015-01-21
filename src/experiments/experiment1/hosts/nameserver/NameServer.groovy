package experiments.experiment1.hosts.nameserver

import common.utils.Utils

/**
 * Ein Server der Gerätenamen in IPv4-Adressen auflöst. Als Transport-Protokoll wird UDP verwendet.
 */
class NameServer {

    //========================================================================================================
    // Vereinbarungen ANFANG
    //========================================================================================================

    /** Der Netzwerk-Protokoll-Stack */
    experiments.experiment1.stack.Stack stack

    String data
    String IPAdr

    /** Konfigurations-Objekt */
    ConfigObject config

    /** Stoppen der Threads wenn false */
    Boolean run = true

    /** Tabelle zur Umsetzung von Namen in IP-Adressen */
    Map<String, String> nameTable = [
            "meinhttpserver": "192.168.2.10",
            "alice": "0.0.0.0",
            "bob": "0.0.0.0",
    ]

    /**eigene Portnummer*/
    int ownPort

    /**Clientdaten*/
    String srcIpAddr
    int srcPort

    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    //------------------------------------------------------------------------------
    /**
     * Start der Anwendung
     */
    static void main(String[] args) {
        // Client-Klasse instanziieren
        NameServer application = new NameServer()
        // und starten
        application.nameserver()
    }
    //------------------------------------------------------------------------------

    /**
     * Der Namens-Dienst
     */
    void nameserver() {

        //------------------------------------------------

        // Konfiguration holen
        config = Utils.getConfig("experiment1", "nameserver")

        // ------------------------------------------------------------

        // Netzwerkstack initialisieren
        stack = new experiments.experiment1.stack.Stack()
        stack.start(config)
        ownPort = config.ownPort

        Utils.writeLog("NameServer", "nameserver", "startet", 1)

        while (run) {
            // Hier Protokoll implementieren:
            // auf Empfang ueber UDP warten
            (srcIpAddr, srcPort, data) = stack.udpReceive()
            Utils.writeLog("Nameserver", "nameserver", "empfängt: $data", 1)
            // Namen über nameTable in IP-Adresse aufloesen
            IPAdr = nameTable.get(data)
            // IP-Adresse ueber UDP zuruecksenden
            stack.udpSend(dstIpAddr: srcIpAddr, dstPort: srcPort,
                    srcPort: ownPort, sdu: IPAdr)

            Utils.writeLog("Nameserver", "nameserver", "sendet: $IPAdr", 1)
        }
    }
    //------------------------------------------------------------------------------
}