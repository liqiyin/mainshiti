### 链接

[什么时候使用CountDownLatch](http://www.importnew.com/15731.html)  
[Java CountDownLatch应用](http://zapldy.iteye.com/blog/746458)

---

CountDownLatch 是一个同步工具类，它允许一个或多个线程一直等待，直到其他线程的操作执行完后再执行。

CountDownLatch 是通过一个计数器来实现的，计数器的初始值为线程的数量。每当一个线程完成了自己的任务后，计数器的值就会减 1。当计数器值到达 0 时，它表示所有的线程已经完成了任务，然后在闭锁上等待的线程就可以恢复执行任务。

### 使用场景

CountDownLatch 的一个非常典型的应用场景是：有一个任务想要往下执行，但必须要等到其他的任务执行完毕后才可以继续往下执行。假如我们这个想要继续往下执行的任务调用一个 CountDownLatch 对象的 await() 方法，其他的任务执行完自己的任务后调用同一个 CountDownLatch 对象上的 countDown() 方法，这个调用 await() 方法的任务将一直阻塞等待，直到这个 CountDownLatch 对象的计数值减到 0 为止。

举个例子，有三个工人在为老板干活，这个老板有一个习惯，就是当三个工人把一天的活都干完了的时候，他就来检查所有工人所干的活。记住这个条件：三个工人先全部干完活，老板才检查。所以在这里用 Java 代码设计两个类，Worker 代表工人，Boss 代表老板，具体的代码实现如下：

```java
package org.zapldy.concurrent;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Worker implements Runnable{

	private CountDownLatch downLatch;
	private String name;

	public Worker(CountDownLatch downLatch, String name){
		this.downLatch = downLatch;
		this.name = name;
	}

	public void run() {
		this.doWork();
		try{
			TimeUnit.SECONDS.sleep(new Random().nextInt(10));
		}catch(InterruptedException ie){
		}
		System.out.println(this.name + "活干完了！");
		this.downLatch.countDown();

	}

	private void doWork(){
		System.out.println(this.name + "正在干活!");
	}
}
```

```java
package org.zapldy.concurrent;

import java.util.concurrent.CountDownLatch;

public class Boss implements Runnable {

	private CountDownLatch downLatch;

	public Boss(CountDownLatch downLatch){
		this.downLatch = downLatch;
	}

	public void run() {
		System.out.println("老板正在等所有的工人干完活......");
		try {
			this.downLatch.await();
		} catch (InterruptedException e) {
		}
		System.out.println("工人活都干完了，老板开始检查了！");
	}
}
```

```java
package org.zapldy.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CountDownLatchDemo {

	public static void main(String[] args) {
		ExecutorService executor = Executors.newCachedThreadPool();

		CountDownLatch latch = new CountDownLatch(3);

		Worker w1 = new Worker(latch,"张三");
		Worker w2 = new Worker(latch,"李四");
		Worker w3 = new Worker(latch,"王二");

		Boss boss = new Boss(latch);

		executor.execute(w3);
		executor.execute(w2);
		executor.execute(w1);
		executor.execute(boss);

		executor.shutdown();
	}
}
```

当你运行CountDownLatchDemo这个对象的时候，你会发现是等所有的工人都干完了活，老板才来检查。

```
王二正在干活!
李四正在干活!
老板正在等所有的工人干完活......
张三正在干活!
张三活干完了！
王二活干完了！
李四活干完了！
工人活都干完了，老板开始检查了！
```
