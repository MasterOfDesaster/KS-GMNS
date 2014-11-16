package experiments.experiment1.stack.layer.pdu

/**
 * TCP-PDU<br/>
 * Unvollständig
 */
class T_PDU extends PDU  {
    int seqNum
    int ackNum
    int dstPort
    int srcPort
    int windSize
    boolean ackFlag
    boolean synFlag
    boolean finFlag
    boolean rstFlag
    String sdu

    String toString() {
        return String.format("TPDU:[seqNum:${seqNum}, ackNum:${ackNum}, ackFlag:${ackFlag}, synFlag:${synFlag}, " +
                "finFlag:${finFlag}, rstFlag:${rstFlag}, dstPort:${dstPort}, srcPort:${srcPort}, windSize:${windSize}, sdu:${sdu}]")
    }
}

