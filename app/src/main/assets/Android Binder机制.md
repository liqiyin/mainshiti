### 链接

[Android Binder机制(一) Binder的设计和框架](http://wangkuiwu.github.io/2014/09/01/Binder-Introduce/)  
[Binder学习指南](http://weishu.me/2016/01/12/binder-index-for-newer/index.html)

---

### Binder 模型

![binder_frame](https://raw.githubusercontent.com/wangkuiwu/android_applets/master/os/pic/binder/binder_frame.jpg)

回想一下日常生活中我们通信的过程：假设 A 和 B 要进行通信，通信的媒介是打电话（A 是 Client，B 是 Server）；A 要给 B 打电话，必须知道 B 的号码，这个号码怎么获取呢？通信录。

如果 A 要给 B 打电话，必须先连接通话中心，说明给我接通 B 的电话；这时候通话中心帮他呼叫 B；连接建立，就完成了通信。

另外，光有电话和通信录是不可能完成通信的，没有基站支持；信息根本无法传达。

我们看到，一次电话通信的过程除了通信的双方还有两个隐藏角色：通信录和基站。Binder 通信机制也是一样：两个运行在用户空间的进程要完成通信，必须借助内核的帮助，这个运行在内核里面的程序叫做 Binder 驱动，它的功能类似于基站；通信录呢，就是一个叫做 ServiceManager 的东西。

### Binder 驱动存在的原因和意义

Binder 机制的目的是实现 IPC(Inter-Process Communication)，即实现进程间通信。

Android 是基于 Linux 内核而打造的操作系统。
以 32 位 Linux 系统而言，它的内存最大是 4G。在这 4G 内存中，0~3G 为用户空间，3~4G 为内核空间。

应用程序都运行在用户空间，而 Kernel 和驱动都运行在内核空间。

每个应用程序都有它自己独立的内存空间。若不同的应用程序之间涉及到通信，需要通过内核进行中转，因为需要用到内核的 copy_from_user() 和 copy_to_user() 等函数。

### ServiceManager 存在的原因和意义

Binder 是要实现 Android 的 C-S 架构的，即 Client-Server 架构。而 ServiceManager，是以服务管理者的身份存在的。

ServiceManager 也是运行在用户空间的一个独立进程。

(01) 对于 Binder 驱动而言，ServiceManager 是一个守护进程，更是 Android 系统各个服务的管理者。Android 系统中的各个服务，都是添加到 ServiceManager 中进行管理的，而且每个服务都对应一个服务名。当 Client 获取某个服务时，则通过服务名来从 ServiceManager 中获取相应的服务。

(02) 对于 MediaPlayerService 和 MediaPlayer 而言，ServiceManager 是一个 Server 服务端，是一个服务器。当要将 MediaPlayerService 等服务添加到 ServiceManager 中进行管理时，ServiceManager 是服务器，它会收到 MediaPlayerService 进程的添加服务请求。当 MediaPlayer 等客户端要获取 MediaPlayerService 等服务时，它会向 ServiceManager 发起获取服务请求。

当 MediaPlayer 和 MediaPlayerService 通信时，MediaPlayerService 是服务端；而当 MediaPlayerService 和 ServiceManager 通信时，ServiceManager 则是服务端。这样，就造就了 ServiceManager 的特殊性。于是，在 Binder 驱动中，将句柄 0 指定为 ServiceManager 对应的句柄，通过这个特殊的句柄就能获取 ServiceManager 对象。这部分的知识后面会详细介绍。

### 为什么采用 Binder 机制，而不是其他的 IPC 通信方式

#### 第一. Binder 能够很好的实现 Client-Server 架构

目前 Linux 支持的 "传统的管道/消息队列/共享内存/信号量/Socket 等"IPC 通信手段中，只有 Socket 是 Client-Server 的通信方式。但是，Socket 主要用于网络间通信以及本机中进程间的低速通信，它的传输效率太低。

#### 第二. Binder的传输效率和可操作性很好

消息队列和管道又采用存储 - 转发方式，使用它们进行 IPC 通信时，需要经过 2 次内存拷贝！效率太低！

首先，数据先从发送方的缓存区 (即，Linux 中的用户存储空间) 拷贝到内核开辟的缓存区 (即，Linux 中的内核存储空间) 中，是第 1 次拷贝。接着，再从内核缓存区拷贝到接收方的缓存区(也是 Linux 中的用户存储空间)，这是第 2 次拷贝。

而采用 Binder 机制的话，则只需要经过 1 次内存拷贝即可！ 即，从发送方的缓存区拷贝到内核的缓存区，而接收方的缓存区与内核的缓存区是映射到同一块物理地址的，因此只需要 1 次拷贝即可。

#### 第三. Binder机制的安全性很高

传统 IPC 没有任何安全措施，完全依赖上层协议来确保。传统 IPC 的接收方无法获得对方进程可靠的 UID/PID(用户 ID / 进程 ID)，从而无法鉴别对方身份。而 Binder 机制则为每个进程分配了 UID/PID 来作为鉴别身份的标示，并且在 Binder 通信时会根据 UID/PID 进行有效性检测。

---

### Binder 相关的概念

#### Binder 实体

Binder 实体，是各个 Server 以及 ServiceManager 在内核中的存在形式。

Binder 实体的作用是在内核中保存 Server 和 ServiceManager 的信息 (例如，Binder 实体中保存了 Server 对象在用户空间的地址)。简言之，Binder 实体是 Server 在 Binder 驱动中的存在形式，内核通过 Binder 实体可以找到用户空间的 Server 对象。

#### Binder 引用

Binder 引用的作用是在表示 "Binder 实体" 的引用。换句话说，每一个 Binder 引用都是某一个 Binder 实体的引用，通过 Binder 引用可以在内核中找到它对应的 Binder 实体。

如果将 Server 看作是 Binder 实体的话，那么 Client 就好比 Binder 引用。Client 要和 Server 通信，它就是通过保存一个 Server 对象的 Binder 引用，再通过该 Binder 引用在内核中找到对应的 Binder 实体，进而找到 Server 对象，然后将通信内容发送给 Server 对象。

Binder 实体和 Binder 引用都是内核 (即，Binder 驱动) 中的数据结构。每一个 Server 在内核中就表现为一个 Binder 实体，而每一个 Client 则表现为一个 Binder 引用。这样，每个 Binder 引用都对应一个 Binder 实体，而每个 Binder 实体则可以多个 Binder 引用。

#### 远程服务

Server 都是以服务的形式注册到 ServiceManager 中进行管理的。如果将 Server 本身看作是 "本地服务" 的话，那么 Client 中的 "远程服务" 就是本地服务的代理。如果你对代理模式比较熟悉的话，就很容易理解了，远程服务就是本地服务的一个代理，通过该远程服务 Client 就能和 Server 进行通信。

#### ServiceManager 守护进程

ServiceManager 是用户空间的一个守护进程。当该应用程序启动时，它会和 Binder 驱动进行通信，告诉 Binder 驱动它是服务管理者；对 Binder 驱动而言，它则会新建 ServiceManager 对应的 Binder 实体，并将该 Binder 实体设为全局变量。为什么要将它设为全局变量呢？这点应该很容易理解 -- 因为 Client 和 Server 都需要和 ServiceManager 进行通信，不将它设为全局变量的话，怎么找到 ServiceManager 呢！

#### Server 注册到 ServiceManager 中

Server 首先会向 Binder 驱动发起注册请求，而 Binder 驱动在收到该请求之后就将该请求转发给 ServiceManager 进程。但是 Binder 驱动怎么才能知道该请求是要转发给 ServiceManager 的呢？这是因为 Server 在发送请求的时候，会告诉 Binder 驱动这个请求是交给 0 号 Binder 引用对应的进程来进行处理的。而 Binder 驱动中指定了 0 号引用是与 ServiceManager 对应的。

在 Binder 驱动转发该请求之前，它其实还做了两件很重要的事：

(01) 当它知道该请求是由一个 Server 发送的时候，它会新建该 Server 对应的 Binder 实体。  
(02) 它在 ServiceManager 的 "保存 Binder 引用的红黑树" 中查找是否存在该 Server 的 Binder 引用；找不到的话，就新建该 Server 对应的 Binder 引用，并将其添加到 "ServiceManager 的保存 Binder 引用的红黑树" 中。

简言之，Binder 驱动会创建 Server 对应的 Binder 实体，并在 ServiceManager 的红黑树中添加该 Binder 实体的 Binder 引用。

当 ServiceManager 收到 Binder 驱动转发的注册请求之后，它就将该 Server 的相关信息注册到 "Binder 引用组成的单链表" 中。这里所说的 Server 相关信息主要包括两部分：Server 对应的服务名 + Server 对应的 Binder 实体的一个 Binder 引用。

#### Client 获取远程服务

Client 要和某个 Server 通信，需要先获取到该 Server 的远程服务。那么 Client 是如何获取到 Server 的远程服务的呢？

Client 首先会向 Binder 驱动发起获取服务的请求。Binder 驱动在收到该请求之后也是该请求转发给 ServiceManager 进程。ServiceManager 在收到 Binder 驱动转发的请求之后，会从 "Binder 引用组成的单链表" 中找到要获取的 Server 的相关信息。至于 ServiceManager 是如何从单链表中找到需要的 Server 的呢？答案是 Client 发送的请求数据中，会包括它要获取的 Server 的服务名；而 ServiceManager 正是根据这个服务名来找到 Server 的。

接下来，ServiceManager 通过 Binder 驱动将 Server 信息反馈给 Client 的。它反馈的信息是 Server 对应的 Binder 实体的 Binder 引用信息。而 Client 在收到该 Server 的 Binder 引用信息之后，就根据该 Binder 引用信息创建一个 Server 对应的远程服务。这个远程服务就是 Server 的代理，Client 通过调用该远程服务的接口，就相当于在调用 Server 的服务接口一样；因为 Client 调用该 Server 的远程服务接口时，该远程服务会对应的通过 Binder 驱动和真正的 Server 进行交互，从而执行相应的动作。

---

### Binder通信模型

![binder_communication](https://raw.githubusercontent.com/wangkuiwu/android_applets/master/os/pic/binder/binder_communication.jpg)

首先，Server 进程要向 ServiceManager 注册；告诉自己是谁，自己有什么能力；在这个场景就是 Server 告诉 ServiceManager，它叫 zhangsan，它有一个 object 对象，可以执行 add 操作；于是 ServiceManager 建立了一张表：zhangsan 这个名字对应进程 Server;

然后 Client 向 ServiceManager 查询：我需要联系一个名字叫做 zhangsan 的进程里面的 object 对象；这时候关键来了：进程之间通信的数据都会经过运行在内核空间里面的驱动，驱动在数据流过的时候做了一点手脚，它并不会给 Client 进程返回一个真正的 object 对象，而是返回一个看起来跟 object 一模一样的代理对象 objectProxy，这个 objectProxy 也有一个 add 方法，但是这个 add 方法没有 Server 进程里面 object 对象的 add 方法那个能力；objectProxy 的 add 只是一个傀儡，它唯一做的事情就是把参数包装然后交给驱动。

但是 Client 进程并不知道驱动返回给它的对象动过手脚，毕竟伪装的太像了，如假包换。Client 开开心心地拿着 objectProxy 对象然后调用 add 方法；我们说过，这个 add 什么也不做，直接把参数做一些包装然后直接转发给 Binder 驱动。

驱动收到这个消息，发现是这个 objectProxy；一查表就明白了：我之前用 objectProxy 替换了 object 发送给 Client 了，它真正应该要访问的是 object 对象的 add 方法；于是 Binder 驱动通知 Server 进程，调用你的 object 对象的 add 方法，然后把结果发给我，Sever 进程收到这个消息，照做之后将结果返回驱动，驱动然后把结果返回给 Client 进程；于是整个过程就完成了。

理解这一点非常重要；务必仔细体会。另外，Android 系统实现这种机制使用的是代理模式, 对于 Binder 的访问，如果是在同一个进程（不需要跨进程），那么直接返回原始的 Binder 实体；如果在不同进程，那么就给他一个代理对象（影子）；我们在系统源码以及 AIDL 的生成代码里面可以看到很多这种实现。

一句话总结就是：Client 进程只不过是持有了 Server 端的代理；代理对象协助驱动完成了跨进程通信。

1. Server 进程启动之后，会进入中断等待状态，等待 Client 的请求。
1. 当 Client 需要和 Server 通信时，会将请求发送给 Binder 驱动。
1. Binder 驱动收到请求之后，会唤醒 Server 进程。
1. 接着，Binder 驱动还会反馈信息给 Client，告诉 Client：它发送给 Binder 驱动的请求，Binder 驱动已经收到。
1. Client 将请求发送成功之后，就进入等待状态。等待 Server 的回复。
1. Binder 驱动唤醒 Server 之后，就将请求转发给 Server 进程。
1. Server 进程解析出请求内容，并将回复内容发送给 Binder 驱动。
1. Binder 驱动收到回复之后，唤醒 Client 进程。
1. 接着，Binder 驱动还会反馈信息给 Server，告诉 Server：它发送给 Binder 驱动的回复，Binder 驱动已经收到。
1. Server 将回复发送成功之后，再次进入等待状态，等待 Client 的请求。
1. 最后，Binder 驱动将回复转发给 Client。
