package experiments.experiment1.stack.layer.pdu

/**
 * UDP-PDU<br/>
 * Unvollständig
 */
class U_PDU extends PDU  {
    int dstPort
    int srcPort
    String sdu

    String toString() {
        return String.format("UPDU:[dstPort:${dstPort}, srcPort:${srcPort}, sdu:${sdu}]")
    }
}

