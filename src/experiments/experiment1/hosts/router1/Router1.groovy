package experiments.experiment1.hosts.router1

import common.utils.Utils
import experiments.experiment1.stack.layer.IpLayer

/**
 * Ein IPv4-Router.<br/>
 * Nur als Ausgangspunkt für eigene Implementierung zu verwenden!<br/>
 * Verwendet UDP zur Verteilung der Routinginformationen.
 *
 */
class Router1 {

    //========================================================================================================
    // Vereinbarungen ANFANG
    //========================================================================================================

    /** Der Netzwerk-Protokoll-Stack */
    experiments.experiment1.stack.Stack stack

    /** Konfigurations-Objekt */
    ConfigObject config

    /** Stoppen der Threads wenn false */
    Boolean run = true

    /** Tabelle der IP-Adressen und UDP-Ports der Nachbarrouter */
    /*  z.B. [["1.2.3.4", 11],["5,6,7.8", 20]]
     */
    List<List> neighborTable

    /** Eine Arbeitskopie der Routingtabelle der Netzwerkschicht */
    List routingTable

    /** Nachbartabellenkopie mit Zaehltimer fuer aktualisierung */
    List<List> counterTable

    /** counter zum Runterzählen der Einträg*/
    int counter = 5

    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    //------------------------------------------------------------------------------
    /**
     * Start der Anwendung
     */
    static void main(String[] args) {
        // Router-Klasse instanziieren
        Router1 application = new Router1()
        // und starten
        application.router()
    }
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Einfacher IP-v4-Forwarder.<br/>
     * Ist so schon funktiionsfähig, da die Wegewahl im Netzwerkstack erfolgt<br/>
     * Hier wird im Laufe des Versuchs ein Routing-Protokoll implementiert.
     */
    void router() {

        // Konfiguration holen
        config = Utils.getConfig("experiment1", "router1")

        // ------------------------------------------------------------

        // Netzwerkstack initialisieren
        stack = new experiments.experiment1.stack.Stack()
        stack.start(config)

        // ------------------------------------------------------------

        // Thread zum Empfang von Routinginformationen erzeugen
        Thread.start{receiveFromNeigbor()}

        // ------------------------------------------------------------

        Utils.writeLog("Router1", "router1", "startet", 1)

        while (run) {
            // Periodisches Versenden von Routinginformationen
            sendPeriodical()
            sleep(config.periodRInfo)
        }
    }

    // ------------------------------------------------------------

    /**
     * Wartet auf Empfang von Routinginformationen
     *
     */
    void receiveFromNeigbor() {
        /** IP-Adresse des Nachbarrouters */
        String iPAddr

        /** UDP-Portnummer des Nachbarrouters */
        int port

        /** Empfangene Routinginformationen */
        String rInfo

        /** neue Liste mit Daten*/
        String[] newRInfo = new String()

        /** neue routing Tabelle */
        List rt

        /** counter für newRInfo */
        int c = 0

        /** linkPort zur Route */
        String linkPort = "lp1"

        /** routingIP für die Tabelle */
        String routingIp

        // Auf UDP-Empfang warten
        (iPAddr, port, rInfo) = stack.udpReceive()

        //neue Routinginformationen "portionieren" per .split()
        newRInfo = rInfo.split()

        // Jetzt aktuelle Routingtablle holen:
        rt = stack.getRoutingTable()

        //Wahrheitswert ob Routingtabellen-Eintrag bereits existiert
        boolean exists = false

        List counterTable = rt.collectNested{it}
        for(List entry in counterTable){
            entry.add(counter)
        }

        new Timer().schedule({
            for(int j = 3; j < counterTable.size(); j++){
                //counter um eins runtersetzen
                Object k
                k = counterTable[j][4]
                k--
                counterTable[j][4] = k
                //falls counter == 0 löschen der Einträge
                if(counterTable[j][4] == 0){
                    counterTable.remove(j)
                    rt.remove(j)
                }
            }

        }as TimerTask,10000,1000)

        List entryx
        // Routingtabelleneinträge durchsuchen
        entryx = rt.find { entry ->
            // Ziel-Ip-Adresse UND Netzpräfix == Zieladresse ?
            Utils.getNetworkId(iPAddr, entry[1] as String) == entry[0]
        }
        //linkPort und routingIp bestimmen
        linkPort = entryx[3]
        routingIp = entryx[2]

        //Tabelle ergänzen
        while(c<=newRInfo.length) {
            //schauen ob Eintrag bereits in Routingtabelle vorhanden
            for (int i = 0; i < rt.size(); i++) {
                if (rt[i][0] == newRInfo[c] && rt[i][2] == routingIp) {
                    Utils.writeLog("Router1", "routing", "Eintrag bereits vorhanden", 1)
                    counterTable[i][4] = counter
                    exists = true
                    break
                }
            }
            //Eintrag hinzufügen
            if(exists == false) {
                rt.add([newRInfo[c], newRInfo[c + 1], routingIp, linkPort])
                counterTable.add([newRInfo[c], newRInfo[c+1], routingIp, linkPort, counter])
                Utils.writeLog("Router1", "routing", "Schreibt neue Route", 1)
            }
            c + 4
            exists = false
        }
        // Routingtabelle an Vermittlungsschicht uebergeben:
        stack.setRoutingTable(rt)
        // und neue Routinginformationen verteilen:
        sendPeriodical()
    }

    // ------------------------------------------------------------

    /** Periodisches Senden der Routinginformationen */
    void sendPeriodical() {
        // Paket mit Routinginformationen packen
        // ... z.B.
        routingTable = stack.getRoutingTable()
        // extrahieren von Information, dann iInfo als !Zeichenkette! erzeugen ...
        String rInfo = ""

        for(int i = 0; i < routingTable.size(); i++){
            for(int j = 0; j<4; j++){
                rInfo += routingTable[i][j] + " "
            }
        }
        // Zum Senden uebergeben
        sendToNeigbors(rInfo)
    }

    // ------------------------------------------------------------

    /** Senden von Routinginformationen an alle Nachbarrouter
     *
     * @param rInfo - Routing-Informationen
     */

    void sendToNeigbors(String rInfo) {
        // rInfo an alle Nachbarrouter versenden
        for (List neigbor in neighborTable) {
            stack.udpSend(dstIpAddr: neigbor[0], dstPort: neigbor[1],
                    srcPort: config.ownPort, sdu: rInfo)
        }
    }
    //------------------------------------------------------------------------------

    void cleanRoutingTable(List routingTable, String IP){
        //TODO:cleanRouting Table via NachbarIpAdresse
        List entryx
        // Routingtabelleneinträge durchsuchen
        entryx = rt.find { entry ->
            // Ziel-Ip-Adresse UND Netzpräfix == Zieladresse ?
            Utils.getNetworkId(iPAddr, entry[1] as String) == entry[0]
        }
        //deadIp bestimmen
        deadIp = entryx[2]
        for(int i = 0; i<rt.size(); i++){
            if(rt[i][2] == deadIp){
                rt.remove(i)
            }
        }
    }
}

