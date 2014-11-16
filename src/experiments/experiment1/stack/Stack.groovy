package experiments.experiment1.stack

import common.utils.Utils
import experiments.experiment1.stack.connector.Connector
import experiments.experiment1.stack.connector.ConnectorToEthernet
import experiments.experiment1.stack.connector.ConnectorToVirtual
import experiments.experiment1.stack.layer.IpLayer
import experiments.experiment1.stack.layer.LinkLayer
import experiments.experiment1.stack.layer.TcpLayer
import experiments.experiment1.stack.layer.UdpLayer
import experiments.experiment1.stack.layer.idu.AT_IDU
import experiments.experiment1.stack.layer.idu.AU_IDU
import experiments.experiment1.stack.layer.idu.TA_IDU
import experiments.experiment1.stack.layer.idu.UA_IDU

import java.util.concurrent.LinkedBlockingQueue as MQueue
import java.util.concurrent.TimeUnit

//========================================================================================================
// Importe ANFANG
//========================================================================================================
//========================================================================================================
// Importe ENDE
//========================================================================================================

//========================================================================================================
// Stack-Klasse ANFANG
//========================================================================================================

/**
 * Der Netzwerkstack.
 * <p/>
 * Beinhaltet Fragmente der Protokolle TCP, UDP, IP und Link (IEEE 802.3 ("Ethernet-MAC")).<br/>
 * TCP - Transport Control Protocol, UDP - User Datagram Protocol, IP - Internet Protocol,<br/>
 * IEEE - Institut of Electrical and Electronics Engineers,<br/>
 * Ethernet - Produktbezeichnung eines häufig verwendeten LAN,<br/>
 * LAN - Local Area Network, MAC - Medium Access Control<br/>
 * <p/>
 * Für weitere Erläuterungen siehe die Kommentare im Quelltext.
 * <p/>
 */
public class Stack {

    //========================================================================================================
    // NETZWERKADAPTER ANFANG
    //========================================================================================================

    /**
     * Eine Map von Netzwerkadaptern. <br/>
     * Der Schlüssel (Name) wird in der Routingtabelle hinterlegt und von der Link-Schicht beim Senden zur
     * Auswahl des Adapters verwendet
     */
    Map<String,Connector> connectors = [:]

    //========================================================================================================
    // NETZWERKADAPTER ENDE
    //========================================================================================================


    //========================================================================================================
    // Vereinbarungen ANFANG
    //========================================================================================================

    //----------------------------------------------------------
    // Protokoll-Schichten
    /** TCP-Schicht */
    TcpLayer tcp = new TcpLayer()
    /** UDP-Schicht */
    UdpLayer udp = new UdpLayer()
    /** IP-Schicht */
    IpLayer ip = new IpLayer()
    /** Link-Schicht */
    LinkLayer link = new LinkLayer()

    //----------------------------------------------------------

    /** Message-Queue von TCP zur Anwendung */
    MQueue<TA_IDU> fromTcpQ = new MQueue(Utils.MAXQUEUE)

    /** Message-Queue zu TCP */
    MQueue<AT_IDU> toTcpQ

    //----------------------------------------------------------

    /** Message-Queue von UDP zur Anwendung */
    MQueue<UA_IDU> fromUdpQ = new MQueue(Utils.MAXQUEUE)

    /** Message-Queue zu UDP */
    MQueue<AU_IDU> toUdpQ

    //========================================================================================================
    // Vereinbarungen ENDE
    //========================================================================================================

    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    //========================================================================================================
    // Steuerung des Stacks ANFANG
    //========================================================================================================

    //----------------------------------------------------------
    /**
     * Starten des Netzwerkstacks
     * @param config Konfiguration aus Datei "config"
     */
    void start(ConfigObject config) {
        // Message Queue Anschluesse -> Link-Schicht
        MQueue conQ = link.getFromConnQ()

        // -----------------------------------------------------------------------------------------------
        // Netzwerkanschluesse initialiiseren

        config.networkConnectors.each { con ->
            if (con.virtual) {

                // Anschluss in einen virtuellen HUB (virtuelles LAN)
                connectors[con.lpName] = new ConnectorToVirtual(con.lpName, con.link, con.connector, con.macAddr, conQ)
            }
            else {
                // Anschluss in ein reales LAN
                connectors[con.lpName] = new ConnectorToEthernet(con.lpName, con.deviceName, con.macAddr, con.recvFilter, conQ)
            }
        }

        // Netzwerkanschluesse starten
        connectors.values().each {
            it.start()
        }

        // -----------------------------------------------------------------------------------------------

        // Protokollschichten verbinden
        // Link-Schicht
        link.start(ip.getFromLinkQ(), connectors, config)

        // IP-Schicht
        ip.setRoutingTable(config.routingTable)
        ip.start([(IpLayer.PROTO_TCP): tcp.getFromIpQ(), (IpLayer.PROTO_UDP): udp.getFromIpQ()], link.getFromIpQ(), config)

        // TCP-Schicht wird mit Stack verbunden
        tcp.start(fromTcpQ, ip.getFromTcpUdpQ(), config)
        toTcpQ = tcp.getFromAppQ()

        // UDP-Schicht wird mit Stack verbunden
        udp.start(fromUdpQ, ip.getFromTcpUdpQ(), config)
        toUdpQ = udp.getFromAppQ()
    }
    //----------------------------------------------------------

    //----------------------------------------------------------
    /**
     * Stoppen des Netzwerkstacks
     */
    void stop() {

        // Schichten stoppen
        tcp.stop()
        udp.stop()
        ip.stop()
        link.stop()

        // Netzwerkanschluesse stoppen
        connectors.values().each {it.stop()}
    }
    //----------------------------------------------------------

    //========================================================================================================
    // Steuerung des Stacks ENDE
    //========================================================================================================

    //========================================================================================================
    // Kommunikation mit den Schichten ANFANG
    //========================================================================================================

    //=== TCP-Schicht ==========================================
    //----------------------------------------------------------
    /**
     * Schnittstelle zur TCP-Schicht
     * @param Map idu: keys: connId, sdu
     */
    void tcpSend(Map idu) {

        AT_IDU at_idu = new AT_IDU()

        at_idu.command = TcpLayer.DATA
        at_idu.sdu = idu.sdu

        toTcpQ.put(at_idu)
    }
    //----------------------------------------------------------

    //----------------------------------------------------------
    /**
     * Schnittstelle von TCP-Schicht
     * @param Map idu: keys: connId  (hier nicht verwendet)
     * @return tidu keys: connId, sdu; sdu ist "null",
            falls innerhalb der angegebenen Zeit kein Element aus der Warteschlange entnommen wurde
     */
    Map tcpReceive(Map idu) {
        Map tidu = [:]

        // Blockierendes Empfangen von TCP,
        TA_IDU ta_idu = fromTcpQ.poll(Utils.sec1, TimeUnit.MILLISECONDS)
        // Timeout aufgetreten?
        if (ta_idu) {
            // Nein
            tidu = [connId: ta_idu.connId, sdu: ta_idu.sdu]
        }
        else {
            // Ja
            tidu = [connId: idu.connId, sdu: null]
        }
        return tidu
    }
    //----------------------------------------------------------

    //----------------------------------------------------------
    /**
     * TCP-verbindung öffnen
     * @param Map idu: keys: dstIpAddr, dstPort
     * @return liefert ID der Verbindung oder 0
     */
    int tcpOpen(Map idu) {
        return tcp.open(idu)
    }
    //----------------------------------------------------------

    //----------------------------------------------------------
    /**
     * TCP-Verbindung schliessen
     * @param Map idu: keys: connId
     */
    void tcpClose(Map idu) {
        tcp.close(idu)
    }
    //----------------------------------------------------------

    //----------------------------------------------------------
    /**
     * Auf TCP-Verbindungsanforderung warten
     * @return [Verbindungs-ID, IP-Adresse des Anfordernden, Portnummer des Anfordernden]
     */
    Map tcpListen() {
        return tcp.listen()
    }
    //----------------------------------------------------------

    //=== UDP-Schicht ==========================================
    //----------------------------------------------------------
    /**
     * Auf UDP-Empfang warten
     * @return [IP-Adresse des Absenders, Portnummer des Absenders, SDU]
     */
    List udpReceive() {
        // Warten auf Empfang
        List uidu = udp.receiving()

        return uidu
    }
    //----------------------------------------------------------

    //----------------------------------------------------------
    /**
     * Über UDP-Senden
     * @param AU_IDU
     */
    void udpSend(Map idu) {
        AU_IDU au_idu = new AU_IDU()

        au_idu.dstIpAddr = idu.dstIpAddr
        au_idu.dstPort = idu.dstPort
        au_idu.srcPort = idu.srcPort
        au_idu.sdu = idu.sdu

        toUdpQ.put(au_idu)
    }
    //----------------------------------------------------------

    //=== IP-Schicht ===========================================
    //----------------------------------------------------------
    /**
     * Routingtabelle von IP-Schicht lesen
     */
    List<List> getRoutingTable() {
        ip.getRoutingTable()
    }
    //----------------------------------------------------------

    //----------------------------------------------------------
    /**
     * Routingtabelle der IP-Schicht schreiben
     */
    void setRoutingTable(List<List> table) {
        ip.setRoutingTable(table)
    }
    //----------------------------------------------------------

    //========================================================================================================
    // Kommunikation mit den Schichten ENDE
    //========================================================================================================

    //========================================================================================================
    // Methoden ENDE
    //========================================================================================================
}

//========================================================================================================
// Stack-Klasse ENDE
//========================================================================================================
