package experiments.experiment1.stack.layer

import common.utils.Utils
import experiments.experiment1.stack.layer.idu.AU_IDU
import experiments.experiment1.stack.layer.idu.IU_IDU
import experiments.experiment1.stack.layer.idu.TRI_IDU
import experiments.experiment1.stack.layer.idu.UA_IDU
import experiments.experiment1.stack.layer.pdu.U_PDU

import java.util.concurrent.LinkedBlockingQueue as MQueue

//========================================================================================================
// UdpLayer-Klasse ANFANG
//========================================================================================================

/**
 * Die UDP-Protokollschicht
 */
class UdpLayer {

    //========================================================================================================
    // Daten ANFANG
    //========================================================================================================

    //------------------------------------------------------------------------------
    /** Stoppen der Threads wenn false */
    Boolean run = true
    //------------------------------------------------------------------------------

    //========================================================================================================

    //------------------------------------------------------------------------------
    /* message queues in Richtung der UDP-Schicht */
    /** message queue von Anwendung */
    MQueue<AU_IDU> fromAppQ = new MQueue(Utils.MAXQUEUE)
    /** message queue von IP-Schicht */
    MQueue<IU_IDU> fromIpQ = new MQueue(Utils.MAXQUEUE)
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /* message queues zu Nachbarschichten */
    /** message queue zu Anwendung */
    MQueue<UA_IDU> toAppQ = null
    /** message queue zur IP-schicht */
    MQueue<TRI_IDU> toIpQ = null
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /** Eigener Port */
    int ownPort

    /** IP_adresse des Absenders */
    String dstIpAddr

    /** Port des Absenders */
    int dstPort

    //------------------------------------------------------------------------------

    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    //------------------------------------------------------------------------------
    /**
     * Empfängt Daten von der IP-Schicht und verarbeitet sie
     */
    void receive() {

        /** IDU zu Anwendung */
        UA_IDU ua_idu

        /** IDU von IP */
        IU_IDU iu_idu

        /** UDP-PDU */
        U_PDU u_pdu

        //-------------------------------------------------------------------------

        while (run) {
            // blockierendes Lesen von IP-Schicht
            iu_idu = fromIpQ.take()

            // UDP-PDU entnehmen
            u_pdu = iu_idu.sdu as U_PDU

            Utils.writeLog("UdpLayer", "receive", "uebernimmt  von IP: ${iu_idu}", 3)


            //TODO: Hier z.B. noch auf richtigen Zielport testen
            // ...

            ua_idu = new UA_IDU()
            ua_idu.sdu = u_pdu.sdu
            ua_idu.srcIpAddr = iu_idu.srcIpAddr
            ua_idu.srcPort = u_pdu.srcPort

            // Daten an Anwendung uebergeben
            toAppQ.put(ua_idu)
        }
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Holt Daten von der Anwendung und uebergibt sie an die IP-Schicht
     */
    void send() {

        /** IDU von Anwendung */
        AU_IDU au_idu

        /** IDU zu IP */
        TRI_IDU ti_idu

        // UDP-PDU
        U_PDU u_pdu

        //-------------------------------------------------------------------------

        while (run) {

            // blockierendes Lesen von Anwendung
            au_idu = fromAppQ.take()

            Utils.writeLog("UdpLayer", "send", "uebernimmt  von Anwendung: ${au_idu}", 3)

            // UDP-PDU erzeugen
            u_pdu = new U_PDU()
            u_pdu.sdu = au_idu.sdu // Anwendungsdaten übernehmen
            u_pdu.dstPort = au_idu.dstPort // Ziel-Portnummer eintragen
            u_pdu.srcPort = ownPort // Quell-Portnummer eintragen

            // IDU zu IP erzeugen
            ti_idu = new TRI_IDU()
            ti_idu.sdu = u_pdu // U_PDU eintragen
            ti_idu.dstIpAddr = au_idu.dstIpAddr // Ziel-IP-Adresse eintragen
            ti_idu.protocol = IpLayer.PROTO_UDP // Absendendes Protokoll eintragen

            // Daten an IP-Schicht uebergeben
            toIpQ.put(ti_idu)
        }
    }

    //------------------------------------------------------------------------------

    //========================================================================================================
    // UDP-Schnittstelle ANFANG
    //========================================================================================================

    //------------------------------------------------------------------------------
    /**
     * Liefert eine ueber UDP uebertragene A_PDU
     * @return UA_IDU
     */
    List receiving() {

        Utils.writeLog("UdpLayer", "receiving", "warte auf Empfang an Port: ${ownPort}", 3)

        // Warten auf Empfang
        UA_IDU ua_idu = toAppQ.take()

        return [ua_idu.srcIpAddr, ua_idu.srcPort, ua_idu.sdu]
    }

    //------------------------------------------------------------------------------

    //========================================================================================================
    // UDP-Schnittstelle ENDE
    //========================================================================================================

    //------------------------------------------------------------------------------
    /**
     * Liefert die Message-Queue in Senderichtung
     * @return
     */
    MQueue<AU_IDU> getFromAppQ() {
        return fromAppQ
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Liefert die Message-Queue in Empfangsrichtung
     * @return
     */
    MQueue<IU_IDU> getFromIpQ() {
        return fromIpQ
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Starten der Schicht
     * @param toApplQ
     * @param toIpQ
     */
    void start(MQueue<UA_IDU> toAppQ, MQueue<TRI_IDU> toIpQ, ConfigObject config) {

        // Parameteruebernajme
        this.toAppQ = toAppQ
        this.toIpQ = toIpQ
        ownPort = config.ownPort

        /** Start der Threads */
        Thread.start { receive() }
        Thread.start { send() }
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Stoppen der Schicht
     */
    void stop() {
        run = false
    }

    //------------------------------------------------------------------------------
}
