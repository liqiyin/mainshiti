### 链接

<http://www.cnblogs.com/dolphin0520/p/3923167.html>

---

### synchronized 的缺陷

如果一个代码块被 synchronized 修饰了，当一个线程获取了对应的锁，并执行该代码块时，其他线程便只能一直等待，等待获取锁的线程释放锁，而这里获取锁的线程释放锁只会有两种情况：
1. 获取锁的线程执行完了该代码块，然后线程释放对锁的占有；
2. 线程执行发生异常，此时 JVM 会让线程自动释放锁。

那么如果这个获取锁的线程由于要等待 IO 或者其他原因（比如调用 sleep 方法）被阻塞了，但是又没有释放锁，其他线程便只能干巴巴地等待，试想一下，这多么影响程序执行效率。

因此就需要有一种机制可以不让等待的线程一直无期限地等待下去（比如只等待一定的时间或者能够响应中断），通过 Lock 就可以办到。

当有多个线程读写文件时，读操作和写操作会发生冲突现象，写操作和写操作会发生冲突现象，但是读操作和读操作不会发生冲突现象。

但是采用 synchronized 关键字来实现同步的话，就会导致一个问题：
如果多个线程都只是进行读操作，所以当一个线程在进行读操作时，其他线程只能等待无法进行读操作。

因此就需要一种机制来使得多个线程都只是进行读操作时，线程之间不会发生冲突，通过 Lock 就可以办到。

要注意以下几点：
1. Lock 不是 Java 语言内置的，synchronized 是 Java 语言的关键字，因此是内置特性。Lock 是一个类，通过这个类可以实现同步访问；
2. Lock 和 synchronized 有一点非常大的不同，采用 synchronized 不需要用户去手动释放锁，当 synchronized 方法或者 synchronized 代码块执行完之后，系统会自动让线程释放对锁的占用；而 Lock 则必须要用户去手动释放锁，如果没有主动释放锁，就有可能导致出现死锁现象。

---

## java.util.concurrent.locks 包中常用的类和接口

### Lock

```java
public interface Lock {
    // 获取锁。如果锁已被其他线程获取，则进行等待。
    void lock();  

    // 当通过这个方法去获取锁时，如果线程正在等待获取锁，则这个线程能够响应中断，即中断线程的等待状态。也就是说，当两个线程同时通过 lock.lockInterruptibly() 想获取某个锁时，假若此时线程 A 获取到了锁，而线程 B 只有在等待，那么对线程 B 调用 threadB.interrupt() 方法能够中断线程 B 的等待过程。
    void lockInterruptibly() throws InterruptedException;

    // 尝试获取锁，如果获取成功，则返回 true，如果获取失败（即锁已被其他线程获取），则返回 false，也就说这个方法无论如何都会立即返回。在拿不到锁时不会一直在那等待。
    boolean tryLock();  

    // 和 tryLock() 方法是类似的，只不过区别在于这个方法在拿不到锁时会等待一定的时间，在时间期限之内如果还拿不到锁，就返回 false。如果如果一开始拿到锁或者在等待期间内拿到了锁，则返回 true。
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;  

    void unlock();

    Condition newCondition();
}
```

由于在前面讲到如果采用 Lock，必须主动去释放锁，并且在发生异常时，不会自动释放锁。因此一般来说，使用 Lock 必须在 try{}catch{} 块中进行，并且将释放锁的操作放在 finally 块中进行，以保证锁一定被被释放，防止死锁的发生。通常使用 Lock 来进行同步的话，是以下面这种形式去使用的：

```java
Lock lock = ...;
lock.lock();
try{
    // 处理任务
}catch(Exception ex){

}finally{
    lock.unlock();   // 释放锁
}
```

当一个线程获取了锁之后，是不会被 interrupt() 方法中断的。因为本身在前面的文章中讲过单独调用 interrupt() 方法不能中断正在运行过程中的线程，只能中断阻塞过程中的线程。
因此当通过 lockInterruptibly() 方法获取某个锁时，如果不能获取到，只有进行等待的情况下，是可以响应中断的。

### ReentrantLock

意思是 “可重入锁”。ReentrantLock 是唯一实现了 Lock 接口的类，并且 ReentrantLock 提供了更多的方法。

```java
Lock lock = new ReentrantLock();
```

### ReadWriteLock

ReadWriteLock 也是一个接口，在它里面只定义了两个方法：

```java
public interface ReadWriteLock {
    /**
     * Returns the lock used for reading.
     *
     * @return the lock used for reading.
     */
    Lock readLock();

    /**
     * Returns the lock used for writing.
     *
     * @return the lock used for writing.
     */
    Lock writeLock();
}
```

一个用来获取读锁，一个用来获取写锁。也就是说将文件的读写操作分开，分成 2 个锁来分配给线程，从而使得多个线程可以同时进行读操作。下面的 ReentrantReadWriteLock 实现了 ReadWriteLock 接口。

### ReentrantReadWriteLock

主要的有两个方法：readLock() 和 writeLock() 用来获取读锁和写锁。

写的时候不能读，读的时候不能写，读的时候可以读。

如果有一个线程已经占用了读锁，则此时其他线程如果要申请写锁，则申请写锁的线程会一直等待释放读锁。

如果有一个线程已经占用了写锁，则此时其他线程如果申请写锁或者读锁，则申请的线程会一直等待释放写锁。

---

### Lock 和 synchronized 的选择

1. Lock 是一个接口，而 synchronized 是 Java 中的关键字，synchronized 是内置的语言实现；
2. synchronized 在发生异常时，会自动释放线程占有的锁，因此不会导致死锁现象发生；而 Lock 在发生异常时，如果没有主动通过 unLock() 去释放锁，则很可能造成死锁现象，因此使用 Lock 时需要在 finally 块中释放锁；
3. Lock 可以让等待锁的线程响应中断，而 synchronized 却不行，使用 synchronized 时，等待的线程会一直等待下去，不能够响应中断；
4. 通过 Lock 可以知道有没有成功获取锁，而 synchronized 却无法办到。
5. Lock 可以提高多个线程进行读操作的效率。

---

## 锁的相关概念

### 可重入锁

如果锁具备可重入性，则称作为可重入锁。像 synchronized 和 ReentrantLock 都是可重入锁，可重入性在我看来实际上表明了锁的分配机制：基于线程的分配，而不是基于方法调用的分配。举个简单的例子，当一个线程执行到某个 synchronized 方法时，比如说 method1，而在 method1 中会调用另外一个 synchronized 方法 method2，此时线程不必重新去申请锁，而是可以直接执行方法 method2。

```java
class MyClass {
    public synchronized void method1() {
        method2();
    }

    public synchronized void method2() {

    }
}
```

上述代码中的两个方法 method1 和 method2 都用 synchronized 修饰了，假如某一时刻，线程 A 执行到了 method1，此时线程 A 获取了这个对象的锁，而由于 method2 也是 synchronized 方法，假如 synchronized 不具备可重入性，此时线程 A 需要重新申请锁。但是这就会造成一个问题，因为线程 A 已经持有了该对象的锁，而又在申请获取该对象的锁，这样就会线程 A 一直等待永远不会获取到的锁。

而由于 synchronized 和 Lock 都具备可重入性，所以不会发生上述现象。

如何实现可重入锁?

为每个锁关联一个获取计数器和一个所有者线程, 当计数值为 0 的时候, 这个所就没有被任何线程只有. 当线程请求一个未被持有的锁时, JVM 将记下锁的持有者, 并且将获取计数值置为 1, 如果同一个线程再次获取这个锁, 技术值将递增, 退出一次同步代码块, 计算值递减, 当计数值为 0 时, 这个锁就被释放.
ReentrantLock 里面有实现

### 可中断锁

可以相应中断的锁。

在 Java 中，synchronized 就不是可中断锁，而 Lock 是可中断锁。

### 公平锁

公平锁即尽量以请求锁的顺序来获取锁。比如同是有多个线程在等待一个锁，当这个锁被释放时，等待时间最久的线程（最先请求的线程）会获得该所，这种就是公平锁。

非公平锁即无法保证锁的获取是按照请求锁的顺序进行的。这样就可能导致某个或者一些线程永远获取不到锁。

在 Java 中，synchronized 就是非公平锁，它无法保证等待的线程获取锁的顺序。

而对于 ReentrantLock 和 ReentrantReadWriteLock，它默认情况下是非公平锁，但是可以设置为公平锁。

我们可以在创建 ReentrantLock 对象时，通过以下方式来设置锁的公平性：

```java
ReentrantLock lock = new ReentrantLock(true);
```

参数为true表示为公平锁，为fasle为非公平锁。
