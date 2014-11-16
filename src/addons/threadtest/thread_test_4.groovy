package addons.threadtest

// Freigabe eines Threads durch einen anderen

import java.util.concurrent.CountDownLatch

class ThreadTest4 {
  CountDownLatch event = new CountDownLatch(1)
  String data = ""

  void erster() {
    println("erster gestartet")

    // hier wird thread angehalten
    event.await()
    println("erster:  ${data}")
  }

  void zweiter() {
    println("zweiter gestartet")
    data = "Hallo Welt!"

    // hier wird erster freigegeben
    event.countDown()
  }
}

def klasse = new ThreadTest4()

// erster startet zuerst, muss aber auf die Freigabe durch zweiter warten
Thread erster = new Thread({klasse.erster()})
Thread zweiter = new Thread({klasse.zweiter()})

erster.start()
sleep(1000)
zweiter.start()

[erster,zweiter].join()
