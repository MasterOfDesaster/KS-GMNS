package experiments.experiment1.stack.layer.idu

import experiments.experiment1.stack.layer.pdu.PDU

/**
 * IDU von Link zu Anschluessen
 */
class LC_IDU extends IDU {

    /** Zu transportierende PDU */
    PDU sdu

    /** Konvertieren in Text */
    String toString() {
        return String.format("LC_IDU: [sdu: ${sdu}]")
    }
}
