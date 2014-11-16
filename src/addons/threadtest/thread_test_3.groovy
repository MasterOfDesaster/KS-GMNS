package addons.threadtest

void count(String name) {
	for (i in 1..10) {
      println("${name}, warten: ${i}")
      sleep((Math.random() * 1000) as int)
      println("${name}, weiter: ${i}")
    }
}

Thread t1 = new Thread({count("t1")})
Thread t2 = new Thread({count("t2")})
t1.start()
sleep(1000)
t2.start()
t1.join()
t2.join()
