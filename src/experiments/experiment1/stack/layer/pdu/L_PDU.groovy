package experiments.experiment1.stack.layer.pdu

/**
 * MAC-Frame<br/>
 * Unvollständig
 */
class L_PDU  extends PDU {
    String srcMacAddr
    String dstMacAddr
    int type
    PDU sdu

    String toString() {
        return String.format("L_PDU:[dstMacAddr:${dstMacAddr}, srcMacAddr:${srcMacAddr}, type:${type}, sdu:${sdu}]")
    }
}
