package common.fsm

/**
 * Grammatik der Zustandsmaschine.<br/>
 * Beschreibt einen einzelnen Zustandsübergang
 */
class Grammer {
    FiniteStateMachine fsm

    int event
    int fromState
    int toState

    Grammer(FiniteStateMachine a_fsm) {
        fsm = a_fsm
    }

    Grammer on(int a_event) {
        event = a_event
        return this
    }

    Grammer from(int a_fromState) {
        fromState = a_fromState
        return this
    }

    Grammer to(int a_toState) {
        assert a_toState, "Invalid toState: ${a_toState}"

        toState = a_toState
        fsm.registerTransition(this)
        return this
    }

    boolean isValid() {
        return event && fromState && toState
    }

    public String toString() {
        return "${event}:${fromState}=>${toState}"
    }
}

