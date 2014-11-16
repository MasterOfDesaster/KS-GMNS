package addons.threadtest

import java.util.concurrent.Semaphore

// Über eine Semaphore wird gesteuert, dass immer nur 3 Threads gleichzeitig laufen

class ThreadTest8 {

  Semaphore sema = new Semaphore(3)

  void etwasTun(String name) {
    while (true) {
      sema.acquire()
      println("$name, gestartet")
      sleep(3000)
      println("$name, gestoppt")
      sema.release()
      sleep ((Math.random() * 1000) as int)
    }
  }
}

def klasse = new ThreadTest8()

Thread t1 = new Thread({klasse.etwasTun("t1")})
Thread t2 = new Thread({klasse.etwasTun("t2")})
Thread t3 = new Thread({klasse.etwasTun("t3")})
Thread t4 = new Thread({klasse.etwasTun("t4")})
Thread t5 = new Thread({klasse.etwasTun("t5")})

t1.start()
t2.start()
t3.start()
t4.start()
t5.start()
