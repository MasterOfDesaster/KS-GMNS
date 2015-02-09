package experiments.experiment1.stack.fsm

/**
 * Definition der möglichen Zustände der FSM.
 */
class State {
    /** Leerlauf */
    static final int S_IDLE = 100

    /** Auf SYN+ACK wartend */
    static final int S_WAIT_SYN_ACK = 110

    /** SYN gesendet -passiv- */
    static final int S_SEND_SYN_ACK = 115

    /** Wartend */
    static final int S_READY = 120

    /** Senden von ACK als Abschluß der Verbindungseröffnung */
    static final int S_SEND_SYN_ACK_ACK = 180

    /** auf SYN+ACK+ACK wartend -passiv- */
    static final int S_WAIT_SYN_ACK_ACK = 185

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

    //auf SYN wartend -passiv-
    static final int S_WAIT_SYN = 235

    /** SYN zur Verbindungsbeendigung wird gesendet */
    static final int S_SEND_FIN = 240

    /** Fin wird bestätigt */
    static final int S_SEND_FIN_ACK = 245

    /** Auf Bestätigung des Fins warten */
    static final int S_WAIT_FIN_ACK_ACK = 255

    /** Daten werden gesendet */
    static final int S_SEND_DATA = 250

    static final int S_RCVD_SYN_ACK_ACK = 260

    static final int S_RCVD_FIN_ACK_ACK = 270

}
