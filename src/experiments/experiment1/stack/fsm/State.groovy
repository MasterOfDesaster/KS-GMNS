package experiments.experiment1.stack.fsm

/**
 * Definition der möglichen Zustände der FSM.
 */
class State {
    /** Leerlauf */
    static final int S_IDLE = 100

    /** Auf SYN+ACK wartend */
    static final int S_WAIT_SYN_ACK = 110

    /** Wartend */
    static final int S_READY = 120

    /** Senden von ACK als Abschluß der Verbindungseröffnung */
    static final int S_SEND_SYN_ACK_ACK = 180

    /** Auf FIN+ACK wartend */
    static final int S_WAIT_FIN_ACK = 190

    /** Senden von ACK als Abschluß der Verbindungbeendigung */
    static final int S_SEND_FIN_ACK_ACK = 200

    /** Daten wurden empfangen */
    static final int S_RCVD_DATA = 210

    /** ACK wurde empfangen */
    static final int S_RCVD_ACK = 220

    /** SYN zur Verbindungseröffnung wird gesendet */
    static final int S_SEND_SYN = 230

    /** SYN zur Verbindungsbeendigung wird gesendet */
    static final int S_SEND_FIN = 240

    /** Daten werden gesendet */
    static final int S_SEND_DATA = 250

}
