package experiments.experiment1.stack.layer.idu

import experiments.experiment1.stack.layer.pdu.PDU

/**
 * IDU von IP zu UDP
 */
class IU_IDU extends IDU {

    /** Quell-IP-Adresse */
    String srcIpAddr

    /** Quell-Port */
    int srcPort

    /** zu transportierende Map */
    PDU sdu

    /** Konvertieren in Text */
    String toString() {
        return String.format("IU_IDU: [srcIpAddr: ${srcIpAddr}, srcPort: ${srcPort}, sdu: ${sdu}]")
    }
}
