package addons.threadtest

void count() {
	for (i in 1..10) {
		sleep((Math.random() * 1000) as int)
		println(i)
    }
}

count()
