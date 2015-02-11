package experiments.experiment1.stack.layer

import common.fsm.FiniteStateMachine
import common.utils.Utils
import experiments.experiment1.stack.fsm.Event
import experiments.experiment1.stack.fsm.State
import experiments.experiment1.stack.layer.idu.AT_IDU
import experiments.experiment1.stack.layer.idu.IT_IDU
import experiments.experiment1.stack.layer.idu.TA_IDU
import experiments.experiment1.stack.layer.idu.TRI_IDU
import experiments.experiment1.stack.layer.pdu.T_PDU

import java.util.concurrent.LinkedBlockingQueue as MQueue
import java.util.concurrent.Semaphore

//========================================================================================================
// TcpLayer-Klasse ANFANG
//========================================================================================================

/**
 * Die TCP-Protokoll-Schicht
 */
class TcpLayer {

    //========================================================================================================
    // Daten ANFANG
    //========================================================================================================

    // Konstanten ------------------------------------------------------------------

    /* Kommandos/Zustaende */
    /** Öffnen einer Verbindung */
    static final int OPEN = 110
    /** Schließen der Verbindung */
    static final int CLOSE = 120
    /** Übertragung von Daten */
    static final int DATA = 130

    /** Maximales TCP-Empfangsfenster */
    static final int WINDOWSIZE = 1000

    /** Maximale TCP-Segmentgrösse */
    static final int MSS = 100

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /** Stoppen der Threads wenn false */
    Boolean run = true
    //------------------------------------------------------------------------------

    //========================================================================================================

    //------------------------------------------------------------------------------
    /* message queues in Richtung der TCP-Schicht */
    /** message queue von Anwendung */
    MQueue<AT_IDU> fromAppQ = new MQueue(Utils.MAXQUEUE)
    /** message queue von IP-Schicht */
    MQueue<IT_IDU> fromIpQ = new MQueue(Utils.MAXQUEUE)
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /* message queues zu Nachbarschichten */
    /** message queue zu Anwendung */
    MQueue<TA_IDU> toAppQ = null
    /** message queue zu IP */
    MQueue<TRI_IDU> toIpQ = null
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /** Warteschlange fuer Sendewiederholungen.
      * Ist "thread save"
      * Format der Map: keys: timeOut: Anzahl noch abzuwartender Perioden,
      *      idu: TRI_IDU
      */
    final List<Map> sendWaitQ = [].asSynchronized()

    // Zeitwerte fuer Steuerung der Sendewiederholung
    /** RTO - retransmission timeout */
    int timeOut
    /** delta t für countdown des RTO */
    int deltaTimeOut

    //------------------------------------------------------------------------------

    //========================================================================================================
    // Verbindungssteuerung                                                                                 //
    //========================================================================================================

    /** ID der Verbindung */
    int connId = 0

    /** eigene Portadresse */
    int ownPort = 0

    /** IP-Adresse des Verbindungsanfordernden */
    String dstIpAddr = "0.0.0.0"

    /** Portnummer des Verbindungsanfordernden */
    int dstPort = 0

    //========================================================================================================
    // TCP-Parameter                                                                                        //
    //========================================================================================================

    // Sendeparameter
    boolean sendAckFlag
    boolean sendSynFlag
    boolean sendFinFlag
    boolean sendRstFlag = false
    int sendSeqNum
    int sendAckNum
    int sendWindSize
    String sendData

    // Empfangene Parameter
    int recvSeqNum
    int recvAckNum
    boolean recvAckFlag
    boolean recvFinFlag
    boolean recvSynFlag
    boolean recvRstFlag = false
    int recvWindSize
    String recvData
    //------------------------------------------------------------------------------

    //========================================================================================================
    // Finite State Machine                                                                                 //
    //========================================================================================================

    /**
     *  Beschreibung der TCP-Zustandsübergänge für dieses Programmbeispiel, d.h. unvollständig!<br/>
     *  Der Anfangszustand ist "S_IDLE".<br/>
     *  Hier müssen Sie ändern und ergänzen.
     */
    List<Map> transitions =
        [
            // Aktiver Verbindungsaufbau
            [on: Event.E_CONN_REQ, from: State.S_IDLE, to: State.S_SEND_SYN],
            [on: Event.E_SEND_SYN, from: State.S_SEND_SYN, to: State.S_WAIT_SYN_ACK],
            [on: Event.E_RCVD_SYN_ACK, from: State.S_WAIT_SYN_ACK, to: State.S_SEND_SYN_ACK_ACK],
            [on: Event.E_SYN_ACK_ACK_SENT, from: State.S_SEND_SYN_ACK_ACK, to: State.S_READY],

            // Passiver Verbindungsaufbau
            //[on: Event.E_WAIT_REQ, from: State.S_IDLE, to: State.S_WAIT_SYN],
            //[on: Event.E_RCVD_SYN, from: State.S_WAIT_SYN, to: State.S_SEND_SYN_ACK],
            [on: Event.E_RCVD_SYN, from: State.S_IDLE, to: State.S_SEND_SYN_ACK],
            [on: Event.E_SEND_SYN_ACK, from:State.S_SEND_SYN_ACK, to: State.S_WAIT_SYN_ACK_ACK],
            [on: Event.E_RCVD_ACK, from: State.S_WAIT_SYN_ACK_ACK, to: State.S_RCVD_SYN_ACK_ACK],
            [on: Event.E_RCVD_SYN_ACK_ACK, from: State.S_RCVD_SYN_ACK_ACK, to: State.S_READY],

            // Datenübertragung: Senden
            [on: Event.E_SEND_DATA, from: State.S_READY, to: State.S_SEND_DATA],
            [on: Event.E_DATA_SENT, from: State.S_SEND_DATA, to: State.S_READY],
            [on: Event.E_RCVD_ACK, from: State.S_READY, to: State.S_RCVD_ACK],
            [on: Event.E_READY, from: State.S_RCVD_ACK, to: State.S_READY],

            // Datenübertragung: Empfangen
            [on: Event.E_RCVD_DATA, from: State.S_READY, to: State.S_RCVD_DATA],
            [on: Event.E_READY, from: State.S_RCVD_DATA, to: State.S_READY],

            // Aktiver Verbindungsabbau
            [on: Event.E_DISCONN_REQ, from: State.S_READY, to: State.S_SEND_FIN],
            [on: Event.E_SEND_FIN, from: State.S_SEND_FIN, to: State.S_WAIT_FIN_ACK],
            [on: Event.E_RCVD_FIN, from: State.S_WAIT_FIN_ACK, to: State.S_SEND_FIN_ACK_ACK],
            [on: Event.E_FIN_ACK_ACK_SENT, from: State.S_SEND_FIN_ACK_ACK, to: State.S_IDLE],

            // Passiver Verbindungsabbau
            [on: Event.E_RCVD_FIN, from: State.S_READY, to: State.S_SEND_FIN_ACK],
            [on: Event.E_SEND_FIN_ACK, from: State.S_SEND_FIN_ACK, to:State.S_WAIT_FIN_ACK_ACK],
            [on: Event.E_RCVD_ACK, from: State.S_WAIT_FIN_ACK_ACK, to: State.S_RCVD_FIN_ACK_ACK],
            [on: Event.E_DISCONN_CON, from: State.S_RCVD_FIN_ACK_ACK, to: State.S_IDLE]
        ]

    /** Die Finite Zustandsmaschine. */
    FiniteStateMachine fsm
    /** Neuer Zustand. */
    int newState

    //========================================================================================================
    // Methoden                                                                                             //
    //========================================================================================================

    //------------------------------------------------------------------------------
    // Hilfen zur Thread-Synchronisierung bei Open und Close
    /** Semaphore zur Steuerung des Verbindungsaufbaus. */
    final Semaphore openSignal = new Semaphore(0)
    void waitForOpen() { openSignal.drainPermits(); openSignal.acquire() }
    void notifyOpen() { openSignal.release() }

    //.............................................................................

    /** Semaphore zur Steuerung des Verbindungsabbaus. */
    final Semaphore closeSignal = new Semaphore(0)
    void waitForClose() { openSignal.drainPermits(); closeSignal.acquire() }
    void notifyClose() { closeSignal.release() }
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Empfaengt Daten von der IP-Schicht und verarbeitet sie
     */
    void receive() {

        /** IDU von IP */
        IT_IDU it_idu

        // TCP-PDU
        T_PDU t_pdu

        //-------------------------------------------------------------------------

        while (run) {
            // blockierendes Lesen von IP-Schicht
            it_idu = fromIpQ.take()

            // TCP-PDU und Parameter entnehmen
            t_pdu = it_idu.sdu as T_PDU
            dstIpAddr = it_idu.srcIpAddr

            //Utils.writeLog("TcpLayer",  "receive"," ${ownPort}, ${t_pdu.dstPort}",2)
            Utils.writeLog("TcpLayer", "receive", "uebernimmt  von IP: ${it_idu}", 2)

            // Hier z.B. noch auf richtigen Zielport testen
            if (ownPort == t_pdu.dstPort) {

                //Entfernen von quittierten Daten aus der Warteschlange
                // fuer Sendewiederholungen
                if (t_pdu.ackFlag) {
                    removeWaitQ(t_pdu.ackNum)
                    Utils.writeLog("TcpLayer", "receive", "löscht Pakete mit der Nummer: ${t_pdu.ackNum}", 2)
                }

                // Analysieren einer empfangenen TCP-PDU
                // Bestimmen eines Ereignises, "feuern" der FSM und Behandlung
                // den neuen Zustands
                recvSeqNum = t_pdu.seqNum
                recvAckNum = t_pdu.ackNum
                recvAckFlag = t_pdu.ackFlag
                recvFinFlag = t_pdu.finFlag
                recvSynFlag = t_pdu.synFlag
                recvRstFlag = t_pdu.rstFlag
                recvWindSize = t_pdu.windSize
                recvData = t_pdu.sdu ? t_pdu.sdu : ""

                if (recvSynFlag) {
                    dstIpAddr = it_idu.srcIpAddr
                    dstPort = t_pdu.srcPort
                }

                //------------------------------------------------------------------

                int event = 0
                // Ereignis bestimmen
                switch (true) {
                    case (recvRstFlag): Utils.writeLog("TcpLayer", "reset", "Verbindung wird abgebrochen", 2); fsm.currentState = State.S_IDLE; break
                    case (recvSynFlag && !recvAckFlag): event = Event.E_RCVD_SYN; break
                    case (recvFinFlag): event = Event.E_RCVD_FIN; break
                    case (recvSynFlag && recvAckFlag): event = Event.E_RCVD_SYN_ACK; break
                    case (recvAckFlag && t_pdu.sdu.size() == 0): event = Event.E_RCVD_ACK; break
                    case (recvAckFlag && t_pdu.sdu.size() > 0): event = Event.E_RCVD_DATA; break
                }

                if (event) {
                    // Neuen Zustand behandeln
                    handleStateChange(event)
                }
            }else{
                Utils.writeLog("TcpLayer", "blockiert", "falscher Port, dstPort: ${t_pdu.dstPort}, ownPort: ${ownPort}",2)
            }
        }
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Holt Daten von der Anwendung und uebergibt sie an die IP-Schicht
     */
    void send() {

        /** IDU von Anwendung */
        AT_IDU at_idu

        //-------------------------------------------------------------------------

        while (run) {
            at_idu = fromAppQ.take() // blockierendes Lesen von Anwendung

            Utils.writeLog("TcpLayer", "send", "uebernimmt  von Anwendung: ${at_idu}", 2)

            switch (at_idu.command) {
                case OPEN:
                    // Neue Verbindung öffnen

                    // Zielparameter des Kommunikationspartners
                    dstIpAddr = at_idu.dstIpAddr
                    dstPort = at_idu.dstPort

                    handleStateChange(Event.E_CONN_REQ)
                    break

                case CLOSE:
                    // Verbindung schließen
                    handleStateChange(Event.E_DISCONN_REQ)
                    break

                case DATA:

                    int current_start_pos = 0
                    int current_end_pos = MSS
                    int dataLength = at_idu.sdu.size()

                    System.out.println("datalength: " + dataLength + ", MSS: " + MSS)
                    if(dataLength < MSS){
                        // Daten senden
                        sendData = at_idu.sdu // Anwendungsdaten übernehmen
                        handleStateChange(Event.E_SEND_DATA)
                    }else{
                        while(current_start_pos<dataLength){
                            //sende Segment
                            sendData = at_idu.sdu[current_start_pos..current_end_pos]
                            Utils.writeLog("TcpLayer", "send", "Segment: Laenge:${sendData.length()} Byte:${sendData.bytes.size()} Gesamtlaenge:${at_idu.sdu.length()} Gesamtbytes:${at_idu.sdu.bytes.size()} Daten:${sendData}", 2)

                            current_start_pos += MSS
                            current_end_pos += MSS
                            System.out.println("atidu: " + at_idu.sdu + ", currentstartpos: " + current_start_pos + ", currentendpos: " + current_end_pos)

                            if (current_end_pos > dataLength) {
                                current_end_pos = dataLength-1
                            }
                        }
                        handleStateChange(Event.E_SEND_DATA)
                    }
                    break

                    /*
                    String dataToSend = at_idu.sdu
                    int dataLength = MSS

                    Utils.writeLog("TcpLayer", "test", "at_idu.sdu: " + at_idu.sdu, 2)
                    while(dataToSend.length() > 0) {
                        if(dataToSend.length() <= dataLength) {
                            sendData = dataToSend
                            dataToSend = ""
                            Utils.writeLog("TcpLayer", "send", "Segment: Laenge:${sendData.length()} Byte:${sendData.bytes.size()} Gesamtlaenge:${at_idu.sdu.length()} Gesamtbytes:${at_idu.sdu.bytes.size()} Daten:${sendData}", 2)
                            handleStateChange(Event.E_SEND_DATA)
                        } else {
                            sendData = dataToSend.substring(0, dataLength)
                            dataToSend = dataToSend.substring(dataLength)
                            Utils.writeLog("TcpLayer", "test", "sendData: " + sendData + ", dataToSend: " + dataToSend + ", at_idu.sdu: " + at_idu.sdu, 2)
                            Utils.writeLog("TcpLayer", "send", "Segment: Laenge:${sendData.length()} Byte:${sendData.bytes.size()} Gesamtlaenge:${at_idu.sdu.length()} Gesamtbytes:${at_idu.sdu.bytes.size()} Daten:${sendData}", 2)
                        }
                    }
                    break
                    */
            }
        }
    }

    //------------------------------------------------------------------------------
    /**
     * Behandelt jeweils einen neuen Zustand der Zustandsmaschine.
     * @param event das neue Ereignis
     */
    synchronized void handleStateChange(int event) {

        // Zustandsübergang bestimmen
        int nextState = fsm.fire(event)

        if (nextState) {
            // Neuen Zustand behandeln

            newState = nextState
            switch (newState) {

            // ----------------------------------------------------------
            // Aktiver Verbindungsaufbau

                case (State.S_SEND_SYN):
                    // Verbindungsaufbau beginnen
                    Utils.writeLog("TcpLayer", "starte", "Verbindungsaufbau", 2)
                    sendAckNum = 0
                    sendSeqNum = new Random().nextInt(6000) + 1

                    sendAckFlag = false
                    sendSynFlag = true
                    sendFinFlag = false
                    sendRstFlag = false
                    sendWindSize = WINDOWSIZE
                    sendData = ""

                    // T-PDU erzeugen und senden
                    Utils.writeLog("TcpLayer", "sendet", "Sende SYN mit Seq-Nr.: ${sendSeqNum}, ACK-Nr.: ${sendAckNum} ", 2)
                    sendTpdu()

                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_SEND_SYN)
                    break

                case (State.S_SEND_SYN_ACK_ACK):
                    // SYN+ACK empfangen
                    Utils.writeLog("TcpLayer", "empfange", "Seq-Nr.: ${recvSeqNum} und ACK-Nr.: ${recvAckNum}",2)
                    // ACK senden
                    sendSynFlag = false
                    sendAckFlag = true
                    sendAckNum = recvSeqNum + 1
                    sendSeqNum += 1
                    sendFinFlag = false
                    sendRstFlag = false
                    sendData = ""

                    // T-PDU erzeugen und senden
                    Utils.writeLog("TcpLayer", "sendet", "SYN-ACK-ACK mit Seq-Nr.: ${sendSeqNum}, ACK-Nr.: ${sendAckNum}", 2)
                    sendTpdu()

                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_SYN_ACK_ACK_SENT)

                    // Hergestellte Verbindung signalisieren
                    notifyOpen()
                    break

            // ----------------------------------------------------------
            // Passiver Verbindungsaufbau
                case (State.S_SEND_SYN_ACK):
                    // SYN+ACK empfangen, ACK senden
                    sendSynFlag = true
                    sendAckFlag = true
                    sendAckNum = recvSeqNum + 1
                    sendSeqNum += new Random().nextInt(6000) + 1
                    sendFinFlag = false
                    sendRstFlag = false
                    sendData = ""

                    // T-PDU erzeugen und senden
                    Utils.writeLog("TcpLayer", "sendet", "SYN-ACK mit Seq-Nr.: ${sendSeqNum}, ACK-Nr.: ${sendAckNum}",2)
                    sendTpdu()

                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_SEND_SYN_ACK)
                    break

                case (State.S_RCVD_SYN_ACK_ACK):
                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_RCVD_SYN_ACK_ACK)

                    // Hergestellte Verbindung signalisieren
                    Utils.writeLog("TcpLayer", "stellte", "Verbindung her", 2)
                    notifyOpen()
                    break

            // ----------------------------------------------------------
            // Aktiver Verbindungsabbau

                case (State.S_SEND_FIN):
                    // Verbindungsabbau beginnen
                    Utils.writeLog("TcpLayer", "startet", "Verbindungsabbau", 2)
                    sendAckFlag = true
                    sendSynFlag = false
                    sendFinFlag = true
                    sendRstFlag = false
                    sendWindSize = 0
                    sendData = ""

                    // T-PDU erzeugen und senden
                    Utils.writeLog("TcpLayer", "sende", "FIN mit Seq-Nr.: ${sendSeqNum}, ACK-Nr.: ${sendAckNum}",2)
                    sendTpdu()

                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_SEND_FIN)
                    break

                case (State.S_SEND_FIN_ACK_ACK):
                    // FIN+ACK empfangen
                    Utils.writeLog("TcpLayer", "empfange", "Seq-Nr.: ${recvSeqNum}, ACK-Nr.: ${recvAckNum}", 2)
                    // ACK senden
                    sendAckFlag = true
                    sendFinFlag = false
                    sendSeqNum += 1
                    sendAckNum = recvSeqNum + 1
                    sendData = ""

                    // ACK nach FIN+ACK senden
                    Utils.writeLog("TcpLayer", "sendet", "FIN-ACK-ACK mit Seq-Nr.: ${sendSeqNum}, ACK-Nr.: ${sendAckNum}", 2)
                    sendTpdu()

                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_FIN_ACK_ACK_SENT)

                    // Ende der Verbindung signalisieren
                    Utils.writeLog("TcpLayer", "beendet", "Verbindungsabbau", 2)
                    notifyClose()
                    break

            // ----------------------------------------------------------
            // Passiver Verbindungsabbau
                case(State.S_SEND_FIN_ACK):
                    //Verbindungsabbau beginnen
                    Utils.writeLog("TcpLayer", "startet", "Verbindungsabbau", 2)
                    sendAckFlag = true
                    sendSynFlag = false
                    sendFinFlag = true
                    sendRstFlag = false
                    sendWindSize = 0
                    sendData = ""

                    sendSeqNum = recvAckNum
                    sendAckNum = recvSeqNum + 1

                    // ACK nach FIN senden
                    Utils.writeLog("TcpLayer", "sendet", "FIN-ACK mit Seq-Nr.: ${sendSeqNum}, ACK-Nr.: ${sendAckNum}",2)
                    sendTpdu()

                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_SEND_FIN_ACK)
                    break

                case(State.S_RCVD_FIN_ACK_ACK):
                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_DISCONN_CON)

                    //Ende der Verbindung signalisieren
                    Utils.writeLog("TcpLayer", "beendet", "Verbindungsabbau", 2)
                    notifyClose()
                    break

            // ----------------------------------------------------------
            // Daten empfangen

                case (State.S_RCVD_DATA):
                    // Daten empfangen
                    // Wurde die Sequenznummer erwartet?
                    // ACHTUNG: hier wird momentan Auslieferungsdisziplin der IP-Schicht angenommen!
                    if (recvSeqNum == sendAckNum) {
                        // Ja, ACK senden
                        sendSynFlag = false
                        sendAckFlag = true
                        // Bei UTF-8 Encoding besser: sendAckNum = sendAckNum + recvData.bytes.size()
                        sendAckNum = sendAckNum + recvData.bytes.size()
                        sendFinFlag = false
                        sendRstFlag = false
                        sendData = ""

                        TA_IDU ta_idu = new TA_IDU()
                        ta_idu.connId = connId

                        // Daten uebernehmen
                        ta_idu.sdu = recvData
                        Utils.writeLog("TcpLayer", "receive", "Daten mit Seq-Nr.: ${recvSeqNum}, ACK-Nr.: ${recvAckNum}, Datenbytes: ${recvData.bytes.size()}", 2)

                        // IDU an Anwendung übergeben
                        toAppQ.put(ta_idu)

                        recvData = ""

                        // ACK senden
                        sendTpdu()
                    }else if(recvSeqNum>sendAckNum){
                        //Pakete verloren gegangen, sende D-ACK
                        sendAckFlag = true
                        sendFinFlag = false
                        sendRstFlag = false
                        sendSynFlag = false
                        sendData =""

                        Utils.writeLog("TcpLayer", "sendet", "Duplicate ACK aufgrund verlorener Pakete mit Seq-Nr.: ${sendSeqNum}, ACK-Nr.: ${sendAckNum}", 2)

                        //ACK senden
                        sendTpdu()
                    }else{
                        Utils.writeLog("TcpLayer", "receive", "unbekannte Pakete mit Seq-Nr.: ${recvSeqNum}, ACK-Nr.: ${recvAckNum} - erwartet: Seq-Nr.: ${sendSeqNum}, ACK-Nr.: ${sendAckNum}", 2)
                    }

                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_READY)
                    break

            // ----------------------------------------------------------
            // Daten senden

                case (State.S_SEND_DATA):
                    // Senden von Anwendungsdaten
                    sendSynFlag = false
                    sendAckFlag = true
                    sendFinFlag = false

                    // Daten senden
                    sendTpdu()
                    Utils.writeLog("TcpLayer","send","Schicke Daten: ACK-Nr.:${sendAckNum} Seq-Nr.:${sendSeqNum} Datenbytes:${sendData.bytes.size()}",2)
                // Bei UTF-8 Encoding besser: sendSeqNum += sendData.bytes.size()
                    sendSeqNum += sendData.bytes.size()

                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_DATA_SENT)
                    break

            // ----------------------------------------------------------
            // ACK empfangen

                case (State.S_RCVD_ACK):
                    if(recvSeqNum==sendAckNum){
                        sendAckNum = recvSeqNum
                        Utils.writeLog("TcpLayer", "empfängt", "ACK mit Seq-Nr.: ${sendSeqNum} und ACK-Nr.: ${sendAckNum}",2)

                    }

                    // Neuen Zustand der FSM erzeugen
                    fsm.fire(Event.E_READY)
                    break

            // ----------------------------------------------------------

                default:
                    // nicht zu behandelnder Zustand oder null bei Fehler
                    break
            }
        }
    }

    //------------------------------------------------------------------------------

    /**
     * Fuellt eine T-PDU und uebergibt sie an die IP-Schicht.
     */
    void sendTpdu() {
        T_PDU tpdu = new T_PDU()
        tpdu.dstPort = dstPort // Ziel-Portnummer eintragen
        tpdu.srcPort = ownPort // Quell-Portnummer eintragen
        tpdu.ackNum = sendAckNum
        tpdu.seqNum = sendSeqNum
        tpdu.ackFlag = sendAckFlag
        tpdu.synFlag = sendSynFlag
        tpdu.finFlag = sendFinFlag
        tpdu.rstFlag = sendRstFlag
        tpdu.windSize = sendWindSize
        tpdu.sdu = sendData

        TRI_IDU ti_idu = new TRI_IDU()
        ti_idu.sdu = tpdu
        // Ziel-IP-Adresse eintragen
        ti_idu.dstIpAddr = dstIpAddr
        // Absendendes Protokoll eintragen
        ti_idu.protocol = IpLayer.PROTO_TCP

        // IDU in Warteschlange fuer Sendewiederholungen eintragen
        if(!sendAckFlag || sendSynFlag || sendFinFlag || sendRstFlag || sendData!="") {
            insertWaitQ(ti_idu)
        }

        // Daten an IP-Schicht uebergeben
        toIpQ.put(ti_idu)
    }

    //=== Steuerung der Sendewiederholung ============================================

    //------------------------------------------------------------------------------
    /**
     * Laeuft periodisch und durchsucht die Warteschlange nach Sendewiederholungen.<br/>
     */
    void timeOut() {
        synchronized (sendWaitQ) {
            sendWaitQ.each { m ->
                // Ist der Timeout abgelaufen?
                if ((m.timeOut - deltaTimeOut) > 0) {
                    // Nein
                    m.timeOut = m.timeOut - deltaTimeOut
                } else {
                    // Ja
                    Utils.writeLog("TcpLayer", "timeOut", "Sendewiederholung: ${m.idu}", 2)

                    m.timeOut = timeOut // Timeout neu setzen
                    toIpQ.put(m.idu) // IDU an IP uebergeben
                }
            }
        }
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------

    /**
     * Fuegt eine mit timeout an IP zu uebergebene IDU in die Sendewarteschlange ein
     * @param idu
     */
    void insertWaitQ(TRI_IDU idu) {
        synchronized (sendWaitQ) {
            sendWaitQ.add([timeOut: timeOut, idu: idu])
        }
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Entfernt alle Elemente mit Sequenznummern kleiner als Parameter seqNumber
     * aus der Warteschlange
     * @param seqNumber
     */
    void removeWaitQ(int seqNumber) {
        synchronized (sendWaitQ) {
            sendWaitQ.removeAll { m ->
                m.idu.sdu.seqNum < seqNumber
            }
        }
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Warteschlange fuer Sendewiederholungen leeren
     */
    void clearWaitQ() {
        synchronized (sendWaitQ) {
            sendWaitQ.clear()
        }
    }

    //------------------------------------------------------------------------------


    //=== Schnittstelle zu TCP ======================================================
    //------------------------------------------------------------------------------
    /**
     * Herstellung einer TCP-Verbindung.<br/>
     * Es kann zu jedem Zeitpunkt nur eine Verbindung unterhalten werden.
     * @param Map at_idu: keys: dstIpAddr, dstPort
     * @return connId ID der Verbindung oder 0 bei Fehler
     */
    int open(Map idu) {

        Utils.writeLog("TcpLayer", "open", "initiiere Verbindung: ${idu}", 2)

        // Auftrag zum Verbindungsaufbau
        AT_IDU at_idu = new AT_IDU()
        at_idu.command = OPEN
        at_idu.dstIpAddr = idu.dstIpAddr
        at_idu.dstPort = idu.dstPort
        fromAppQ.put(at_idu)

        // Warten auf Abschluss des Verbindungsaufbaus
        waitForOpen()

        // Ist Verbindung hergestellt?
        if (fsm.currentState == State.S_READY) {
            // Ja
            // Es kann nur eine Verbindung unterhalten werden,
            // deshalb ist die Nummer beliebig
            connId = 1

            Utils.writeLog("TcpLayer", "open", "Verbindung wurde geöffnet: ${connId}", 2)
        } else {

            Utils.writeLog("TcpLayer", "open", "Verbindungsaufbau fehlgeschlagen", 2)
            connId = 0
        }
        return connId
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Wartet auf eingehende Verbindungsanforderung.
     * @return Map: keys: [connId:, dstIpAddr:, dstPort:] oder "null"
     */
    Map listen() {

        Utils.writeLog("TcpLayer", "receiving", "warte auf Verbindung an Port: ${ownPort}", 2)

        // Bei Fehler wird null geliefert
        Map conn = null
        // Ist eine Verbindung aktiv?
        if (fsm.currentState == State.S_IDLE) {
            // Nein
            // Warten auf Verbindungsanforderung
            waitForOpen()
            // Es kann nur eine Verbindung unterhalten werden,
            // deshalb ist die Nummer eigentlich beliebig
            connId = 1
            conn = [connId: connId, srcIpAddr: dstIpAddr, srcPort: dstPort]
            Utils.writeLog("TcpLayer", "receiving", "Verbindung wurde geöffnet: ${conn}", 2)

        }
        return conn
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Schliesst eine Verbindung
     * @param idu : keys: connId
     */
    void close(Map idu) {

        Utils.writeLog("TcpLayer", "close", "schliesse Verbindung: ${idu}", 2)

        // ID der Verbindung
        if (idu.connId == connId) {
            // Ja
            // Verbindung schliessen
            // Auftrag zum Verbindungsabbruch
            AT_IDU at_idu = new AT_IDU()
            at_idu.connId = idu.connId
            at_idu.command = CLOSE
            fromAppQ.put(at_idu)

            // Warten auf Beendigung
            waitForClose()

            Utils.writeLog("TcpLayer", "close", "Verbindung wurde geschlossen: ${idu.connId}", 2)
        }
    }

    //------------------------------------------------------------------------------


    // Steuerung ====================================================================
    //------------------------------------------------------------------------------
    /**
     * Liefert die Message-Queue in Senderichtung
     * @return fromAppQ
     */
    MQueue<AT_IDU> getFromAppQ() {
        return fromAppQ
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Liefert die Message-Queue in Empfangsrichtung
     * @return fromIpQ
     */
    MQueue<IT_IDU> getFromIpQ() {
        return fromIpQ
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Initialisieren und Starten der Schicht
     * @param toAppQ message queue zur Anwendung
     * @param toIpQ  message queue zur IP-Schicht
     * @param config Konfiguration
     */
    void start(MQueue<TA_IDU> toAppQ, MQueue<TRI_IDU> toIpQ, ConfigObject config) {

        // Parameteruebernahme
        this.toAppQ = toAppQ
        this.toIpQ = toIpQ

        // Konfiguration laden
        ownPort = config.ownPort
        timeOut = config.timeOut
        deltaTimeOut = config.deltaTimeOut

        // Initialisieren der FSM
        fsm = new FiniteStateMachine(transitions, State.S_IDLE)

        /** Start der Threads */
        Thread.start { receive() }
        Thread.start { send() }

        // Timer-Thread starten
        // Parameter: als Thread auszuführende Closure, initiale Verzögerung, periodische Verzögerung
        new Timer().schedule({ timeOut() } as TimerTask, deltaTimeOut, deltaTimeOut)
    }

    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Stoppen der Schicht
     */
    void stop() {
        // Threads stoppen
        run = false
    }

    //------------------------------------------------------------------------------
}



