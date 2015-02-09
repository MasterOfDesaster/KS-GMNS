package experiments.experiment1.stack.fsm

/**
 * Definition der möglichen Zustände der FSM.
 */
class State {
    /** Leerlauf */
    static final int S_IDLE = 100

    /** Auf SYN wartend */
    static final int S_WAIT_SYN = 105

    /** Auf SYN+ACK wartend */
    static final int S_WAIT_SYN_ACK = 110

    /**  Auf SYN+ACK+ACK warten*/
    static final int S_WAIT_SYN_ACK_ACK = 115

    /** Wartend */
    static final int S_READY = 120

    /** Senden von SYN-ACK*/
    static final int S_SEND_SYN_ACK = 130

    /** Senden von ACK als Abschluß der Verbindungseröffnung */
    static final int S_SEND_SYN_ACK_ACK = 180

    /** Empfangen von SYN ACK ACK als Abschluss der Verbindungseröffnung*/
    static final int S_RCVD_SYN_ACK_ACK = 185

    /** Auf FIN+ACK wartend */
    static final int S_WAIT_FIN_ACK = 190

    /** Senden des FIN-ACK's*/
    static final int S_SEND_FIN_ACK = 195

    /** Senden von ACK als Abschluß der Verbindungbeendigung */
    static final int S_SEND_FIN_ACK_ACK = 200

    /** warten auf FIN ACK ACK*/
    static final int S_WAIT_FIN_ACK_ACK = 205

    /** Daten wurden empfangen */
    static final int S_RCVD_DATA = 210

    /** FIN ACK ACK wurde empfangen*/
    static final int S_RCVD_FIN_ACK_ACK = 215

    /** ACK wurde empfangen */
    static final int S_RCVD_ACK = 220

    /** SYN zur Verbindungseröffnung wird gesendet */
    static final int S_SEND_SYN = 230

    /** SYN zur Verbindungsbeendigung wird gesendet */
    static final int S_SEND_FIN = 240

    /** Daten werden gesendet */
    static final int S_SEND_DATA = 250

}
