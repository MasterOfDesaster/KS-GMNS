package experiments.experiment1.stack.layer.pdu

/**
 * ARP-PDU<br/>
 * Unvollständig
 */
class AR_PDU extends PDU {
    String targetHardAddr  // MAC-Adresse des abgefragten Geräts
    String senderHardAddr  // MAC-Adresse des abfragenden Geräts
    String targetProtoAddr  // IP-Adresse des abgefragten Geräts
    String senderProtoAddr  // IPC-Adresse des abfragenden Geräts
    int operation

    String toString() {
        return String.format("AR_PDU:[operation:${operation}, targetHardAddr:${targetHardAddr}, targetProtoAddr:${targetProtoAddr}, " +
                "senderHardAddr:${senderHardAddr}, senderProtoAddr:${senderProtoAddr}]")
    }
}
