### 链接：

<http://www.cnblogs.com/dolphin0520/p/3923737.html>

---

### 线程安全问题

多个线程同时访问一个资源时，会导致程序运行结果并不是想看到的结果。这个资源被称为：临界资源（也有称为共享资源）。
当多个线程同时访问临界资源（一个对象，对象中的属性，一个文件，一个数据库等）时，就可能会产生线程安全问题。
不过，当多个线程执行一个方法，方法内部的局部变量并不是临界资源，因为方法是在栈上执行的，而Java栈是线程私有的，因此不会产生线程安全问题。

基本上所有的并发模式在解决线程安全问题时，都采用“序列化访问临界资源”的方案，即在同一时刻，只能有一个线程访问临界资源，也称作同步互斥访问。
通常来说，是在访问临界资源的代码前面加上一个锁，当访问完临界资源后释放锁，让其他线程继续访问。
在Java中，提供了两种方式来实现同步互斥访问：synchronized和Lock。

### 互斥锁

如果对临界资源加上互斥锁，当一个线程在访问该临界资源时，其他线程便只能等待。

在Java中，每一个对象都拥有一个锁标记（monitor），也称为监视器，多线程同时访问某个对象时，线程只有获取了该对象的锁才能访问。

在Java中，可以使用synchronized关键字来标记一个方法或者代码块，当某个线程调用该对象的synchronized方法或者访问synchronized代码块时，这个线程便获得了该对象的锁，其他线程暂时无法访问这个方法，只有等待这个方法执行完毕或者代码块执行完毕，这个线程才会释放该对象的锁，其他线程才能执行这个方法或者代码块。

### synchronized方法

```java
class InsertData {
    private ArrayList<Integer> arrayList = new ArrayList<Integer>();

    public synchronized void insert(Thread thread){
        for(int i=0;i<5;i++){
            System.out.println(thread.getName()+"在插入数据"+i);
            arrayList.add(i);
        }
    }
}
```

1）当一个线程正在访问一个对象的synchronized方法，那么其他线程不能访问该对象的其他synchronized方法。这个原因很简单，因为一个对象只有一把锁，当一个线程获取了该对象的锁之后，其他线程无法获取该对象的锁，所以无法访问该对象的其他synchronized方法。

2）当一个线程正在访问一个对象的synchronized方法，那么其他线程能访问该对象的非synchronized方法。这个原因很简单，访问非synchronized方法不需要获得该对象的锁，假如一个方法没用synchronized关键字修饰，说明它不会使用到临界资源，那么其他线程是可以访问这个方法的，

3）如果一个线程A需要访问对象object1的synchronized方法fun1，另外一个线程B需要访问对象object2的synchronized方法fun1，即使object1和object2是同一类型），也不会产生线程安全问题，因为他们访问的是不同的对象，所以不存在互斥问题。

### synchronized代码块

当在某个线程中执行这段代码块，该线程会获取对象synObject的锁，从而使得其他线程无法同时访问该代码块。

synObject可以是this，代表获取当前对象的锁，也可以是类中的一个属性，代表获取该属性的锁。

比如上面的insert方法可以改成以下两种形式：

```java
class InsertData {
    private ArrayList<Integer> arrayList = new ArrayList<Integer>();

    public void insert(Thread thread){
        synchronized (this) {
            for(int i=0;i<100;i++){
                System.out.println(thread.getName()+"在插入数据"+i);
                arrayList.add(i);
            }
        }
    }
}
```

```java
class InsertData {
    private ArrayList<Integer> arrayList = new ArrayList<Integer>();
    private Object object = new Object();

    public void insert(Thread thread){
        synchronized (object) {
            for(int i=0;i<100;i++){
                System.out.println(thread.getName()+"在插入数据"+i);
                arrayList.add(i);
            }
        }
    }
}
```

synchronized代码块可以实现只对需要同步的地方进行同步。

每个类也会有一个锁，它可以用来控制对static数据成员的并发访问。

并且如果一个线程执行一个对象的非static synchronized方法，另外一个线程需要执行这个对象所属类的static synchronized方法，此时不会发生互斥现象，因为访问static synchronized方法占用的是类锁，而访问非static synchronized方法占用的是对象锁，所以不存在互斥现象。

无论synchronized关键字加在方法上还是对象上，它取得的锁都是对象

### synchronized 静态方法

```java
Class Foo {
  // 同步的static 函数
  public synchronized static void methodAAA()  {
  //….
  }
  public void methodBBB() {
       synchronized(Foo.class)   // class literal(类名称字面常量)
  }    
}
```

代码中的methodBBB()方法是把class literal作为锁的情况，它和同步的static函数产生的效果是一样的，取得的锁很特别，是当前调用这个方法的对象所属的类（Class，而不再是由这个Class产生的某个具体对象了）。

可以推断：如果一个类中定义了一个synchronized 的 static 函数A，也定义了一个 synchronized 的 instance函数B，那么这个类的同一对象Obj在多线程中分别访问A和B两个方法时，不会构成同步，因为它们的锁都不一样。B方法的锁是Obj这个对象，而B的锁是Obj所属的那个Class。

### synchronized 原理

synchronized代码块实际上多了monitorenter和monitorexit两条指令。monitorenter指令执行时会让对象的锁计数加1，而monitorexit指令执行时会让对象的锁计数减1，其实这个与操作系统里面的PV操作很像，操作系统里面的PV操作就是用来控制多个线程对临界资源的访问。对于synchronized方法，执行中的线程识别该方法的 method_info 结构是否有 ACC_SYNCHRONIZED 标记设置，然后它自动获取对象的锁，调用方法，最后释放锁。如果有异常发生，线程自动释放锁。

有一点要注意：对于synchronized方法或者synchronized代码块，当出现异常时，JVM会自动释放当前线程占用的锁，因此不会由于异常导致出现死锁现象。
