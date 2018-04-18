### 链接

[深入理解 Activity 的四种启动模式](http://yifeng.studio/2018/01/09/understand-four-launch-modes-for-android-activity/index.html)

---

### standard

创建一个新的 Activity 。

* Android 5.0 以前：新 Activity 放在 Intent 相同的 Task 的栈顶。
* Android 5.0 及以后，新 Activity 来自相同的应用：表现形式与 5.0 之前的系统一样。
* Android 5.0 及以后，Intent 发送自不同的应用程序：新的 task 将被创建，同时新创建的 Activity 作为一个根 Activity 被放置。

### singleTop

* Activity 不在栈顶：表现形式类似于 standard 。
* Activity 在栈顶：不会创建新的 Activity，取而代之的是通过 onNewIntent() 方法将 Intent 发送至这个已经存在的 Activity 实例中。

### singleTask

拥有 singleTask 启动模式的 Activity 在系统中只允许存在一个实例。

在相同应用程序：
* 没有 singleTask Activity 实例：创建一个新的 Activity 并放置在相同 Task 里面。
* 有 singleTask Activity 实例：这个 singleTask Activity 上面的所有 Activities 都会自动销毁。同时，这个 singleTask Activity 通过 onNewIntent() 方法接受被发送的 Intent 对象。

在不同应用程序中：
* 没有 singleTask Activity 实例：新的 Task 将被创建，并且新创建的 Activity 被作为根 Activity 放置。
* 存在一个 singleTask Activity 拥有者 Task 并且 Activity 还没有被创建：新创建的 Activity 将被置于栈顶。
* singleTask Activity 实例已经在某个 Task 中存在：整个 Task 将会出现在顶部并且位于该 singleTask Activity 上面的所有 Activity 会销毁掉。

如果你希望开启 singleTask Activity 时，创建一个新的 Task 并作为根 Activity 放置，你需要额外指派 taskAffinity 属性给这个 Activity。

### singleInstance

拥有这个 Activity 的 Task 只能包含一个 Activity 实例，也就是这个 singleInstance 模式的 Activity。

如果使用这种模式的 Activity 打开另一个 Activity，系统将自动创建一个新的 Task 来容纳新的 Activity。

同样的，如果 singleInstance Activity 被调用，新的 Task 将被创建来存放这个 Activity。
