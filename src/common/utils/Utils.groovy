package common.utils

import jpcap.JpcapCaptor
import jpcap.JpcapSender
import jpcap.NetworkInterface

/**
 * Eine Sammlung von nützlichen Methoden.
 */
class Utils {

    /* Zeitkonstanten */
    /** Zeitkonstante 0.5s usw. */
    static final long sec05 = 500, sec1 = 1000, sec2 = 2000, sec5 = 5000, sec10 = 10000

    /** Maximale Message-Queue Länge */
    static final int MAXQUEUE = 10


    // ======================================================================== =

    /**
     * Zeigt eine Liste der verfügbaren Netzwerk-Devices (-Interfaces) an.<br/>
     * libjpcap.so muss installiert sein (setzt für Installation und Verwendung Administartorrechte voraus!)
     */
    public static void listDevices() {
        //Obtain the list of network interfaces
        List<NetworkInterface> devices = JpcapCaptor.getDeviceList()

        //for each network interface
        devices.each { device ->
            //print out its name and description
            println("${device.name} (${device.description})")

            //print out its datalink name and description
            println(" datalink: ${device.datalink_name} (${device.datalink_description ?: "Keine Beschreibung"})")

            //print out its MAC address
            print(" MAC address:")

            device.mac_address.each { address ->
                print("${Integer.toHexString((address & 0xff) as int)} :")
            }

            println()

            //print out its IP address, subnet mask and broadcast address
            device.addresses.each {
                println(" address: ${it.address} ${it.subnet} ${it.broadcast}")
            }
            println()
        }
    }

    //-------------------------------------------------------------------------------

    /**
     * Liefert ein Netzwerk-Device-Objekt basierend auf einem Netzwerk-Interface des Betriebssystems.<br/>
     * libjpcap.so muss installiert sein (setzt für Installation und Verwendung Administartorrechte voraus!)
     * @param name Name des Name des Netzwerk-Interfaces, z.B. "eth0", "en0" "lo0", "lan1".
     * Die Namen erfährt man z.B. unter Unix durch das Kommando "ifconfig"
     * @return Das Netzwerk-Device-Objekt
     */
    public static NetworkInterface getDevice(String name) {
        //Obtain the list of network interfaces
        List<NetworkInterface> devices = JpcapCaptor.getDeviceList()
        NetworkInterface device = devices.find {
            it.name == name
        }
        return device
    }

    //-------------------------------------------------------------------------------

    /**
     * Öffnet ein Netzwerk-Device des Betriebssystems zur Verwendung durch Jpcap.<br/>
     * libjpcap.so muss installiert sein (setzt für Installation und Verwendung Administartorrechte voraus!)
     * @param device das Netzwerk-Device-Objekt
     * @return Liefert ein Jpcap-Sender-Objekt
     */
    public static JpcapSender openDevice(NetworkInterface device) {
        //open a network interface to send a packet to
        return JpcapSender.openDevice(device)
    }

    // ======================================================================== =

    /**
     * Konvertiert eine Zeichenkettendarstellung einer MAC-Adresse in ein Bytefeld.
     * @param sMac String in der Form "01:02:03:04:05:06"
     * @return Bytefeld in der Form [1,2,3,4,5,6]
     */
    public static byte[] stringToMac(String sMac) {
        return sMac.replace(':', '').decodeHex()
    }

    /**
     * Konvertiert eine MAC-Adresse in Bytefeld-Form in eine Zeichenkettendarstellung.
     * @param bMac Bytefeld in der Form [1,2,3,4,5,6]
     * @return String in der Form "01:02:03:04:05:06"
     */
    public static String macToString(byte[] bMac) {
        return sprintf("%02x:%02x:%02x:%02x:%02x:%02x", bMac[0], bMac[1], bMac[2], bMac[3], bMac[4], bMac[5])
    }

    //-------------------------------------------------------------------------------

    /**
     * Konvertiert eine Zeichenkettendarstellung einer IP-Adresse in ein Bytefeld.
     * @param bIp Bytefeld in der Form [1,2,3,4]
     * @return Ipv4-Adresse in DQN
     */
    public static String ipToString(byte[] bIp) {
        return InetAddress.getByAddress(bIp).getHostAddress()
    }

    //-------------------------------------------------------------------------------

    /**
     * Konvertiert eine Zeichenkettendarstellung einer IPv4-Adresse in ein Bytefeld.
     * @param sIp String in der Form "10.1.34.240"
     * @return Bytefeld in der Form [10,1,34,240]
     */
    public static byte[] stringToIp(String sIp) {
        return InetAddress.getByName(sIp).getAddress()
    }

    //-------------------------------------------------------------------------------

    /**
     * Liefert die UND-Verknuepfung einer IP-Adresse und einer Netzmaske/Präfix
     * @param ipAddr Eine IP-Adresse
     * @param netMask Eine Subnetzmaske bzw. ein Netzwerkpräfix
     * @return Die UND-Verknuepfung von IP-Adresse und Subnetzmaske
     */
    public static String getNetworkId(String ipAddr, String netMask) {
        byte[] band = new byte[4]

        byte[] bip = stringToIp(ipAddr)
        byte[] bim = stringToIp(netMask)

        (0..3).each {i ->
            band[i] = bip[i] & bim[i]
        }
        return InetAddress.getByAddress(band).getHostAddress()
    }

    // ======================================================================== =

    /**
     Zerteilt die Daten in max. size grosse Teile (Pakete),
     liefert eine Liste mit den Paketen

     @param daten die zu zerteilenden Daten
     @param size max. Länge der Pakete
     @return die Liste mit maximal size-langen Datenfragmenten
     */
    public static List fragment(byte[] daten, int size) {
        // Leere Liste erzeugen
        List pakete = []
        int delta
        int i = 0
        int l = daten.size()

        while (l > 0) {
            if ((l - size) > 0) {
                // Paket wird size Byte lang
                delta = size - 1
                l -= size
            } else {
                delta = l - 1
                l = 0
            }
            byte[] p = new byte[delta + 1]
            System.arraycopy(daten[i..i + delta] as byte[], 0, p, 0, delta + 1)
            pakete.add(p)
            i += size
        }
        return pakete
    }

    //-------------------------------------------------------------------------------

    /**
     * Fügt zwei Bytefelder zusammen.
     * @param a Bytefeld 1
     * @param b Bytefeld 2
     * @return Bytefeld mit Inhalten von a und b
     */
    public static byte[] concatenateByteArrays(byte[] a, byte[] b) {
        return reassemble([a, b])
    }

    // ------------------------------------------------------ =

    /**
     Fügt die Elemente (Byte-Arrays) einer Liste zusammen.

     @param pakete Liste mit Datenfragmenten
     @return Daten als ByteArray
     */
    public static byte[] reassemble(List pakete) {
        byte[] daten
        int laenge = 0
        int offset = 0
        int len

        // Gesamtlänge der Daten bestimmen und ByteArray anlegen
        for (teil in pakete) {
            laenge += (teil as byte[]).length
        }

        // Beispiel für "groovysche" Notationen der Schleife:
        // pdu.each {
        //   laenge += (it as byte[]).length
        // }
        // oder:
        // laenge = pdu.inject(0) {len, p -> len += (p as byte[]).length} as int }

        daten = new byte[laenge]

        for (teil in pakete) {
            len = (teil as byte[]).length
            System.arraycopy(teil, 0, daten, offset, len)
            offset += len
        }
        return daten
    }

    // ======================================================================== =

    /** Debug-Level */
    static List<Integer> debugLevel

    /** Anzeigeverzögerung in MilliSekunden */
    static int debugDelay

    /**
     Schreibt eine Protokollmeldung formatiert an die Standard-Ausgabe.
     <p/>
     Verwendung:<br>
     writeLog("Klasse A", "Methode B", "Hier", 3)

     @param klass Name der Klasse, in der die Meldung erzeugt wird
     @param method Name der Methode, welche die Meldung erzeugt
     @param comment die Meldung
     @param dbgLvl befindet sich der angegebene Wert in der Liste debugLevel, wird die Meldung ausgegeben
     */
    public static void writeLog(String klass, String method, String comment, int dbgLvl) {
        long ms = System.currentTimeMillis() % 1000

        if (dbgLvl in debugLevel) {
            printf("%8s.%03d: %10s - %10s - %s\n", [new Date().format("HH-mm-ss"),
                    ms,
                    klass.size() <= 10 ? klass : klass[0..9],
                    method.size() <= 10 ? method : method[0..9],
                    comment])

            // u.U. Verzögern
            if (debugDelay > 0) sleep(debugDelay)
        }
    }

    // ==========================================================================

    /**
     * Liefert die Konfiguration fuer den Versuch "test" und das Gerät "host"
     * @param host
     * @param environment
     * @return ConfigObject
     */
    static ConfigObject getConfig(String test, String host) {
        String path

        // Ist "-D stand.alone" in der Kommandozeile enthalten?
        if (System.getProperty("stand.alone"))
            // Ja
            path = new File("../../../../../src/experiments/${test}/config").text // Konfigurationsdatei lesen
        else
            // Nein
            path = new File("src/experiments/${test}/config").text // Konfigurationsdatei lesen

        // ConfigObject zusammenstellen
        ConfigObject co1 = new ConfigSlurper().parse(path)
        ConfigObject co2 = new ConfigSlurper(co1.environment).parse(path)
        co1.merge(co2[host] as ConfigObject)

        // Konfigurieren der Meldungen
        debugLevel = co2.debugLevel
        debugDelay = co2.debugDelay

        return co1
    }

    // ==========================================================================

    /**
     * Liest eine Zeile von der Standard-Eingabe
     *
     * @return Eingabezeile
     */
    public static String readLine() {
        return System.in.newReader().&readLine()
    }
}
