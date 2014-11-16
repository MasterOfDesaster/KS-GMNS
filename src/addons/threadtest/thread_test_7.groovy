package addons.threadtest

import java.util.concurrent.LinkedBlockingQueue

// Thread Synchronisierung und Datenaustausch mittel blockierender Message-Queue

class ThreadTest7 {

  LinkedBlockingQueue q = new LinkedBlockingQueue()
  String text = ""

  void lies() {
    println("lies: gestartet")
    text = q.take()
    println("lies: $text")
  }

  void schreib() {
    println("schreib: gestartet")
    q.put("Hallo Welt!")
  }
}

def klasse = new ThreadTest7()

Thread t1 = new Thread({klasse.lies()})
Thread t2 = new Thread({klasse.schreib()})

t1.start()
sleep(2000)
t2.start()

t1.join()
t2.join()
