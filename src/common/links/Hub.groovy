package common.links

/**
 * Konstanten, zur Konfiguration eines Anschlusses (siehe Datei "config") an einen LAN-HUB im Zusammenhang mit "ConnectorToVirtual".
 */
class Hub {
    /**
     * Diese Konstanten müssen zur Bildung der virtuellen LAN verwendet werden.<br/>
     * Es sind max. 9 HUB's mit je 6 HUB-Anschluessen (HUB-Ports) und damit 9 LAN's verfuegbar.
     */

    public final static int HUB_1 = 0
    public final static int HUB_2 = 1
    public final static int HUB_3 = 2
    public final static int HUB_4 = 3
    public final static int HUB_5 = 4
    public final static int HUB_6 = 5
    public final static int HUB_7 = 6
    public final static int HUB_8 = 7
    public final static int HUB_9 = 8

    // ---------------------------------------------------------------------------

    /** Die Anschluesse des HUB's. */

    public final static int HUB_PORT_1 = 0
    public final static int HUB_PORT_2 = 1
    public final static int HUB_PORT_3 = 2
    public final static int HUB_PORT_4 = 3
    public final static int HUB_PORT_5 = 4
    public final static int HUB_PORT_6 = 5
}
