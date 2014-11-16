package experiments.experiment1.stack.layer.idu

/**
 * IDU von Anwendung tu TCP
 */
class AT_IDU extends IDU {

    /** Kommando */
    int command

    /** Ziel-IP-Adresse */
    String dstIpAddr

    /** Ziel-Port */
    int dstPort

    /** Verbindungs-ID */
    int connId

    /** A_PDU */
    String sdu

    /** Konvertieren in Text */
    String toString() {
        return String.format("AT_IDU: [connId: ${connId}, command: ${command}, dstIpAddr: ${dstIpAddr}, dstPort: ${dstPort}, sdu: ${sdu}]")
    }
}
