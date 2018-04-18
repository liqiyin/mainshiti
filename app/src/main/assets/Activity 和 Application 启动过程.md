### 链接

[【凯子哥带你学 Framework】Activity 启动过程全解析](http://blog.csdn.net/zhaokaiqiang1992/article/details/49428287)  
[Android 应用程序的 Activity 启动过程简要介绍和学习计划](http://blog.csdn.net/luoshengyang/article/details/6685853)  
[[译]Android Application 启动流程分析](https://www.jianshu.com/p/a5532ecc8377)  

---

### 主要对象功能介绍

#### ActivityManagerServices

简称 AMS，服务端对象，负责系统中 **所有 Activity 的生命周期**

#### ActivityThread

App 的真正入口。当开启 App 之后，会调用 main() 开始运行，开启 **消息循环队列** ，这就是传说中的 **UI 线程** 或者叫主线程。与 ActivityManagerServices 配合，一起完成 Activity 的管理工作

#### ApplicationThread

用来 **实现 ActivityManagerService 与 ActivityThread 之间的交互** 。在 ActivityManagerService 需要管理相关 Application 中的 Activity 的生命周期时，通过 ApplicationThread 的代理对象与 ActivityThread 通讯。

#### ApplicationThreadProxy

是 ApplicationThread 在服务器端的代理，**负责和客户端的 ApplicationThread 通讯** 。AMS 就是通过该代理与 ActivityThread 进行通信的。

#### Instrumentation

每一个应用程序只有一个 Instrumentation 对象，每个 Activity 内都有一个对该对象的引用。Instrumentation 可以理解为 **应用进程的管家** ，ActivityThread 要创建或暂停某个 Activity 时，都需要通过 Instrumentation 来进行具体的操作。

#### ActivityStack

**Activity 在 AMS 的栈管理** ，用来记录已经启动的 Activity 的先后关系，状态信息等。通过 ActivityStack 决定是否需要启动新的进程。

#### ActivityRecord

ActivityStack 的管理对象，每个 Activity 在 AMS 对应一个 ActivityRecord，来 **记录 Activity 的状态以及其他的管理信息** 。其实就是服务器端的 Activity 对象的映像。

#### TaskRecord

AMS 抽象出来的一个 “任务” 的概念，是记录 ActivityRecord 的栈，一个 “Task” 包含若干个 ActivityRecord。AMS 用 TaskRecord **确保 Activity 启动和退出的顺序** 。如果你清楚 Activity 的 4 种 launchMode，那么对这个概念应该不陌生。

---

### zygote 进程

在 Android 系统里面，zygote 是一个进程的名字。Android 是基于 Linux System 的，当你的手机开机的时候，Linux 的内核加载完成之后就会启动一个叫 “init“的进程。在 Linux System 里面，所有的进程都是由 init 进程 fork 出来的，我们的 zygote 进程也不例外。

我们都知道，每一个 App 其实都是：

* 一个单独的 dalvik 虚拟机
* 一个单独的进程

所以当系统里面的第一个 zygote 进程运行之后，在这之后再开启 App，就相当于开启一个新的进程。而为了实现资源共用和更快的启动速度，Android 系统开启新进程的方式，是通过 fork 第一个 zygote 进程实现的。所以说，除了第一个 zygote 进程，其他应用所在的进程都是 zygote 的子进程。

---

### SystemServer 进程

SystemServer 也是一个进程，而且是由 zygote 进程 fork 出来的。

系统里面重要的服务都是在这个进程里面开启的，比如 ActivityManagerService、PackageManagerService、WindowManagerService 等等。

### ActivityManagerService

ActivityManagerService，简称 AMS，服务端对象，**负责系统中所有 Activity 的生命周期** 。

在 SystemServer 进程开启的时候，就会初始化 ActivityManagerService。

```java
public final class SystemServer {

    //zygote 的主入口
    public static void main(String[] args) {
        new SystemServer().run();
    }

    public SystemServer() {
        // Check for factory test mode.
        mFactoryTestMode = FactoryTest.getMode();
    }

    private void run() {
        ...

        // 加载本地系统服务库，并进行初始化
        System.loadLibrary("android_servers");
        nativeInit();

        // 创建系统上下文
        createSystemContext();

        // 初始化 SystemServiceManager 对象，
        // 下面的系统服务开启都需要调用 SystemServiceManager.startService(Class<T>) 这个方法通过反射来启动对应的服务
        mSystemServiceManager = new SystemServiceManager(mSystemContext);

        // 开启服务
        try {
            startBootstrapServices();
            startCoreServices();
            startOtherServices();
        } catch (Throwable ex) {
            throw ex;
        }

        ...
    }

    // 初始化系统上下文对象 mSystemContext，并设置默认的主题
    // mSystemContext 实际上是一个 ContextImpl 对象。
    // 调用 ActivityThread.systemMain() 的时候，会调用 ActivityThread.attach(true)，
    // 在 attach() 里面，创建了 Application 对象，
    // 并调用了 Application.onCreate()。
    private void createSystemContext() {
        ActivityThread activityThread = ActivityThread.systemMain();
        mSystemContext = activityThread.getSystemContext();
        mSystemContext.setTheme(android.R.style.Theme_DeviceDefault_Light_DarkActionBar);
    }

    // 在这里开启了几个核心的服务，因为这些服务之间相互依赖，
    // 所以都放在了这个方法里面。
    private void startBootstrapServices() {
        ...

        // 初始化 ActivityManagerService
        mActivityManagerService = mSystemServiceManager.startService(
                ActivityManagerService.Lifecycle.class).getService();
        mActivityManagerService.setSystemServiceManager(mSystemServiceManager);

        // 初始化 PowerManagerService，因为其他服务需要依赖这个 Service，因此需要尽快的初始化
        mPowerManagerService = mSystemServiceManager.startService(PowerManagerService.class);

        // 现在电源管理已经开启，ActivityManagerService 负责电源管理功能
        mActivityManagerService.initPowerManagement();

        // 初始化 DisplayManagerService
        mDisplayManagerService = mSystemServiceManager.startService(DisplayManagerService.class);

        // 初始化 PackageManagerService
        mPackageManagerService = PackageManagerService.main(mSystemContext, mInstaller,
           mFactoryTestMode != FactoryTest.FACTORY_TEST_OFF, mOnlyCore);

        ...
    }
}
```

经过上面这些步骤，我们的 ActivityManagerService 对象已经创建好了，并且完成了成员变量初始化。而且在这之前，调用 createSystemContext() 创建系统上下文的时候，也已经完成了 mSystemContext 和 ActivityThread 的创建。注意，这是系统进程开启时的流程，在这之后，会开启系统的 Launcher 程序，完成系统界面的加载与显示。

在 Android 系统中，任何一个 Activity 的启动都是由 AMS 和应用程序进程（主要是 ActivityThread）相互配合来完成的。AMS 服务统一调度系统中所有进程的 Activity 启动，而每个 Activity 的启动过程则由其所属的进程具体来完成。

### 服务器与客户端

服务器客户端的概念不仅仅存在于 Web 开发中，在 Android 的框架设计中，使用的也是这一种模式。服务器端指的就是所有 App 共用的系统服务，比如我们这里提到的 ActivityManagerService，和前面提到的 PackageManagerService、WindowManagerService 等等，这些基础的系统服务是被所有的 App 公用的，当某个 App 想实现某个操作的时候，要告诉这些系统服务，比如你想打开一个 App，那么我们知道了包名和 MainActivity 类名之后就可以打开。

但是，我们的 App 通过调用 startActivity() 并不能直接打开另外一个 App，这个方法会通过一系列的调用，最后还是告诉 AMS 说：“我要打开这个 App，我知道他的住址和名字，你帮我打开吧！” 所以是 AMS 来通知 zygote 进程来 fork 一个新进程，来开启我们的目标 App 的。这就像是浏览器想要打开一个超链接一样，浏览器把网页地址发送给服务器，然后还是服务器把需要的资源文件发送给客户端的。

### Launcher

Launcher 本质上也是一个应用程序，和我们的 App 一样，也是继承自 Activity。

在桌面上点击快捷图标，或者从程序列表界面点击图标的时候，会调用：

```java
startActivitySafely(v, intent, tag);
```

这个方法会走到 startActivity(v, intent, tag)：

```java
boolean startActivity(View v, Intent intent, Object tag) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try {
        boolean useLaunchAnimation = (v != null) &&
                !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);

        if (useLaunchAnimation) {
            if (user == null || user.equals(android.os.Process.myUserHandle())) {
                startActivity(intent, opts.toBundle());
            } else {
                launcherApps.startMainActivity(intent.getComponent(), user,
                        intent.getSourceBounds(),
                        opts.toBundle());
            }
        } else {
            if (user == null || user.equals(android.os.Process.myUserHandle())) {
                startActivity(intent);
            } else {
                launcherApps.startMainActivity(intent.getComponent(), user,
                        intent.getSourceBounds(), null);
            }
        }
        return true;
    } catch (SecurityException e) {
    ...
    }
    return false;
}
```

可以看到，因为有 FLAG_ACTIVITY_NEW_TASK，这个 Activity 会添加到一个新的 Task 栈中。

### Instrumentation

每个 Activity 都持有 Instrumentation 对象的一个引用，但是整个进程只会存在一个 Instrumentation 对象。当 startActivityForResult() 调用之后，实际上还是调用了 mInstrumentation.execStartActivity()。

```java
public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
    if (mParent == null) {
        Instrumentation.ActivityResult ar =
            mInstrumentation.execStartActivity(
                this, mMainThread.getApplicationThread(), mToken, this,
                intent, requestCode, options);
        if (ar != null) {
            mMainThread.sendActivityResult(
                mToken, mEmbeddedID, requestCode, ar.getResultCode(),
                ar.getResultData());
        }
        ...ignore some code...    
    } else {
        if (options != null) {
             // 当现在的 Activity 有父 Activity 的时候会调用，但是在 startActivityFromChild() 内部实际还是调用的 mInstrumentation.execStartActivity()
            mParent.startActivityFromChild(this, intent, requestCode, options);
        } else {
            mParent.startActivityFromChild(this, intent, requestCode);
        }
    }
     ...ignore some code...    
}
```

下面是 mInstrumentation.execStartActivity() 的实现：

```java
public ActivityResult execStartActivity(
        Context who, IBinder contextThread, IBinder token, Activity target,
        Intent intent, int requestCode, Bundle options) {
    IApplicationThread whoThread = (IApplicationThread) contextThread;
        ...ignore some code...
    try {
        intent.migrateExtraStreamToClipData();
        intent.prepareToLeaveProcess();
        int result = ActivityManagerNative.getDefault()
            .startActivity(whoThread, who.getBasePackageName(), intent,
                    intent.resolveTypeIfNeeded(who.getContentResolver()),
                    token, target != null ? target.mEmbeddedID : null,
                    requestCode, 0, null, options);
        checkStartActivityResult(result, intent);
    } catch (RemoteException e) {
    }
    return null;
}
```

Instrumentation 类就是完成对 Application 和 Activity 初始化和生命周期的工具类。比如 callActivityOnCreate()：

```java
public void callActivityOnCreate(Activity activity, Bundle icicle) {
    prePerformCreate(activity);
    activity.performCreate(icicle);
    postPerformCreate(activity);
}
```

在这里调用了的 Activity 的入口函数 onCreate()：

```java
final void performCreate(Bundle icicle) {
    onCreate(icicle);
    mActivityTransitionState.readState(icicle);
    performCreateCommon();
}
```

App 和 AMS 是通过 Binder 传递信息的，ActivityThread 就是专门负责与 AMS 的外交工作的。

AMS 是董事会，负责指挥和调度的，ActivityThread 是老板，虽然说家里的事自己说了算，但是需要听从 AMS 的指挥，而 Instrumentation 则是老板娘，负责家里的大事小事，但是一般不抛头露面，听一家之主 ActivityThread 的安排。

---

## Activity 启动流程

### Step 1

无论是通过 Launcher 来启动 Activity，还是通过 Activity 内部调用 startActivity 接口来启动新的 Activity，都通过 Binder 进程间通信进入到 ActivityManagerService 进程中，并且调用 ActivityManagerService.startActivity 接口；

### Step 2

ActivityManagerService 调用 ActivityStack.startActivityMayWait 来做准备要启动的 Activity 的相关信息；

### Step 3

ActivityStack 通知 ApplicationThread 要进行 Activity 启动调度了。

这里的 ApplicationThread 代表的是调用 ActivityManagerService.startActivity 接口的进程，对于通过点击应用程序图标的情景来说，这个进程就是 Launcher 了，而对于通过在 Activity 内部调用 startActivity 的情景来说，这个进程就是这个 Activity 所在的进程了；

### Step 4

ApplicationThread 不执行真正的启动操作，它通过调用 ActivityManagerService.activityPaused 接口进入到 ActivityManagerService 进程中，看看是否需要创建新的进程来启动 Activity；

### Step 5

对于通过点击应用程序图标来启动 Activity 的情景来说，ActivityManagerService 在这一步中，会调用 startProcessLocked 来创建一个新的进程，而对于通过在 Activity 内部调用 startActivity 来启动新的 Activity 来说，这一步是不需要执行的，因为新的 Activity 就在原来的 Activity 所在的进程中进行启动；

### Step 6

ActivityManagerService 调用 ApplicationThread.scheduleLaunchActivity 接口，通知相应的进程执行启动 Activity 的操作；

### Step 7

ApplicationThread 把这个启动 Activity 的操作转发给 ActivityThread，ActivityThread 通过 ClassLoader 导入相应的 Activity 类，然后把它启动起来。

---

## Application 启动流程

### App 基础理论

Android Application 与其他移动平台有两个重大不同点:

* 每个 Android App 都在一个独立空间里, 意味着其运行在一个单独的进程中, 拥有自己的 VM, 被系统分配一个唯一的 user ID.
* Android App 由很多不同组件组成, 这些组件还可以启动其他 App 的组件. 因此, Android App 并没有一个类似程序入口的 main() 方法.

Android 进程与 Linux 进程一样. 默认情况下, 每个 apk 运行在自己的 Linux 进程中. 另外, 默认一个进程里面只有一个线程 --- 主线程. 这个主线程中有一个 Looper 实例, 通过调用 Looper.loop() 从 Message 队列里面取出 Message 来做相应的处理.

简单的说, 进程在其需要的时候被启动. 任意时候, 当用户或者其他组件调取你的 apk 中的任意组件时, 如果你的 apk 没有运行, 系统会为其创建一个新的进程并启动. 通常, 这个进程会持续运行直到被系统杀死. 关键是: 进程是在被需要的时候才创建的.

---

## 启动 App 流程

用户点击 Home 上的一个 App 图标, 启动一个应用时:

Click 事件会调用 startActivity(Intent), 会通过 Binder IPC 机制, 最终调用到 ActivityManagerService. 该 Service 会执行如下操作:

* 第一步通过 PackageManager 的 resolveIntent() 收集这个 intent 对象的指向信息. 指向信息被存储在一个 intent 对象中.
* 下面重要的一步是通过 grantUriPermissionLocked() 方法来验证用户是否有足够的权限去调用该 intent 对象指向的 Activity.
* 如果有权限, ActivityManagerService 会检查并在新的 task 中启动目标 activity.
* 现在, 是时候检查这个进程的 ProcessRecord 是否存在了.
* 如果 ProcessRecord 是 null, ActivityManagerService 会创建新的进程来实例化目标 activity.

### 创建进程

ActivityManagerService 调用 startProcessLocked() 方法来创建新的进程, 该方法会通过前面讲到的 socket 通道传递参数给 Zygote 进程. Zygote 孵化自身, 并调用 ZygoteInit.main() 方法来实例化 ActivityThread 对象并最终返回新进程的 pid.

ActivityThread 随后依次调用 Looper.prepareLoop() 和 Looper.loop() 来开启消息循环.

### 绑定 Application

接下来要做的就是将进程和指定的 Application 绑定起来. 这个是通过上节的 ActivityThread 对象中调用 bindApplication() 方法完成的. 该方法发送一个 BIND_APPLICATION 的消息到消息队列中, 最终通过 handleBindApplication() 方法处理该消息. 然后调用 makeApplication() 方法来加载 App 的 classes 到内存中.

### 启动 Activity

经过前两个步骤之后, 系统已经拥有了该 application 的进程. 后面的调用顺序就是普通的从一个已经存在的进程中启动一个新进程的 activity 了.

实际调用方法是 realStartActivity(), 它会调用 application 线程对象中的 sheduleLaunchActivity() 发送一个 LAUNCH_ACTIVITY 消息到消息队列中, 通过 handleLaunchActivity() 来处理该消息.

---

### App 的主线程的消息循环是在哪里创建的？

ActivityThread 初始化的时候，就已经创建消息循环了，所以在主线程里面创建 Handler 不需要指定 Looper，而如果在其他线程使用 Handler，则需要单独使用 Looper.prepare() 和 Looper.loop() 创建消息循环。

### Application 是在什么时候创建的？onCreate() 什么时候调用的？

也是在 ActivityThread.main() 的时候，具体是在 thread.attach(false)。
