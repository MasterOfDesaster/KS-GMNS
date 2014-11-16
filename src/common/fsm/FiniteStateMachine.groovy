package common.fsm

/**
 * Endliche Zustands Maschine. <p/>
 * Steuert die Zustandwechsel der TCP-Protokollmaschine<p/>
 * Verwendung:<br/>
 * <pre>
 *     List transitions = [[on: eventB, from: state1, to: state2],
 *                         [on: eventA, from: state2, to: state1]]
 *
 *     FiniteStateMachine fsm = new FiniteStateMachine(transitions, state1)
 *     ...
 *     newstate = state1
 *     loop {
 *       switch (newState) {
 *         case (state1):
 *         print "Flip "
 *         event = eventB
 *         break
 *         case (state2):
 *         Print "Flop "
 *         event = eventA
 *         break
 *       }
 *       newState = fsm.fire(event)
 *     }
 * </pre>
 *     Bei einem nicht möglichen Zustandswechsel wird eine 0 geliefert
 */
class FiniteStateMachine {
    /**
    * Speichert Ereignisse und zugehörige Zustandsübergänge
    **/
    Map transitions = [:]

    int initialState
    int currentState

    /**
     * Ereignisse und Zustände sind in {@link Event} und {@link State} definiert
     *
     * @param transList List von Maps mit möglichen Zustandsübergängen
     * Format:<pre>
     * [[on: Ereignis, from: Zustand, to: neuer Zustand], [...]]</pre>
     *
     * @param a_initialState Initialzustand
     */
    FiniteStateMachine(List transList, int a_initialState) {
        assert a_initialState, "You need to provide an initial state"

        initialState = a_initialState
        currentState = a_initialState

        transList.each {Map it ->
            record().on(it.on as int).from(it.from as int).to(it.to as int)
        }
    }

    Grammer record() {
        return new Grammer(this)
    }

    void registerTransition(Grammer a_grammer) {
        assert a_grammer.isValid(), "Invalid transition (${a_grammer})"

        Map transition

        int event = a_grammer.event
        int fromState = a_grammer.fromState
        int toState = a_grammer.toState

        if (!transitions[event]) {
            transitions[event] = [:]
        }

        transition = transitions[event] as Map
        assert !transition[fromState], "Duplicate fromState ${fromState} for transition ${a_grammer}"

        transition[fromState] = toState
    }

    /**
     * Rücksetzen der Zustandsmaschine
     */
    void reset() {
        currentState = initialState
    }

    /**
     * Führt aufgrund des übergebenen Ereignisses einen Zustandswechsel aus
     *
     * @throws Exception bei üngültigem Event oder ungültigem Zustand
     * @param a_event Ein Ereigniss
     * @return Neuer Zustand oder 0 bei nicht möglichem Zustandswechsel
     */
    int fire(int a_event) {
        assert currentState, "Invalid current state '${currentState}': pass into constructor"
        assert transitions.containsKey(a_event), "Invalid event '${a_event}', should be one of ${transitions.keySet()}"

        Map transition = transitions[a_event] as Map

        int nextState = 0
        if (transition[currentState]) {
            nextState = transition[currentState] as int
        }

        //assert nextState, "There is no transition from '${currentState}' to any other state"
        if (nextState)
            currentState = nextState

        return nextState
    }

    /**
     * Setzt des Zustand
     *
     * @param a_state Neuer Zustand
     */
    void setState(int a_state) {
        currentState = a_state
    }

    /**
     * Liefert den aktuellen Zustand
     * @return Aktueller Zustand
     */
    int getState() {
        return currentState
    }
}
