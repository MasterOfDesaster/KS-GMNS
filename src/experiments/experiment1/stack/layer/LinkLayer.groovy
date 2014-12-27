package experiments.experiment1.stack.layer

import common.utils.Utils
import experiments.experiment1.stack.connector.Connector
import experiments.experiment1.stack.layer.idu.CL_IDU
import experiments.experiment1.stack.layer.idu.IL_IDU
import experiments.experiment1.stack.layer.idu.LC_IDU
import experiments.experiment1.stack.layer.idu.LI_IDU
import experiments.experiment1.stack.layer.pdu.AR_PDU
import experiments.experiment1.stack.layer.pdu.L_PDU

import java.util.concurrent.LinkedBlockingQueue as MQueue

/**
 * Die Link-Schicht (MAC-Framing und ARP)
 */
class LinkLayer {

    //========================================================================================================
    // Konstanten ANFANG
    //========================================================================================================

    final int ETHERTYPE_IP = 0x800
    final int ETHERTYPE_ARP = 0x806
    final int ARP_REQUEST = 1
    final int ARP_REPLY = 2

    // Broadcast MAC-Adresse
    final String broadcastMacAddress = "ff:ff:ff:ff:ff:ff"

    //========================================================================================================
    // Daten ANFANG
    //========================================================================================================

    //------------------------------------------------------------------------------
    /** Dient der Bestimmung der MAC-Adresse eines Geräts auf Grund seiner IP-Adresse */
    Map arpTable
    //------------------------------------------------------------------------------

    /** Stoppen der Threads wenn false */
    Boolean run = true

    /** Wenn true: warten auf ARP-Reply */
    Boolean waitARP = false

    /** von dieser Adresse wird ARP-Reply erwartet */
    String waitDstIpAddr

    //========================================================================================================

    /** message queues in Richtung der IP-Schicht */
    MQueue<IL_IDU> fromIpQ = new MQueue(Utils.MAXQUEUE)

    /** message queue von den Anschluessen */
    MQueue<CL_IDU> fromConnQ = new MQueue(Utils.MAXQUEUE)

    /** ARP message queue */
    MQueue<String> arpQ = new MQueue(1)

    //------------------------------------------------------------------------------

    /** message queues zu Nachbarschichten */
    MQueue<LI_IDU> toIpQ = null

    //========================================================================================================

    /** Die Anschluesse, adressierbar über den internen Namen des Anschlusses */
    Map<String, Connector> connectors

    //------------------------------------------------------------------------------
    /** Eigene IP-Adressen (eine IP-Adresse je Anschluss) */
    Map<String, String> ownIpAddrs = [:]

    /** Eigene Mac-Adressen (eine Mac-Adresse je Anschluss) */
    Map<String, String> ownMacAddrs = [:]

    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    /**
     * Empfängt Daten von der IP-Schicht und verarbeitet sie
     */
    void receive() {
        while (run) {
            /** IDU von Anschluessen */
            CL_IDU cl_idu

            /** IDU zu Anschluessen */
            LC_IDU lc_idu

            /** IDU zu IP */
            LI_IDU li_idu

            //------------------------------------------------------------------------------

            // blockierendes Lesen der Anschluesse
            cl_idu = fromConnQ.take()

            // Mac-Frame (L-PDU) entnehmen
            L_PDU macFrame = cl_idu.sdu as L_PDU

            Utils.writeLog("LinkLayer", "receive", "uebernimmt  von Anschluss ${cl_idu.lpName}: ${cl_idu}", 5)

            // IP-PDU behandeln:

            // IDU erzeugen
            li_idu = new LI_IDU()
            li_idu.lpName = cl_idu.lpName
            li_idu.sdu = macFrame.sdu

            // entweder:

            // IDU an IP uebergeben
            toIpQ.put(li_idu)

            // oder besser:

//            // Ist es eine eigene MAC-Adresse oder ein MAC-Broadcast ?
//            if (macFrame.dstMacAddr == ??? ||
//                    macFrame.dstMacAddr == ???) {
//                // Ja
//                // Frame-Typ untersuchen
//                switch (macFrame.type) {
//                    case ???:
//                        // IP-PDU behandeln:
//
//                        // IDU erzeugen
//                        li_idu = new LI_IDU()
//                        li_idu.lpName = cl_idu.lpName
//                        li_idu.sdu = macFrame.sdu
//
//                        // IDU an IP uebergeben
//                        toIpQ.put(li_idu)
//                        break
//
//                    case ???:
//                        // ARP-PDU behandeln:
////                        AR_PDU ar_pdu = macFrame.sdu as AR_PDU
////
////                        switch (ar_pdu.operation) {
////                            case ARP_REPLY:
////                                // Warten auf ARP-Reply von abgefragtem Geraet
////                                if (waitARP && waitDstIpAddr == ar_pdu.senderProtoAddr) {
////                                    waitARP = false
////
////                                    // Gesuchte MAC-Adresse uebernehmen
////                                    String macAddr = ???
////
////                                    Utils.writeLog("LinkLayer", "receive", "empfaengt ARP-Reply von ${ar_pdu.senderProtoAddr}: ${macAddr}", 5)
////
////                                    // MAC-Adresse an wartenden Sender-Thread uebergeben
////                                    arpQ.put(macAddr)
////                                }
////                                break
////
////                            case ARP_REQUEST:
////                                // Wird eigene MAC-Adresse abgefragt?
////                                if (ar_pdu.targetProtoAddr == ???) {
////                                    // Ja
////                                    // ARP-Reply senden
////
////                                    Utils.writeLog("LinkLayer", "receive", "empfaengt ARP-Request und sendet Reply", 5)
////
////                                    ar_pdu.operation = ARP_REPLY
////                                    ar_pdu.targetProtoAddr = ??? // IP-Adresse des Ziels
////                                    ar_pdu.targetHardAddr = ??? // MAC-Zieladresse des Ziels
////
////                                    Connector connector = connectors[cl_idu.lpName]
////                                    ar_pdu.senderProtoAddr = ??? // Eigene IP-Adresse
////                                    ar_pdu.senderHardAddr = connector.getMacAddr() // Eigene MAC-Adresse
////
////                                    macFrame.dstMacAddr = ??? // MAC-Zieladresse
////                                    macFrame.srcMacAddr = ar_pdu.senderHardAddr
////                                    macFrame.sdu = ar_pdu
////                                    macFrame.type = ??? // Typfeld
////
////                                    // MAC-Frame mit ARP-PDU an Anschluss uebergeben
////                                    // IDU erzeugen
////                                    lc_idu = new LC_IDU()
////                                    lc_idu.sdu = macFrame
////                                    connector.send(lc_idu)
////                                }
////                                break
////                        }
//                        break
//                }
//            }
        }
    }

    //------------------------------------------------------------------------------

    /**
     * Holt Daten von der IP-Schicht und uebergibt sie an einen Anschluss
     */
    void send() {

        /** Name des Linkports */
        String lpName

        /** IDU von IP */
        IL_IDU il_idu

        /** IDU zu Anschluessen */
        LC_IDU lc_idu

        /** Ein MAC-Frame */
        L_PDU macFrame

        /** Der zu verwendende Anschluss */
        Connector connector

        while (run) {
            // Blockierendes Lesen von IP-Schicht
            il_idu = fromIpQ.take()

            Utils.writeLog("LinkLayer", "send", "uebernimmt  von IP: ${il_idu}", 5)

            // Namen des Linkports entnehmen
            lpName = il_idu.lpName
            // Anschluss bestimmen
            connector = connectors[lpName]

            // MAC-Frame erzeugen
            macFrame = new L_PDU()
            // MAC-Adresse des Anschlusses holen
            macFrame.srcMacAddr = connector.getMacAddr()

            // IDU zu Anschluss erzeugen
            lc_idu = new LC_IDU()
            lc_idu.sdu = macFrame // L_PDU eintragen

            // entweder:

            // für alle Ziele gleiche MAC-Adresse eintragen
//            macFrame.dstMacAddr = "00:00:00:00:00:00"

            // oder besser:

            // Entnahme der MAC-Adresse eines Ziels im LAN aus einer Tabelle
            // aufgrund der IP-Adresse des Ziels; die Tabelle wird manuell verwaltet
//            macFrame.dstMacAddr = arpTable[il_idu.nextHopAddr]

            // oder besser:

            // Die MAC-Adresse des Ziel wird aus einer Tabelle entnommen deren Inhalt per ARP
            // (Address Resolution Protocol) dynamisch bestimmt wird.
            // Wird kein Eintrag gefunden -> null (siehe "?."-Operator)
macFrame?.dstMacAddr = arpTable[il_idu.nextHopAddr]

// Wurde die MAC-Adresse fuer das naechste Ziel in der ARP-Tabelle gefunden?
if (!macFrame.dstMacAddr) {
// Nein -> ARP verwenden

// Warten auf ARP-Reply, wird in "receive" geaendert
waitARP = true
waitDstIpAddr = il_idu.nextHopAddr

// ARP_PDU erzeugen
AR_PDU ar_pdu = new AR_PDU()
ar_pdu.operation = 1
ar_pdu.senderProtoAddr = ownIpAddrs[lpName]// IP-Adresse des Senders
ar_pdu.senderHardAddr = connector.macAddr //macFrame.srcMacAddr// MAC-Adresse des Senders

ar_pdu.targetProtoAddr = waitDstIpAddr// IP-Adresse des ARP-Ziels
ar_pdu.targetHardAddr = broadcastMacAddress // Gesuchter Eintrag

macFrame.dstMacAddr = broadcastMacAddress // Broadcast-MAC-Zieladresse
macFrame.sdu = ar_pdu
macFrame.type = ETHERTYPE_ARP // Typfeld

Utils.writeLog("LinkLayer", "send", "sendet ARP-Request: ${lc_idu}", 5)

// MAC_Frame mit ARP-PDU an Anschluss uebergeben
connector.send(lc_idu)

// Warten auf ARP-Response, receive-Thread uebergibt die MAC-Adresse aus einem
// ARP-Reply ueber "arpQ"
// Der Sendethread blockiert hier: "quick and dirty"
// Besser waere es einen eigenen Thread auszufueren
String nextMacAddr = arpQ.take()

// Arp-Tabelle aktualisieren
arpTable[waitDstIpAddr] = nextMacAddr

// MAC-Ziel-Adresse in MAC-Frame einsetzen
macFrame.dstMacAddr = nextMacAddr
}

macFrame.sdu = il_idu.sdu // PDU entnehmen
macFrame.type = ETHERTYPE_IP // Typfeld

Utils.writeLog("LinkLayer", "send", "uebergibt an Anschluss ${lpName}: ${lc_idu}", 5)

// Daten an Anschluss uebergeben
connector.send(lc_idu)
        }
    }

    //========================================================================================================

    /**
     * Liefert die Message-Queue in Senderichtung
     * @return
     */
    MQueue<IL_IDU> getFromIpQ() {
        return fromIpQ
    }

    //------------------------------------------------------------------------------

    /**
     * Liefert die Message-Queue in Empfangsrichtung
     * @return
     */
    MQueue<CL_IDU> getFromConnQ() {
        return fromConnQ
    }

    //------------------------------------------------------------------------------

    /**
     * Starten der Schicht
     * @param toIpQ
     * @param connectors
     * @param config
     */
    void start(MQueue<LI_IDU> toIpQ, Map<String, Connector> connectors, ConfigObject config) {

        // Parameteruebernahme
        this.connectors = connectors
        this.toIpQ = toIpQ
        this.arpTable = config.arpTable

        // Tabelle der IP-Adressen erzeugen
        config.networkConnectors.each { conn ->
            ownIpAddrs[conn.lpName] = conn.ipAddr
            ownMacAddrs[conn.lpName] = conn.macAddr
        }

        /** Start der Threads */
        Thread.start { receive() }
        Thread.start { send() }
    }

    //------------------------------------------------------------------------------

    /**
     *  Stoppen der Schicht
     */
    void stop() {
        run = false
    }
}
