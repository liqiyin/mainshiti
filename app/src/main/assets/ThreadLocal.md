### 链接

[理解Java中的ThreadLocal](https://droidyue.com/blog/2016/03/13/learning-threadlocal-in-java/)

---

ThreadLocal 是一个关于创建线程局部变量的类。

通常情况下，我们创建的变量是可以被任何一个线程访问并修改的。而使用 ThreadLocal 创建的变量只能被当前线程访问，其他线程则无法访问和修改。

### 使用

```java
// 创建，支持泛型
ThreadLocal<String> mStringThreadLocal = new ThreadLocal<>();

// set方法
mStringThreadLocal.set("droidyue.com");

// get方法
mStringThreadLocal.get();

// 完整的使用示例
private void testThreadLocal() {
    Thread t = new Thread() {
        ThreadLocal<String> mStringThreadLocal = new ThreadLocal<>();

        @Override
        public void run() {
            super.run();
            mStringThreadLocal.set("droidyue.com");
            mStringThreadLocal.get();
        }
    };

    t.start();
}
```

### ThreadLocal 初始值

如果要为 ThreadLocal 设置默认的 get 初始值，需要重写 initialValue 方法，下面是一段代码，我们将默认值修改成了线程的名字：

```java
ThreadLocal<String> mThreadLocal = new ThreadLocal<String>() {
    @Override
    protected String initialValue() {
      return Thread.currentThread().getName();
    }
};
```

---

### Android中的应用

在Android中，Looper类就是利用了ThreadLocal的特性，保证每个线程只存在一个Looper对象。

```java
static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
private static void prepare(boolean quitAllowed) {
    if (sThreadLocal.get() != null) {
        throw new RuntimeException("Only one Looper may be created per thread");
    }
    sThreadLocal.set(new Looper(quitAllowed));
}
```

---

### 对象存放在哪里

ThreadLocal 的实例和值都是位于堆上，只是通过一些技巧将可见性修改成了线程可见。

---

### InheritableThreadLocal

使用 InheritableThreadLocal 可以实现让子线程访问 ThreadLocal 的值。

使用 InheritableThreadLocal 可以将某个线程的 ThreadLocal 值在其子线程创建时传递过去。因为在线程创建过程中，有相关的处理逻辑。

```java
private void testInheritableThreadLocal() {
    final ThreadLocal threadLocal = new InheritableThreadLocal();
    threadLocal.set("droidyue.com");
    Thread t = new Thread() {
        @Override
        public void run() {
            super.run();
            Log.i(LOGTAG, "testInheritableThreadLocal = " + threadLocal.get());
        }
    };

    t.start();
}
```

上面的代码输出的日志信息为：

```
I/MainActivity( 5046): testInheritableThreadLocal = droidyue.com
```

---

### 使用场景

* 实现单个线程单例以及单个线程上下文信息存储，比如交易id等
* 实现线程安全，非线程安全的对象使用ThreadLocal之后就会变得线程安全，因为每个线程都会有一个对应的实例
* 承载一些线程相关的数据，避免在方法中来回传递参数
