package addons.tcptest

// Portnummer des Servers
int port = 4444

// Name des Servers
String server = "erni.local"

// Netzwerk-Socket erzeugen
def socket = new Socket(server, port)

// Senden und Empfangen
socket.withStreams { input, output ->

    // An den Server senden
    output << "GET /xyz HTTP/1.1\n\n"

    // Antwort vom Server erwarten
    def buffer = input.newReader().readLine()

    // Antwort anzeigen
    println "Server hat geantwortet: $buffer"
}