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

    /** Konfigurations-Objekt */
    ConfigObject config

    /** Stoppen der Threads wenn false */
    Boolean run = true

    /** Tabelle zur Umsetzung von Namen in IP-Adressen */
    Map<String, String> nameTable = [
            "meinhttpserver": "0.0.0.0",
            "alice": "0.0.0.0",
            "bob": "0.0.0.0",
    ]

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

        Utils.writeLog("NameServer", "nameserver", "startet", 1)

        while (run) {
            // Hier Protokoll implementieren:
            // auf Empfang ueber UDP warten
            // Namen über nameTable in IP-Adresse aufloesen
            // IP-Adresse ueber UDP zuruecksenden
        }
    }
    //------------------------------------------------------------------------------
}
