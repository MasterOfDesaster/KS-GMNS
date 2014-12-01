package experiments.experiment1.stack.layer

import common.utils.Utils
import experiments.experiment1.stack.layer.idu.IL_IDU
import experiments.experiment1.stack.layer.idu.IT_IDU
import experiments.experiment1.stack.layer.idu.IU_IDU
import experiments.experiment1.stack.layer.idu.LI_IDU
import experiments.experiment1.stack.layer.idu.TRI_IDU
import experiments.experiment1.stack.layer.pdu.I_PDU

import java.util.concurrent.LinkedBlockingQueue as MQueue

/**
 * Die IPv4-Schicht
 */
class IpLayer {
    //========================================================================================================
    // Konstanten ANFANG
    //========================================================================================================

    /** Protokolle übergeben ihren Typ */
    public static final int PROTO_TCP = 6
    public static final int PROTO_UDP = 17

    //========================================================================================================
    // Daten ANFANG
    //========================================================================================================

    /** Stoppen der Threads wenn false */
    Boolean run = true

    //========================================================================================================

    // Message queues in Richtung der TCP- UDP-Schicht
    /** Message-Queue von TCP/UDP zu IP */
    MQueue<TRI_IDU> fromTcpUdpQ = new MQueue(Utils.MAXQUEUE)
    /** Message-Queue von Link zu IP */
    MQueue<LI_IDU> fromLinkQ = new MQueue(Utils.MAXQUEUE)

    //------------------------------------------------------------------------------

    // message queues zu Nachbarschichten
    /** Message-Queues von IP zu TCP und UDP */
    Map<Integer, MQueue> toTcpUdpQ = [(PROTO_TCP): null, (PROTO_UDP): null]
    /** Message-Queues von IP zu Link */
    MQueue<IL_IDU> toLinkQ = null

    //========================================================================================================

    /** Routing-Tabelle */
    List<List> routingTable = []

    /** Zur Seuerung des Zugriffs auf die Routing-Tabelle (Thread-Synchronisation!) */
    final Object cntrlRT = new Object()

    /** Eigene IP-Adressen (eine IPv4-Adresse je Anschluss) */
    Map<String, String> ownIpAddrs = [:]

    /** IP-Adresse des nächsten Gerätes auf der Route bzw. des Ziels */
    String defaultRouter

    /** Einheitliche Subnetz-Maske */
    String globalNetMask

    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    /**
     * Empfängt Daten von der Link-Schicht
     */
    void receive() {

        // IDU von Link
        LI_IDU li_idu

        // IDU zu TVP oder UDP
        IT_IDU it_idu
        IU_IDU iu_idu

        // IP-PDU
        I_PDU i_pdu

        // Name des zu verwendenden Link-Ports
        String linkPortName

        // IP-Adresse des nächsten Gerätes ("hops") auf der Route
        String nextHopAddr

        while (run) {
            // blockierendes Lesen von Link-Schicht
            li_idu = fromLinkQ.take()

            // IP-PDU entnehmen
            i_pdu = li_idu.sdu as I_PDU

            Utils.writeLog("IpLayer", "receive", "uebernimmt von Link: ${li_idu}", 4)

            // Ist das Paket an eine der eigenen IP-Adressen adressiert
            // Auf IP-Broadcast wird hier nicht getestet
            if (i_pdu.dstIpAddr == ownIpAddrs[li_idu.lpName]) {
                // Ja
                switch (i_pdu.protocol) {
                    case PROTO_TCP:
                        // Daten an TCP uebergeben
                        it_idu = new IT_IDU()
                        it_idu.sdu = i_pdu.sdu
                        it_idu.srcIpAddr = i_pdu.srcIpAddr
                        toTcpUdpQ[PROTO_TCP].put(it_idu)
                        break

                    case PROTO_UDP:
                        // Daten an UDP uebergeben
                        iu_idu = new IU_IDU()
                        iu_idu.sdu = i_pdu.sdu
                        iu_idu.srcIpAddr = i_pdu.srcIpAddr
                        toTcpUdpQ[PROTO_UDP].put(iu_idu)
                        break
                }
            } else {
//                // Nein, ist nicht die eigene IP-Adresse, also weiterleiten
//                // Nächsten Hop suchen
//                (linkPortName, nextHopAddr) = findNextHop(i_pdu.dstIpAddr)
//
//                // Nächsten Hop gefunden?
//                if (linkPortName && nextHopAddr) {
//                    // Ja
//                    // IDU zu Link-Schicht erzeugen
//                    IL_IDU il_idu = new IL_IDU()
//
//                    // i_pdu eintragen
//                    il_idu.sdu = i_pdu
//
//                    // An Link-Schicht uebergeben
//                    il_idu.lpName = linkPortName
//
//                    // Ist es eine direkte Route?
//                    if (nextHopAddr == ownIpAddrs[linkPortName])
//                        // Ja
//                        il_idu.nextHopAddr = ???
//                    else
//                        // Nein
//                        il_idu.nextHopAddr = ???
//
//                    Utils.writeLog("IpLayer", "receive", "forwarding: ${li_idu}", 4)
//
//                    // Daten an Link-Schicht uebergeben
//                    ???
//                }
//                else {
//                    // Nein
//                    Utils.writeLog("IpLayer", "receive", "keine Route gefunden fuer: ${linkPortName}, ${i_pdu.dstIpAddr}", 4)
//                }
            }
        }
    }

    //------------------------------------------------------------------------------

    /**
     * Übergibt Daten von der TCP- oder UDP-Schicht an die Link-Schicht
     */
    void send() {


        // IDU zu Link
        IL_IDU il_idu

        // IP-PDU
        I_PDU i_pdu

        // Name des zu verwendenden Link-Ports
        String linkPortName

        // I-Adresse des nächsten Gerätes ("hops") auf der Route
        String nextHopAddr

        while (run) {

            // IDU von TCP oder UDP
            TRI_IDU ti_idu

            // blockierendes Lesen von TCP oder UDP
            ti_idu = fromTcpUdpQ.take()

            Utils.writeLog("IpLayer", "send", "uebernimmt von TCP/UDP: ${ti_idu}", 4)

            linkPortName = "lp1"
            nextHopAddr = ownIpAddrs[linkPortName]

//            // Nächstes Gerät (next hop) auf dem Pfad zum Zielgerät suchen
//            (linkPortName, nextHopAddr) = findNextHop(ti_idu.dstIpAddr)

            // Nächsten Hop gefunden?
            if (linkPortName && nextHopAddr) {
                // Ja
                // IP-PDU erzeugen
                i_pdu = new I_PDU()

                // Anwendungsdaten übernehmen
                i_pdu.sdu = ti_idu.sdu

                // Protokolldaten eintragen
                i_pdu.dstIpAddr = ti_idu.dstIpAddr

                // Die diesem Link-Port zugeordnete Ipv4-Adresse eintragen
                i_pdu.srcIpAddr = ownIpAddrs[linkPortName]

                i_pdu.protocol = ti_idu.protocol

                // IDU zu Link erzeugen
                il_idu = new IL_IDU()

                // i_pdu eintragen
                il_idu.sdu = i_pdu

                // An Link-Schicht uebergeben
                il_idu.lpName = linkPortName

                // Ist es eine direkte Route?
//                if (nextHopAddr == ownIpAddrs[linkPortName])
//                    // Ja
//                    il_idu.nextHopAddr = ???
//                else
//                    // Nein
//                    il_idu.nextHopAddr = ???
//
                // IP-Adresse des naechsten Geraetes auf dem Pfad zum Ziel eintragen
                il_idu.nextHopAddr = nextHopAddr

                // Daten an Link-Schicht uebergeben
                toLinkQ.put(il_idu)
            } else {
                // Nein
                Utils.writeLog("IpLayer", "send", "keine Route gefunden fuer: ${linkPortName}, ${ti_idu.dstIpAddr}", 4)
            }
        }
    }

    //------------------------------------------------------------------------------

    /**
     * Sucht den Ausgangs-Anschluss (Link-Port) passend zur Route.<br/>
     * Dabei wird die IP-Adresse des nächsten Ziels und die Link-Port-Name bestimmt.
     * Es kann die IP-Adresse des eigentlichen Ziels oder die des nächsten Routers auf dem Weg zum Ziel sein.
     * Die Link-Schicht benötigt sie, um die MAC-Adresse des nächsten Ziels zu bestimmen
     * @param dstIpAddr Ziel-IPv4-Adresse
     * @return Liste: IP-v4-Adresse des nächsten Gerätes (next hop) und Link-Port-Name oder "null"
     */
    List findNextHop(String dstIpAddr) {
        List entryx

        // Routingtabelleneinträge durchsuchen
        entryx = routingTable.find { entry ->
            // Ziel-Ip-Adresse UND Netzpräfix == Zieladresse ?
            Utils.getNetworkId(dstIpAddr, entry[1] as String) == entry[0]
        }

        // Ausgang gefunden?
        if (entryx) {
            // Ja
            return [entryx[3], entryx[2]]
        }
        else
            // Nein
            return [null,null]
    }

    //========================================================================================================

    /**
     * Routing-Tablle holen, um z.B. durch Routingprotokolle modifiziert zu werden.
     * @return
     */
    synchronized List<List> getRoutingTable() {
        // Kopie der Routing-Tablle
        List rt

        // Zugriff vor anderen Threads schützen
        synchronized(cntrlRT) {
            rt = routingTable.clone() as List<List>
        }
        return rt
    }

    //------------------------------------------------------------------------------

    /**
     * Routing-Tabelle setzen, z.B. nach Modifizierung durch Routingprotokoll <br/>
     * oder zur Initialisierung
     * @param table
     */
    synchronized void setRoutingTable(List routingTable) {
        // Zugriff vor anderen Threads schützen
        synchronized(cntrlRT) {
            this.routingTable = routingTable.clone() as List<List>
        }
    }

    //------------------------------------------------------------------------------

    /**
     * Liefert die Message-Queue in Senderichtung
     * @return fromTcpUdpQ
     */
    MQueue<TRI_IDU> getFromTcpUdpQ() {
        return fromTcpUdpQ
    }

    //------------------------------------------------------------------------------

    /**
     * Liefert die Message-Queue in Empfangsrichtung
     * @return fromLinkQ
     */
    MQueue<LI_IDU> getFromLinkQ() {
        return fromLinkQ
    }

    //------------------------------------------------------------------------------

    /**
     * Starten der Schicht
     * @param toTcpQ
     * @param toLinkQ
     */
    void start(Map toTcpUdpQ, MQueue<IL_IDU> toLinkQ, ConfigObject config) {

        this.toTcpUdpQ = toTcpUdpQ
        this.toLinkQ = toLinkQ
        defaultRouter = config.defaultRouter
        globalNetMask = config.globalNetMask

        // Tabelle der IP-Adressen erzeugen
        config.networkConnectors.each { conn ->
            ownIpAddrs[conn.lpName] = conn.ipAddr
        }

        // Start der Threads
        Thread.start { receive() }
        Thread.start { send() }
    }

    //------------------------------------------------------------------------------


    /**
     * Stoppen der Schicht
     */
    void stop() {
        run = false
    }
}
