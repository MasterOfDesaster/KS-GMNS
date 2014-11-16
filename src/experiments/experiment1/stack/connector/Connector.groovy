package experiments.experiment1.stack.connector

import experiments.experiment1.stack.layer.idu.LC_IDU

/**
 * Elternklasse für Netzwerkanschlüsse
 */
abstract class Connector {

    //------------------------------------------------------------------------------
    /** Max. Länge von zu übertragende Daten.
     *  Ein zu sendendes Paket darf diese Länge nicht überschreiten.
     */
    public final static int MTU = 512
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    abstract void send(LC_IDU lc_idu)
    abstract void start()
    abstract void stop()
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    String getMacAddr() {
        return macAddr
    }
    //------------------------------------------------------------------------------
}