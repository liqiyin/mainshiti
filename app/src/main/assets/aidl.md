### 链接

[Android IPC机制（三）在Android Studio中使用AIDL实现跨进程方法调用](http://blog.csdn.net/itachi85/article/details/50451908)  
[Binder学习指南](http://weishu.me/2016/01/12/binder-index-for-newer/index.html)

---

AIDL 可以实现跨进程方法调用。

### 1. 创建 AIDL 文件

在 java 同级目录创建 aidl 文件夹，在文件夹中创建一个包名和应用包名一致的包。

先创建一个 IGameManager.aidl 的文件，这里面定义了两个方法 addGame() 和 getGameList()。

这个文件用到了 Game 这个类，在 IGameManager.aidl 文件中我们要 import 进来。

```java
package com.cashow.messengerdemo;

import com.cashow.messengerdemo.Game;

interface IGameManager {
    List<Game> getGameList();
    void addGame(in Game game);
}
```

Game 类实现了 Parcelable。

```java
public class Game implements Parcelable {
    public String gameName;
    public String gameDescribe;

    public Game(String gameName,String gameDescribe){
        this.gameName=gameName;
        this.gameDescribe=gameDescribe;
    }

    protected Game(Parcel in) {
        gameName=in.readString();
        gameDescribe=in.readString();
    }

    public static final Creator<Game> CREATOR = new Creator<Game>() {
        @Override
        public Game createFromParcel(Parcel in) {
            return new Game(in);
        }

        @Override
        public Game[] newArray(int size) {
            return new Game[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(gameName);
        dest.writeString(gameDescribe);
    }
}
```

在上面的 IGameManager.aidl 文件中我们用到了 Game 这个类，所以我们也要创建 Game.aidl 文件，来申明 Game 实现了 parcelable 接口。

```java
package com.cashow.messengerdemo;

parcelable Game;
```

这个时候我们重新编译程序，工程就会自动生成IGameManager.aidl对应的接口文件 IGameManager.java。

### 2. 创建服务端

服务端在 onCreate 方法中创建了两个游戏的信息并创建 Binder 对象实现了 AIDL 的接口文件中的方法，并在 onBind 方法中将 Binder 对象返回。

```java
public class AIDLService extends Service {
    private CopyOnWriteArrayList<Game> mGameList = new CopyOnWriteArrayList<>();
    private Binder binder = new IGameManager.Stub() {
        @Override
        public List<Game> getGameList() throws RemoteException {
            return mGameList;
        }

        @Override
        public void addGame(Game game) throws RemoteException {
            mGameList.add(game);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mGameList.add(new Game("game_name_1", "game_desc_1"));
        mGameList.add(new Game("game_name_2", "game_desc_2"));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
```

### 3. 客户端调用

最后我们在客户端 onCreate 方法中调用 bindService 方法绑定远程服务端，绑定成功后将返回的 Binder 对象转换为 AIDL 接口，这样我们就可以通过这个接口来调用远程服务端的方法了。

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindAIDLService();
    }

    private void bindAIDLService() {
        Intent intent = new Intent(this, AIDLService.class);
        bindService(intent, mAIDLServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mAIDLServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IGameManager iGameManager = IGameManager.Stub.asInterface(service);
            Game game = new Game("game_name_3", "game_desc_3");
            try {
                iGameManager.addGame(game);
                List<Game> mList = iGameManager.getGameList();
                for (int i = 0; i < mList.size(); i++) {
                    Game mGame = mList.get(i);
                    Toast.makeText(getApplicationContext(), "game : " + mGame.gameName + " " + mGame.gameDescribe, Toast.LENGTH_SHORT).show();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mAIDLServiceConnection);
    }
}
```

---

### AIDL文件中支持的数据类型

* 基本数据类型
* String 和 CharSequence
* List：只支持 ArrayList, 里面的元素都必须被 AIDL 支持
* Map：只支持 HashMap, 里面的元素必须被 AIDL 支持
* 实现 Parcelable 接口的对象
* 所有 AIDL 接口

---

### 深入理解 Java 层的 Binder

`IBinder/IInterface/Binder/BinderProxy/Stub`

* IBinder 是一个接口，它代表了一种跨进程传输的能力；只要实现了这个接口，就能将这个对象进行跨进程传递；这是驱动底层支持的；在跨进程数据流经驱动的时候，驱动会识别 IBinder 类型的数据，从而自动完成不同进程 Binder 本地对象以及 Binder 代理对象的转换。
* IBinder 负责数据传输，那么 client 与 server 端的调用契约（这里不用接口避免混淆）呢？这里的 IInterface 代表的就是远程 server 对象具有什么能力。具体来说，就是 aidl 里面的接口。
* Java 层的 Binder 类，代表的其实就是 Binder 本地对象。BinderProxy 类是 Binder 类的一个内部类，它代表远程进程的 Binder 对象的本地代理；这两个类都继承自 IBinder, 因而都具有跨进程传输的能力；实际上，在跨越进程的时候，Binder 驱动会自动完成这两个对象的转换。
* 在使用 AIDL 的时候，编译工具会给我们生成一个 Stub 的静态内部类；这个类继承了 Binder, 说明它是一个 Binder 本地对象，它实现了 IInterface 接口，表明它具有远程 Server 承诺给 Client 的能力；Stub 是一个抽象类，具体的 IInterface 的相关实现需要我们手动完成，这里使用了策略模式。

创建好 IGameManager.aidl 文件后，用编译工具编译，可以得到对应的 IGameManager.java 类。

系统帮我们生成了这个文件之后，我们只需要继承 IGameManager.Stub 这个抽象类，实现它的方法，然后在 Service 的 onBind 方法里面返回就实现了 AIDL。

Stub 类继承自 Binder，意味着这个 Stub 其实自己是一个 Binder 本地对象，然后实现了 IGameManager 接口，IGameManager 本身是一个 IInterface，因此他携带某种客户端需要的能力（这里是方法 addGame() 和 getGameList())。此类有一个内部类 Proxy，也就是 Binder 代理对象；

然后看看 asInterface 方法，我们在 bind 一个 Service 之后，在 onServiceConnecttion 的回调里面，就是通过这个方法拿到一个远程的 service 的，这个方法做了什么呢？

```java
public static com.cashow.messengerdemo.IGameManager asInterface(android.os.IBinder obj) {
    if ((obj == null)) {
        return null;
    }
    android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
    if (((iin != null) && (iin instanceof com.cashow.messengerdemo.IGameManager))) {
        return ((com.cashow.messengerdemo.IGameManager) iin);
    }
    return new com.cashow.messengerdemo.IGameManager.Stub.Proxy(obj);
}
```

首先看函数的参数 IBinder 类型的 obj，这个对象是驱动给我们的，如果是 Binder 本地对象，那么它就是 Binder 类型，如果是 Binder 代理对象，那就是 BinderProxy 类型；然后，正如上面自动生成的文档所说，它会试着查找 Binder 本地对象，如果找到，说明 Client 和 Server 都在同一个进程，这个参数直接就是本地对象，直接强制类型转换然后返回，如果找不到，说明是远程对象（处于另外一个进程）那么就需要创建一个 Binder 代理对象，让这个 Binder 代理实现对于远程对象的访问。一般来说，如果是与一个远程 Service 对象进行通信，那么这里返回的一定是一个 Binder 代理对象，这个 IBinder 参数的实际上是 BinderProxy;

再看看我们对于 aidl 的 addGame() 方法的实现；在 Stub 类里面，addGame() 是一个抽象方法，我们需要继承这个类并实现它；如果 Client 和 Server 在同一个进程，那么直接就是调用这个方法；那么，如果是远程调用，这中间发生了什么呢？Client 是如何调用到 Server 的方法的？

我们知道，对于远程方法的调用，是通过 Binder 代理完成的，在这个例子里面就是 Proxy 类；Proxy 对于 addGame() 方法的实现如下：

```java
@Override
public void addGame(com.cashow.messengerdemo.Game game) throws android.os.RemoteException {
    android.os.Parcel _data = android.os.Parcel.obtain();
    android.os.Parcel _reply = android.os.Parcel.obtain();
    try {
        _data.writeInterfaceToken(DESCRIPTOR);
        if ((game != null)) {
            _data.writeInt(1);
            game.writeToParcel(_data, 0);
        } else {
            _data.writeInt(0);
        }
        mRemote.transact(Stub.TRANSACTION_addGame, _data, _reply, 0);
        _reply.readException();
    } finally {
        _reply.recycle();
        _data.recycle();
    }
}
```

它首先用 Parcel 把数据序列化了，然后调用了 transact 方法；这个 transact 到底做了什么呢？这个 Proxy 类在 asInterface 方法里面被创建，前面提到过，如果是 Binder 代理那么说明驱动返回的 IBinder 实际是 BinderProxy, 因此我们的 Proxy 类里面的 mRemote 实际类型应该是 BinderProxy；我们看看 BinderProxy 的 transact 方法：(Binder.java 的内部类)

```java
public native boolean transact(int code, Parcel data,
 Parcel reply, int flags) throws RemoteException;
```

这是一个本地方法；它的实现在 native 层，里面进行了一系列的函数调用，它最终调用到了 talkWithDriver 函数；看这个函数的名字就知道，通信过程要交给驱动完成了；这个函数最后通过 ioctl 系统调用，Client 进程陷入内核态，Client 调用 addGame 方法的线程挂起等待返回；驱动完成一系列的操作之后唤醒 Server 进程，调用了 Server 进程本地对象的 onTransact 函数（实际上由 Server 端线程池完成）。我们再看 Binder 本地对象的 onTransact 方法（这里就是 Stub 类里面的此方法）：

```java
@Override
public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
    switch (code) {
        case INTERFACE_TRANSACTION: {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        case TRANSACTION_getGameList: {
            data.enforceInterface(DESCRIPTOR);
            java.util.List<com.cashow.messengerdemo.Game> _result = this.getGameList();
            reply.writeNoException();
            reply.writeTypedList(_result);
            return true;
        }
        case TRANSACTION_addGame: {
            data.enforceInterface(DESCRIPTOR);
            com.cashow.messengerdemo.Game _arg0;
            if ((0 != data.readInt())) {
                _arg0 = com.cashow.messengerdemo.Game.CREATOR.createFromParcel(data);
            } else {
                _arg0 = null;
            }
            this.addGame(_arg0);
            reply.writeNoException();
            return true;
        }
    }
    return super.onTransact(code, data, reply, flags);
}
```

在 Server 进程里面，onTransact 根据调用号（每个 AIDL 函数都有一个编号，在跨进程的时候，不会传递函数，而是传递编号指明调用哪个函数）调用相关函数；在这个例子里面，调用了 Binder 本地对象的 addGame() 方法；这个方法将结果返回给驱动，驱动唤醒挂起的 Client 进程里面的线程并将结果返回。于是一次跨进程调用就完成了。

Proxy 与 Stub 不一样，虽然他们都既是 Binder 又是 IInterface，不同的是 Stub 采用的是继承（is 关系），Proxy 采用的是组合（has 关系）。他们均实现了所有的 IInterface 函数，不同的是，Stub 又使用策略模式调用的是虚函数（待子类实现），而 Proxy 则使用组合模式。为什么 Stub 采用继承而 Proxy 采用组合？事实上，Stub 本身 is 一个 IBinder（Binder），它本身就是一个能跨越进程边界传输的对象，所以它得继承 IBinder 实现 transact 这个函数从而得到跨越进程的能力（这个能力由驱动赋予）。Proxy 类使用组合，是因为他不关心自己是什么，它也不需要跨越进程传输，它只需要拥有这个能力即可，要拥有这个能力，只需要保留一个对 IBinder 的引用。如果把这个过程做一个类比，在封建社会，Stub 好比皇帝，可以号令天下，他生而具有这个权利。如果一个人也想号令天下，可以，“挟天子以令诸侯”。为什么不自己去当皇帝，其一，一般情况没必要，当了皇帝其实限制也蛮多的是不是？我现在既能掌管天下，又能不受约束（Java 单继承）；其二，名不正言不顺啊，我本来特么就不是（Binder），你非要我是说不过去，搞不好还会造反。最后呢，如果想当皇帝也可以，那就是 asBinder 了。在 Stub 类里面，asBinder 返回 this，在 Proxy 里面返回的是持有的组合类 IBinder 的引用。

再去翻阅系统的 ActivityManagerServer 的源码，就知道哪一个类是什么角色了：IActivityManager 是一个 IInterface，它代表远程 Service 具有什么能力，ActivityManagerNative 指的是 Binder 本地对象（类似 AIDL 工具生成的 Stub 类），这个类是抽象类，它的实现是 ActivityManagerService；因此对于 AMS 的最终操作都会进入 ActivityManagerService 这个真正实现；同时如果仔细观察，ActivityManagerNative.java 里面有一个非公开类 ActivityManagerProxy, 它代表的就是 Binder 代理对象；是不是跟 AIDL 模型一模一样呢？那么 ActivityManager 是什么？他不过是一个管理类而已，可以看到真正的操作都是转发给 ActivityManagerNative 进而交给他的实现 ActivityManagerService 完成的。
