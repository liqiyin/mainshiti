### 链接

[Java：多线程，线程池，使用CompletionService通过Future来处理Callable的返回结果
](http://www.cnblogs.com/nayitian/p/3273468.html)

---

在 Java5 的多线程中，可以使用 Callable 接口来实现具有返回值的线程。使用线程池的 submit 方法提交 Callable 任务，并通过返回的 Future 对象的 get 方法来获取任务的运行结果。

如果你要获取所有任务的返回结果，有两种方法可以实现：

方法一：自己维护一个 Collection 保存 submit 方法返回的 Future 对象，然后在主线程中遍历这个 Collection 并调用 Future 对象的 get() 方法取到线程的返回值。

方法二：使用 CompletionService 类。你可以将 Callable 任务提交给它去执行，然后使用类似于队列中的 take 方法获取线程的返回值。

```java
package com.clzhang.sample.thread;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPoolTest4 {
    // 具有返回值的测试线程
    class MyThread implements Callable<String> {
        private String name;
        public MyThread(String name) {
            this.name = name;
        }

        @Override
        public String call() {
            int sleepTime = new Random().nextInt(1000);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 返回给调用者的值
            String str = name + " sleep time:" + sleepTime;
            System.out.println(name + " finished...");

            return str;
        }
    }

    private final int POOL_SIZE = 5;
    private final int TOTAL_TASK = 20;

    // 方法一，自己写集合来实现获取线程池中任务的返回结果
    public void testByQueue() throws Exception {
        // 创建线程池
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        BlockingQueue<Future<String>> queue = new LinkedBlockingQueue<Future<String>>();

        // 向里面扔任务
        for (int i = 0; i < TOTAL_TASK; i++) {
            Future<String> future = pool.submit(new MyThread("Thread" + i));
            queue.add(future);
        }

        // 检查线程池任务执行结果
        for (int i = 0; i < TOTAL_TASK; i++) {
            System.out.println("method1:" + queue.take().get());
        }

        // 关闭线程池
        pool.shutdown();
    }

    // 方法二，通过CompletionService来实现获取线程池中任务的返回结果
    public void testByCompetion() throws Exception {
        // 创建线程池
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        CompletionService<String> cService = new ExecutorCompletionService<String>(pool);

        // 向里面扔任务
        for (int i = 0; i < TOTAL_TASK; i++) {
            cService.submit(new MyThread("Thread" + i));
        }

        // 检查线程池任务执行结果
        for (int i = 0; i < TOTAL_TASK; i++) {
            Future<String> future = cService.take();
            System.out.println("method2:" + future.get());
        }

        // 关闭线程池
        pool.shutdown();
    }

    public static void main(String[] args) throws Exception {
        ThreadPoolTest4 t = new ThreadPoolTest4();
        t.testByQueue();
        t.testByCompetion();
    }
}
```

使用方法一，自己创建一个集合来保存 Future 对象并循环调用其返回结果的时候，主线程并不能保证首先获得的是最先完成任务的线程返回值。它只是 **按加入线程池的顺序返回**。因为 take 方法是阻塞方法，后面的任务完成了，前面的任务却没有完成，主程序就那样等待在那儿，只到前面的完成了，它才知道原来后面的也完成了。

使用方法二，使用 CompletionService 来维护处理线程不的返回结果时，主线程总是能够拿到 **最先完成的任务的返回值**，而不管它们加入线程池的顺序。
