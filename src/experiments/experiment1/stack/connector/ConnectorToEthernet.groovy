package experiments.experiment1.stack.connector

import common.utils.Utils
import experiments.experiment1.stack.layer.idu.CL_IDU
import experiments.experiment1.stack.layer.idu.LC_IDU
import experiments.experiment1.stack.layer.pdu.AR_PDU
import experiments.experiment1.stack.layer.pdu.I_PDU
import experiments.experiment1.stack.layer.pdu.L_PDU
import experiments.experiment1.stack.layer.pdu.T_PDU
import experiments.experiment1.stack.layer.pdu.U_PDU
import jpcap.JpcapCaptor
import jpcap.JpcapSender
import jpcap.PacketReceiver
import jpcap.packet.*

import java.util.concurrent.LinkedBlockingQueue as MQueue

/**
 *
 * Implementiert das Interface PacketReceiver, so das bei jedem empfangenen Ethernet-Frame die Methode
 * "{@link ConnectorToEthernet#receivePacket(Packet) receivePacket}" gerufen wird
 */
public class ConnectorToEthernet extends Connector implements PacketReceiver {

    //======================================================================================================Å==
    // Klassenvariablen ANFANG
    //========================================================================================================

    //========================================================================================================
    // Instanzvariablen ANFANG
    //========================================================================================================

    /** Repräsentiert das Netzwerk-Interface */
    jpcap.NetworkInterface device = null

    /** Der Empfänger für MAC-Frames */
    JpcapCaptor receiver = null

    /** Der Sender für MAC-Frames.<br/>
     * Um testen zu können wird "sender" typenlos definiert */
    def sender = null

    /** Ein Thread zum Empfang von MAC-Frames **/
    Thread receiverThread

    // Max. Puffergrösse
    private static int MAXBUFFER = 1400

    // eigene MAC-Adresse
    String macAddr

    // Broadcast-MAC-Adresse
    final String broadcastMacAddr = "ff:ff:ff:ff:ff:ff"

    // Name des Linkports
    String lpName

    // OS Gerätename des Anschlusses
    String deviceName

    // Jpcap Empfangsfilter
    String recvFilter

    //------------------------------------------------------------------------------
    /**
     * Warteschlange für empfangene Pakete <b/>
     * Wird von allen Anschlüssen gemeinsam verwendet
     */
    public MQueue<CL_IDU> toLinkQ

    //========================================================================================================
    // Konstruktor ANFANG
    //========================================================================================================
    /**
     *
     * @param lpName
     * @param deviceName
     * @param macAddr
     * @param recvFilter
     * @param recvQ
     */
    ConnectorToEthernet(String lpName, String deviceName, String macAddr = "00:00:00:00:00:00",
                        String recvFilter, MQueue<CL_IDU> recvQ) {
        this.lpName = lpName // Name des Linkports
        this.deviceName = deviceName // OS Gerätename
        this.macAddr = macAddr // eigene MAC-Adresse
        this.recvFilter = recvFilter // Jpcap Empfangsfilter
        toLinkQ = recvQ // Message-Queue zue Link-Schicht
    }

    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    /**
     * Stoppen des Netzwerkstacks
     * @param recvFilter
     */
    void start() {
        // jpcap initialisieren
        device = Utils.getDevice(deviceName) // Netzwerk-Device initialisieren
        receiver = JpcapCaptor.openDevice(device, 65535, true, 20) // Empfänger-Objekt erzeugen
        sender = receiver.getJpcapSenderInstance() // Sender-Objekt erzeugen

        // Wenn dieser Filter entfernt wird, werden alle MAC-Frames, die zu der physischen Netzwerkkarte
        // des Rechners gesendet werden, empfangen
        // Dir Filterausdruecke entsprechend den tcpdump-Regeln
        // Beispiel "portrange 5000-5200": nur Pakete in diesem Portbereich
        receiver.setFilter(recvFilter, true)

        // Receiver als thread starten
        receiverThread = Thread.start { receiver.loopPacket(-1, this) }
    }

    /**
     * Stoppen des Netzwerkstacks
     */
    void stop() {
        receiver.breakLoop() // Stoppen des Empfangs-Threads
        sleep(Utils.sec1) // Verzögerung
        receiverThread.join() // Warten auf Stoppen des Threads
    }

    //========================================================================================================

    /**
     * Erzeugt die zur Übergabe an die Library {@link jpcap} notwendige TCPPacket-Instanz.
     *
     * @param lc_idu
     */

    void send(LC_IDU lc_idu) {

        // MAC-Frame uebernehmen
        L_PDU macFrame = lc_idu.sdu as L_PDU

        // Zu sendendes Paket
        Packet sendPacket = null

        switch (macFrame.type) {
            case EthernetPacket.ETHERTYPE_IP:
                // IPDU uebernehmen
                I_PDU i_pdu = macFrame.sdu as I_PDU

                switch (i_pdu.protocol) {
                    case IPPacket.IPPROTO_TCP:
                        //------------------------------------------------------
                        // TCP
                        T_PDU t_pdu = i_pdu.sdu as T_PDU

                        sendPacket = new TCPPacket(t_pdu.srcPort, t_pdu.dstPort, t_pdu.seqNum,
                                t_pdu.ackNum, false, t_pdu.ackFlag, false,
                                false, t_pdu.synFlag, t_pdu.finFlag, false, false, t_pdu.windSize, 0)
                        sendPacket.data = t_pdu.sdu.getBytes()

                        //------------------------------------------------------
                        // IP
                        sendPacket.setIPv4Parameter(0, false, false, false, 0, false, false, false,
                                0, 1234, 100, i_pdu.protocol,
                                InetAddress.getByName(i_pdu.srcIpAddr), InetAddress.getByName(i_pdu.dstIpAddr))

                        break

                    case IPPacket.IPPROTO_UDP:
                        //------------------------------------------------------
                        // UDP
                        U_PDU u_pdu = i_pdu.sdu as U_PDU

                        sendPacket = new UDPPacket(u_pdu.srcPort, u_pdu.dstPort)
                        sendPacket.data = u_pdu.sdu.getBytes()

                        //------------------------------------------------------
                        // IP
                        sendPacket.setIPv4Parameter(0, false, false, false, 0, false, false, false,
                                0, 1234, 100, i_pdu.protocol,
                                InetAddress.getByName(i_pdu.srcIpAddr), InetAddress.getByName(i_pdu.dstIpAddr))

                        break
                }
                break

            case EthernetPacket.ETHERTYPE_ARP:
                // ARP
                AR_PDU ar_pdu = macFrame.sdu as AR_PDU

                sendPacket = new ARPPacket()

                sendPacket.hardtype = ARPPacket.HARDTYPE_ETHER
                sendPacket.prototype = ARPPacket.PROTOTYPE_IP
                sendPacket.operation = ar_pdu.operation
                sendPacket.hlen = 6
                sendPacket.plen = 4
                sendPacket.sender_hardaddr = Utils.stringToMac(macAddr)
                sendPacket.sender_protoaddr = Utils.stringToIp(ar_pdu.senderProtoAddr)
                sendPacket.target_hardaddr = Utils.stringToMac(ar_pdu.targetHardAddr)
                sendPacket.target_protoaddr = Utils.stringToIp(ar_pdu.targetProtoAddr)

                EthernetPacket ether = new EthernetPacket()
                ether.frametype = EthernetPacket.ETHERTYPE_ARP
                ether.src_mac = Utils.stringToMac(macAddr)
                ether.dst_mac = Utils.stringToMac(macFrame.dstMacAddr)

                sendPacket.datalink = ether

                break
        }

        if (sendPacket) {
            //------------------------------------------------------
            // MAC
            EthernetPacket ether = new EthernetPacket()
            ether.frametype = macFrame.type
            ether.src_mac = Utils.stringToMac(macFrame.srcMacAddr)
            ether.dst_mac = Utils.stringToMac(macFrame.dstMacAddr)
            sendPacket.datalink = ether

            //------------------------------------------------------
            // Senden
            Utils.writeLog("ConnectorToEthernet", "send", "sendet: ${sendPacket}", 66)
            ((JpcapSender) sender).sendPacket(sendPacket)
        }
    }

    //========================================================================================================

    /**
     * Diese Methode wird für jeden empfangenen MAC-Frame von der Library Jpcap aus gerufen.
     * @param recvPacket Das empfangene MAC-Frame
     */
    void receivePacket(Packet recvPacket) {
        L_PDU macFrame = new L_PDU()
        // IDU zur Link-Schicht
        CL_IDU cl_idu = new CL_IDU()
        // IP_PDU
        I_PDU i_pdu = new I_PDU()
        // ARP_PDU
        AR_PDU ar_pdu = new AR_PDU()
        // TCP_PDU
        T_PDU t_pdu = new T_PDU()
        // UDP_PDU
        U_PDU u_pdu = new U_PDU()

        EthernetPacket recvFrame
        IPPacket recvIpPacket = null
        ARPPacket recvArpPacket
        TCPPacket recvTCPPacket
        UDPPacket recvUDPPacket

        boolean ready = false
        // Dient zur Steuerung des Abbruchs des Paketempfangs

        //------------------------------------------------------
        // MAC bzw. Verbindungsschicht bzw. Sicherungsschicht
        // Adresse
        recvFrame = recvPacket.datalink as EthernetPacket

        macFrame.dstMacAddr = recvFrame.destinationAddress
        macFrame.srcMacAddr = recvFrame.sourceAddress
        macFrame.type = recvFrame.frametype

        // Ist es ein selbst gesendetes Broadcast-Frame ?
        if (macFrame.srcMacAddr != macAddr) {
            // Nein

            Utils.writeLog("ConnectorToEthernet", "receivePacket", "empfängt: ${recvPacket}", 66)

            // ---------------------------------------------------------------------------

            // MAC-Typfeld analysieren
            switch (recvFrame.frametype) {
            // Test auf IP-PDU
                case (EthernetPacket.ETHERTYPE_IP):
                    recvIpPacket = recvPacket as IPPacket
                    i_pdu.dstIpAddr = recvIpPacket.dst_ip.getHostAddress()
                    i_pdu.srcIpAddr = recvIpPacket.src_ip.getHostAddress()
                    i_pdu.id = recvIpPacket.ident
                    i_pdu.offset = recvIpPacket.offset
                    i_pdu.protocol = recvIpPacket.protocol
                    macFrame.sdu = i_pdu
                    break

            // Test auf ARP_PDU
                case (EthernetPacket.ETHERTYPE_ARP):
                    recvArpPacket = recvPacket as ARPPacket

                    ar_pdu.operation = recvArpPacket.operation
                    ar_pdu.senderHardAddr = Utils.macToString(recvArpPacket.sender_hardaddr)
                    ar_pdu.senderHardAddr = Utils.macToString(recvArpPacket.sender_hardaddr)
                    ar_pdu.senderProtoAddr = Utils.ipToString(recvArpPacket.sender_protoaddr)
                    ar_pdu.targetHardAddr = Utils.macToString(recvArpPacket.target_hardaddr)
                    ar_pdu.targetProtoAddr = Utils.ipToString(recvArpPacket.target_protoaddr)

                    macFrame.sdu = ar_pdu
                    // Paketanalyse abbrechen
                    ready = true
                    break

                default:
                    ready = true
            }

            // ---------------------------------------------------------------------------

            // Transport-Protokoll
            if (!ready) {
                switch (recvIpPacket.protocol) {
                // Test auf TCP
                    case (IPPacket.IPPROTO_TCP):
                        recvTCPPacket = recvPacket as TCPPacket
                        t_pdu.ackNum = recvTCPPacket.ack_num
                        t_pdu.seqNum = recvTCPPacket.sequence
                        t_pdu.dstPort = recvTCPPacket.dst_port
                        t_pdu.srcPort = recvTCPPacket.src_port
                        t_pdu.ackFlag = recvTCPPacket.ack
                        t_pdu.synFlag = recvTCPPacket.syn
                        t_pdu.finFlag = recvTCPPacket.fin
                        t_pdu.rstFlag = recvTCPPacket.rst
                        t_pdu.windSize = recvTCPPacket.window
                        t_pdu.sdu = new String(recvTCPPacket.data)
                        i_pdu.sdu = t_pdu
                        ready = true
                        break

                // Test auf UDP
                    case (IPPacket.IPPROTO_UDP):
                        recvUDPPacket = recvPacket as UDPPacket
                        u_pdu.dstPort = recvUDPPacket.dst_port
                        u_pdu.srcPort = recvUDPPacket.src_port
                        u_pdu.sdu = new String(recvUDPPacket.data)
                        i_pdu.sdu = u_pdu
                        ready = true
                        break

                    default:
                        ready = false
                }
            }

            // ---------------------------------------------------------------------------

            // Paket übergeben
            if (ready) {
                cl_idu.sdu = macFrame
                cl_idu.lpName = lpName
                toLinkQ.put(cl_idu) // Frame an Link-Schicht uebergeben
            }
        }
    }
}
