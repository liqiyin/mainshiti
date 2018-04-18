### 链接

<http://thomaschen2011.iteye.com/blog/1468085>

---

java.util.concurrent.atomic 包里面提供了一组原子类。其基本的特性就是在多线程环境下，当有多个线程同时执行这些类的实例包含的方法时，具有排他性，即当某个线程进入方法，执行其中的指令时，不会被其他线程打断，而别的线程就像自旋锁一样，一直等到该方法执行完成，才由JVM从等待队列中选择一个另一个线程进入，这只是一种逻辑上的理解。

其中的类可以分成4组

* AtomicBoolean，AtomicInteger，AtomicLong，AtomicReference
* AtomicIntegerArray，AtomicLongArray
* AtomicLongFieldUpdater，AtomicIntegerFieldUpdater，AtomicReferenceFieldUpdater
* AtomicMarkableReference，AtomicStampedReference，AtomicReferenceArray

---

### AtomicBoolean , AtomicInteger, AtomicLong, AtomicReference

这四种基本类型用来处理布尔，整数，长整数，对象四种数据。

#### 构造函数（两个构造函数）

* 默认的构造函数：初始化的数据分别是false，0，0，null
* 带参构造函数：参数为初始化的数据

#### set()和get()方法

可以原子地设定和获取atomic的数据。类似于volatile，保证数据会在主存中设置或读取

#### getAndSet()方法

原子的将变量设定为新数据，同时返回先前的旧数据
其本质是get()操作，然后做set()操作。尽管这2个操作都是atomic，但是他们合并在一起的时候，就不是atomic。在Java的源程序的级别上，如果不依赖synchronized的机制来完成这个工作，是不可能的。只有依靠native方法才可以。

#### compareAndSet() 和weakCompareAndSet()方法

这两个方法都是conditional modifier方法。这2个方法接受2个参数，一个是期望数据(expected)，一个是新数据(new)；如果atomic里面的数据和期望数据一致，则将新数据设定给atomic的数据，返回true，表明成功；否则就不设定，并返回false。

#### 对于AtomicInteger、AtomicLong还提供了一些特别的方法

getAndIncrement( )、incrementAndGet( )、getAndDecrement( )、decrementAndGet ( )、addAndGet( )、getAndAdd( )以实现一些加法，减法原子操作。
