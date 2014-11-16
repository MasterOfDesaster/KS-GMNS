package experiments.experiment1.stack.layer.idu

import experiments.experiment1.stack.layer.pdu.PDU

/**
 * IDU von Transportschicht (TCP oder UDP) zu IP
 */
class TRI_IDU extends IDU {

    /** Ziel-IP-Adresse */
    String dstIpAddr

    /** Zu transportierender Protokolltyp */
    int protocol

    /** Zu transportierende PDU */
    PDU sdu

    /** Konvertieren in Text */
    String toString() {
        return String.format("TRI_IDU: [dstIpAddr: ${dstIpAddr}, protocol: ${protocol}, sdu: ${sdu}]")
    }
}
