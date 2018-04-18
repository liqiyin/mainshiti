### 链接

<https://www.jianshu.com/p/de2ff82b37b3>
<https://www.jianshu.com/p/5b6c71a7e8d7>

---

### new Thread 的缺点

在日常开发中，我们经常会通过 `new Thread(){}.start();` 的方式来开辟一个新的线程。但是如果我们想要多次执行任务的时候，通过这种方式我就会创建多个线程，这样会使我们的程序运行起来越来越慢。通常情况下采用HandlerThread的方式来开辟一个线程。

### HandlerThread 介绍

HandlerThread是Thread的一个子类，HandlerThread自带Looper使他可以通过消息队列来重复使用当前线程，节省系统资源开销。这是它的优点也是缺点，每一个任务都将以队列的方式逐个被执行到，一旦队列中有某个任务执行时间过长，那么就会导致后续的任务都会被延迟处理。

### HandlerThread 使用方法

新建HandlerThread并且执行start()

```java
private HandlerThread mHandlerThread;
......
mHandlerThread = new HandlerThread("HandlerThread");
handlerThread.start();
```

创建Handler，使用mHandlerThread.getLooper()生成Looper：

```java
final Handler handler = new Handler(mHandlerThread.getLooper()){
    @Override
    public void handleMessage(Message msg) {
        System.out.println("收到消息");
    }
};
```

然后再新建一个子线程来发送消息：

```java
new Thread(new Runnable() {
    @Override
    public void run() {
        try {
            Thread.sleep(1000);//模拟耗时操作
            handler.sendEmptyMessage(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}).start();
```

最后一定不要忘了在onDestroy释放,避免内存泄漏：

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    mHandlerThread.quit();
}
```

### 整体流程

当我们使用HandlerThread创建一个线程，它statr()之后会在它的线程创建一个Looper对象且初始化了一个MessageQueue(消息队列），通过Looper对象在他的线程构建一个Handler对象，然后我们通过Handler发送消息的形式将任务发送到MessageQueue中，因为Looper是顺序处理消息的，所以当有多个任务存在时就会顺序的排队执行。当我们不使用的时候我们应该调用它的quit()或者quitSafely()来终止它的循环。
