package experiments.experiment1.stack.layer.pdu

/**
 * IP-PDU<br/>
 * Unvollständig
 */
class I_PDU extends PDU {
    String dstIpAddr
    String srcIpAddr
    int offset
    int id
    int protocol
    PDU sdu

    String toString() {
        return String.format("I_PDU:[dstIpAddr:${dstIpAddr}, srcIpAddr:${srcIpAddr}, protocol:${protocol}, sdu:${sdu}]")
    }
}
