package experiments.experiment1.hosts.router1

import common.utils.Utils

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

    /** Tabelle um zu merken wenn ein Router nicht mehr mit einem spricht*/
    List<List> tickTable

    /** Eine Arbeitskopie der Routingtabelle der Netzwerkschicht */
    List routingTable

    /** Boolean um abzufragen ob noch irgendjemand mit dir redet*/
    Boolean stillThere

    /** Routingtabelle fuer reset*/
    List<List> resetRT
    List<List> newRT

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
        neighborTable=config.neighborTable
        resetRT=config.routingTable
        stillThere=false
        if(neighborTable==null){
            neighborTable=[]
        }

        // tickTable initialisieren
        tickTable = neighborTable.collectNested{it}
        for( List entry in tickTable) {
            entry.add(0)
        }

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

        new Timer().schedule({
            if(stillThere==false){

                neighborTable.clear()
                tickTable.clear()
                stack.setRoutingTable(resetRT)
            }else{
                stillThere=false
            }
        } as TimerTask,10000,8000)

        while(1) {

            int ticklimit=neighborTable.size()

            // Auf UDP-Empfang warten
            (iPAddr, port, rInfo) = stack.udpReceive()
            stillThere=true
            Utils.writeLog("Router1", "empfängt von " + iPAddr, "Routinginformationen", 1)

            List rInfoList = rInfo.tokenize("|")
            List<List> rInfoTable = []
            for (String part in rInfoList) {
                rInfoTable.add(part.tokenize(", "))

            }

            Utils.writeLog("Router1", "schreibt", "neue Routinginformationen", 1)

            //Vorraussetzung: Nur Nachbarrouter sprechen untereinander

            // Aktualisieren von NeighborTable

            if(!neighborTable.contains([iPAddr,port])) {
                //Neuer Nachbar!!

                neighborTable.add([iPAddr,port])
                tickTable.add([iPAddr,port, 0])
            }
            List<List> rem = []
            tickTable.each { List p ->
                if(p[0] == iPAddr) {
                    p[2] = 0
                }else if(p[2] < ticklimit) {
                    p[2]++
                } else{
                    rem.add(p.clone())
                }
            }
            rem.each{ List p ->
                tickTable.remove(p)
                def List q=p.clone()
                q.remove(2)
                neighborTable.remove(q)
            }

            List<List> rt= stack.getRoutingTable()

            List<List> tbd = [] //Liste mit zu löschenden Einträgen

            for( List eRT in rt){
                for(List eRI in rInfoTable) {

                    // Eintrag in eigener Liste vorhanden
                    if(eRT[0] == eRI[0]) {

                        //Beide Eintraege werden per Metrik Verglichen
                        if(eRT[4].toInteger() > (1 + eRI[4].toInteger()) ){
                            //ubernehme eintrag

                            eRT[2] = iPAddr //Nachbarrouter wird nextHop
                            String linkport
                            String nexthop
                            (linkport,nexthop) = stack.ip.findNextHop(iPAddr)
                            eRT[3]=linkport

                            //Metrik errechnen
                            eRT[4] = (eRI[4].toInteger() + 1).toString()
                        }
                        //eintrag im spaeter aus rInfoTable loeschen

                        tbd.add(eRI.clone())
                    }

                }

            }

            tbd.each{
                List p -> rInfoTable.remove(p)
            }

            //Noch neue information in rInfo
            if(!rInfoTable.isEmpty()){
                String linkport
                String nexthop
                for(List eRI in rInfoTable){
                    (linkport,nexthop) = stack.ip.findNextHop(iPAddr)
                    rt.add([eRI[0],eRI[1],iPAddr,linkport,(eRI[4].toInteger() + 1).toString()])
                }

            }
            //NOCH ZU DEN ANDEREN ROOTERN KOPIEREN
            //nicht-mehr-Nachbarn rauslöschen
            newRT = []
            Boolean check
            List<String> neighbors = []
            neighbors = neighborTable.collect { it[0] }

            stack.ip.ownIpAddrs.each {
                key, value -> neighbors.add(value)
            }

            for (List eRT in rt) {
                //suche in Neighbortable
                check = false
                for (String s in neighbors) {

                    if (eRT[2] == s) {
                        check = true
                    }

                }

                if (check) {
                    newRT.add(eRT)
                }


            }


            stack.setRoutingTable(newRT)


        }
    }

    // ------------------------------------------------------------

    /** Periodisches Senden der Routinginformationen */
    void sendPeriodical() {
        // Paket mit Routinginformationen packen
        routingTable = stack.getRoutingTable()
        // extrahieren von Information, dann iInfo als !Zeichenkette! erzeugen ...
        String rInfo = ""

        for(int i = 0; i < routingTable.size(); i++){
            for(int j = 0; j<5; j++) {
                if (j == 4) {
                    rInfo += routingTable[i][j] + "|"
                } else {
                    rInfo += routingTable[i][j] + ", "

                }
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
            Utils.writeLog("Router1", "sendet", "Routinginformationen an: "+neigbor[0]+", "+neigbor[1], 1)
        }
    }
    //------------------------------------------------------------------------------
}

