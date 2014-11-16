package addons.threadtest

void count() {
  for (i in 1..10) {
    sleep((Math.random() * 1000) as int)
    println(i)
  }
}

Thread t1 = new Thread({count()})
t1.start()
t1.join()