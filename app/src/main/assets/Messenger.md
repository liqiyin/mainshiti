### 链接

<http://blog.csdn.net/itachi85/article/details/50448409>

---

Messenger可以在不同进程中传递Message对象，我们在Message中加入我们想要传的数据就可以在进程间的进行数据传递了。Messenger是一种轻量级的IPC方案并对AIDL 进行了封装，它实现起来比较容易。

```java
public class MessengerService extends Service {

    public static final int MSG_FROMCLIENT = 1000;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FROMCLIENT:
                    Messenger messenger = msg.replyTo;

                    Message message = Message.obtain(null, MessengerService.MSG_FROMCLIENT);
                    Bundle bundle = new Bundle();
                    bundle.putString("reply", "hello from MessengerService");
                    message.setData(bundle);

                    try {
                        messenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getApplicationContext(), "收到客户端信息：" + msg.getData().getString("msg"), Toast.LENGTH_SHORT).show();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(mHandler).getBinder();
    }
}

public class MainActivity extends AppCompatActivity {
    private Messenger messenger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindMessengerService();
    }

    private void bindMessengerService() {
        Intent intent = new Intent(this, MessengerService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);

            Message message = Message.obtain(null, MessengerService.MSG_FROMCLIENT);
            Bundle bundle = new Bundle();
            bundle.putString("msg", "hello from mainactivity");
            message.setData(bundle);
            message.replyTo = new Messenger(handler);

            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessengerService.MSG_FROMCLIENT:
                    Toast.makeText(getApplicationContext(), "收到服务端信息：" + msg.getData().getString("reply"), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }
}
```
