package addons.threadtest

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

// Thread Synchronisierung mittels Condition-Objekt

class ThreadTest6 {
  ReentrantLock lock = new ReentrantLock()
  Condition cond = lock.newCondition()

  String data = ""

  void schreib() {
    println("schreib: startet")
    lock.lock()
    data = "Hallo Welt!"
    cond.signal()
    lock.unlock()
  }

  void lies() {
    println("lies: startet")
    lock.lock()
    if (data == "")
      cond.await()
    println("lies: $data")
    lock.unlock()
  }
}

def klasse = new ThreadTest6()

Thread t1 = new Thread({klasse.lies()})
Thread t2 = new Thread({klasse.schreib()})

t1.start()
sleep(2000)
t2.start()

t1.join()
t2.join()

