package experiments.experiment1.stack.layer.idu

/**
 * IDU von UDP zu Anwendung
 */
class UA_IDU extends IDU {

    /** Quell-IP-Adresse */
    String srcIpAddr

    /** Quell-Port */
    int srcPort

    /** Anwendungsdaten */
    String sdu

    /** Konvertieren in Text */
    String toString() {
        return String.format("UA_IDU: [dsrcIpAddr: ${srcIpAddr}, srcPort: ${srcPort}, sdu: ${sdu}]")
    }
}
