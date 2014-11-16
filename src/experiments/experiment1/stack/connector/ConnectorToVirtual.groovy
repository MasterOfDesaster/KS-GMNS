package experiments.experiment1.stack.connector

import common.utils.Utils
import experiments.experiment1.stack.layer.idu.CL_IDU
import experiments.experiment1.stack.layer.idu.LC_IDU
import experiments.experiment1.stack.layer.pdu.L_PDU

import java.util.concurrent.LinkedBlockingQueue as MQueue

/**
 * Adapter fuer virtuelle LAN's oder Punkt-zu-Punkt-Verbindungen.<br/>
 * Virtuelle LAN's werden durch Angabe einer HUB-ID oder einer P2P-Link-ID gebildet. Alle Daten, die ueber Anschluesse mit der <br/>
 * gleichen HUB-ID gesendet werden, werden an alle Anschluesse mit der selben HUB-ID verteilt.<br/>
 * Das entspricht der Funktionsweise eines LAN mit einem HUB als zentralem Element.
 */
class ConnectorToVirtual extends Connector {

    //========================================================================================================
    // Instanzvariablen ANFANG
    //========================================================================================================

    //------------------------------------------------------------------------------
    // UDP-Portnummern zur Verwendung in "virtuellen" LAN

    final static udpPortBase = 5000

    /** ports[0..8]: UDP-Portadressen fuer virtuelle LAN,
     *  ports[9..17]: UDP-Portadressen fuer Point2Point-Links */
    final static List<List<Integer>> ports = [
            [udpPortBase + 11, udpPortBase + 12, udpPortBase + 13, udpPortBase + 14, udpPortBase + 15, udpPortBase + 16],
            [udpPortBase + 21, udpPortBase + 22, udpPortBase + 23, udpPortBase + 24, udpPortBase + 25, udpPortBase + 26],
            [udpPortBase + 31, udpPortBase + 32, udpPortBase + 33, udpPortBase + 34, udpPortBase + 35, udpPortBase + 36],
            [udpPortBase + 41, udpPortBase + 42, udpPortBase + 43, udpPortBase + 44, udpPortBase + 45, udpPortBase + 46],
            [udpPortBase + 51, udpPortBase + 52, udpPortBase + 53, udpPortBase + 54, udpPortBase + 55, udpPortBase + 56],
            [udpPortBase + 61, udpPortBase + 62, udpPortBase + 63, udpPortBase + 64, udpPortBase + 65, udpPortBase + 66],
            [udpPortBase + 71, udpPortBase + 72, udpPortBase + 73, udpPortBase + 74, udpPortBase + 75, udpPortBase + 76],
            [udpPortBase + 81, udpPortBase + 82, udpPortBase + 83, udpPortBase + 84, udpPortBase + 85, udpPortBase + 86],
            [udpPortBase + 91, udpPortBase + 92, udpPortBase + 93, udpPortBase + 94, udpPortBase + 95, udpPortBase + 96],
            [udpPortBase + 17, udpPortBase + 18],
            [udpPortBase + 27, udpPortBase + 28],
            [udpPortBase + 37, udpPortBase + 38],
            [udpPortBase + 47, udpPortBase + 48],
            [udpPortBase + 57, udpPortBase + 58],
            [udpPortBase + 67, udpPortBase + 68],
            [udpPortBase + 77, udpPortBase + 78],
            [udpPortBase + 87, udpPortBase + 88],
            [udpPortBase + 97, udpPortBase + 98],
    ]
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Warteschlange für empfangene Pakete <b/>
     * Wird von allen Anschlüssen gemeinsam verwendet
     */
    public MQueue<CL_IDU> toLinkQ
    //------------------------------------------------------------------------------

    /** Die ID des HUB's */
    int link

    /** Nummer des Anschlusses an diesem HUB */
    int connector

    /** Eigene (UDP)-Portadresse */
    int udpPort

    /** Eigene MAC-Adresse */
    String macAddr

    // Name des Linkports
    String lpName

    /** Wenn run == false stoppen die Threads */
    private boolean run = true

    // Max. Sende-Puffergrösse
    private final int MAX_SEND_PUFFER = 1400

    // Max. Empfangs-Puffergrösse
    private final int MAX_RECV_PUFFER = 1500

    // IP-Adresse vom local loop device
    InetAddress localAddr = InetAddress.getByName("localhost")

    /** Variablen fuer die Übertragung ueber UDP */
    DatagramSocket socket = null

    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    /**
     *
     * @param link Der HUB des virtuellen LAN oder ein P2P-Link
     * @param connector Nummer des virtuellen Anschlusses
     * @param macAddr eigene MAC-Adresse
     * @param toLinkQ Empfangs-Queue der Link-Schicht
     */
    ConnectorToVirtual(String lpName, int link, int connector, String macAddr, MQueue<CL_IDU> toLinkQ) {

        this.lpName = lpName
        this.link = link
        this.connector = connector
        this.macAddr = macAddr
        this.toLinkQ = toLinkQ

        udpPort = ports[link][connector]
    }

    //------------------------------------------------------------------------------

    /** Läuft als Thread und empfängt L-PDU's (MAC-Frame's) */
    void receive() {

        CL_IDU cl_idu
        byte[] bytes
        L_PDU macFrame
        DatagramPacket recvPacket

        while (run) {
            try {
                bytes = new byte[MAX_RECV_PUFFER]

                // Leeres UDP-Datagramm erzeugen
                recvPacket = new DatagramPacket(bytes, bytes.length)

                // Warten auf Empfang
                socket.receive(recvPacket)

                bytes = recvPacket.data

                // Deserialiiserung vorbereiten
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes)
                ObjectInputStream ois = new ObjectInputStream(bis)

                // Deserialiiserung
                macFrame = ois.readObject() as L_PDU

                Utils.writeLog("ConnectorToVirtual", "receive", "empfängt Frame: ${macFrame}", 66)

                // Ab hier steht eine L_PDU zur Verfügung
                // IDU fuellen
                cl_idu = new CL_IDU()
                cl_idu.sdu = macFrame
                cl_idu.lpName = lpName

                // IDU an Link-Schicht übergeben
                toLinkQ.put(cl_idu)

            } catch (Exception e) {
                println("Error in virtual network receiver:  ${e}")
                break // Schleife und damit Thread beenden
            }
        }
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    void send(LC_IDU lc_idu) {
        try {

            Utils.writeLog("ConnectorToVirtual", "send","uebernimmt von Link: ${lc_idu}", 66)

            // MAC-Frame uebernehmen
            L_PDU frame = lc_idu.sdu as L_PDU

            // Vorbereitung der Serialisierung
            ByteArrayOutputStream bos = new ByteArrayOutputStream()
            ObjectOutputStream oos = new ObjectOutputStream(bos)

            // Serialisierung der Sendedaten
            oos.writeObject(frame)
            oos.flush()
            byte[] bytes = bos.toByteArray()

            // Verteilung des Frames per UDP, ausser an eigenen UDP-Port
            ports[link].each { otherUdpPort ->
                if (otherUdpPort != udpPort) {
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, localAddr, otherUdpPort)
                    socket.send(packet)
                }
            }
        } catch (Exception e) {
            println("Error in virtual network sender:, ${e}")
        }
    }
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    void start() {
        // UDP-Socket zum Senden und Empfangen erzegen
        socket = new DatagramSocket(udpPort, localAddr)

        Thread.start { receive() } // Empfänger-Thread starten
    }
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    void stop() {
        run = false
        socket.close()
    }
    //------------------------------------------------------------------------------
}
