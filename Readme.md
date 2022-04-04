##1. HVAC 功能介绍
HVAC 全称：供暖通风与空气调节(Heating Ventilation and Air Conditioning)。用户可以通过他来控制整个汽车的空调系统，是汽车中非常重要的一个功能。
![](https://upload-images.jianshu.io/upload_images/3146091-f93c1608e5e610ac.gif?imageMogr2/auto-orient/strip)
汽车的空调HMI虽然并不复杂，但是大多都是用符号来表示功能，对于还没有实际用过汽车空调系统的开发者来说，理解空调的各个符号表示的含义也是非常有必要。
下面就以Android 12中的HVAC来介绍空调系统中包含的最基础的功能。

### 1.1 双区温度调节
![](https://upload-images.jianshu.io/upload_images/3146091-93838ee9ffbdfac6.gif?imageMogr2/auto-orient/strip)

空调的温度调节功能，默认是华氏度，可以在系统设置修改温度单位。可调节范围是61 - 82华氏度，对应16 - 28 摄氏度。
左侧按钮用来调节主驾，右侧按钮用来调节副驾。在以往都是只有高配车型才有双区空调，现在的车上双区空调几乎已经是标配了。

### 1.2 空调开关
![](https://upload-images.jianshu.io/upload_images/3146091-117f622ecb072772.gif?imageMogr2/auto-orient/strip)
开启关闭空调的开关

### 1.3 内/外循环
![](https://upload-images.jianshu.io/upload_images/3146091-a9c3afa5b61ccafc.gif?imageMogr2/auto-orient/strip)
内循环是汽车空气调节系统的一种状态。这种状态下，车内外的换气通道关闭，风机关闭时车内气流不循环，风机开启时，吸入的气流也仅来自车内，形成车辆内部的气流循环。
外循环则相反，风机开启时，吸入的气流也仅来自车外，可以更新车内的空气质量，代价是会更耗电。

### 1.4 风量调节
![](https://upload-images.jianshu.io/upload_images/3146091-0796b6832db55d93.gif?imageMogr2/auto-orient/strip)
用于增大或减小空调的风量。

### 1.5 风向调节
![](https://upload-images.jianshu.io/upload_images/3146091-d6005a2cbfca4112.gif?imageMogr2/auto-orient/strip)
从左到右分别是吹脸、吹脸+吹脚、吹脚、吹脚+吹挡风玻璃

### 1.6 A/C开关
![](https://upload-images.jianshu.io/upload_images/3146091-efb99b93957d1774.gif?imageMogr2/auto-orient/strip)
A/C按键，它就是制冷开关，按下A/C按键，也就启动了压缩机，通俗地说就是开冷气。

### 1.7 主副驾座椅加热
![](https://upload-images.jianshu.io/upload_images/3146091-737c2b9f64fd0770.gif?imageMogr2/auto-orient/strip)
左边的按钮用于调节主驾座椅加热，右边的按钮用于调节副驾座椅加热

### 1.8 除霜
![](https://upload-images.jianshu.io/upload_images/3146091-9b712b25d9a77dc1.gif?imageMogr2/auto-orient/strip)
左边的按钮是开启/关闭 前挡风玻璃加热，开启后用来除去前挡风玻璃上的雾气。右边的按钮是开启/关闭后挡风玻璃加热，开启后用来除去后挡风玻璃上的雾气。

### 1.9 自动模式
![](https://upload-images.jianshu.io/upload_images/3146091-9db713bf1611d2c5.gif?imageMogr2/auto-orient/strip)
自动空调其实就是省略了风速、风向等调节功能，自动空调是全自动调节，只需要选择风向和设定温度。AUTO按键按下后，就会根据车内传感器来控制出风的温度，冬天热风，夏天冷风。会保持车内有较适宜的温度，如果温度过高或过低，空调也会自动改变出风口的温度及风速，调整车内温度。
以上就是车载空调系统中最基础的功能了，实际开发中我们还会遇到如座椅通风、座椅按摩、智能新风、负离子等等一些近几年才出现的空调新功能，在应用开发上无非就是多几个界面或按钮。

##2. HVAC 源码结构
本文中的源码基于Android 12下HVAC APP，源码请见：https://github.com/linux-link/CarHvac

原生的Hvac App中不存在Activity、Fragment等传统意义上用来显示HMI的组件，取而代之是使用Service来显示一个Window。主要原因在于Hvac的界面层级比一般的HMI的层级要高，呼出Hvac时需要部分或全部覆盖其他的应用上（当然IVI中还是有应用比Hvac的层级要高的），这时候使用Activity就显不合适了。
![](https://upload-images.jianshu.io/upload_images/3146091-d3daf6b22afca42e.gif?imageMogr2/auto-orient/strip)
需要注意的是，Havc在Android 12中虽然有一个独立的app，但是上图中Hvac其实并不是使用这个独立的app，它的HMI和相关其实都是写在SystemUI中的。
通过使用adb发送一个广播，可以调出真正的Hvac。
```
adb shell am broadcast -a android.car.intent.action.TOGGLE_HVAC_CONTROLS
```
![](https://upload-images.jianshu.io/upload_images/3146091-3baa281e7a681655.gif?imageMogr2/auto-orient/strip)
以下是Hvac App的关键部分的源码结构图
![](https://upload-images.jianshu.io/upload_images/3146091-2930770fff432ca0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##3. HVAC 核心源码分析
### 3.1 AndroidManifest.xml
```
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.android.car.hvac">

    <uses-sdk
        android:minSdkVersion="22"
        android:targetSdkVersion="29" />

    <uses-permission android:name="android.car.permission.CONTROL_CAR_CLIMATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <!-- Required to use the TYPE_DISPLAY_OVERLAY layout param for the overlay hvac ui-->
    <uses-permission android:name="android.permission.INTERNAL_SYSTEM_WINDOW" />
    <!-- Allow Hvac to go across all users-->
    <uses-permission android:name="android.permission.INTERACT_ACROSS_USERS" />
    <uses-permission android:name="android.permission.INTERACT_ACROSS_USERS_FULL" />

    <protected-broadcast android:name="android.car.intent.action.TOGGLE_HVAC_CONTROLS" />

    <application
        android:icon="@drawable/ic_launcher_hvac"
        android:label="@string/hvac_label"
        android:persistent="true">

        <!--用于控制空调功能的Service-->
        <service
            android:name=".HvacController"
            android:exported="false"
            android:singleUser="true" />
        <!-- 用于显示UI的Service-->
        <service
            android:name=".HvacUiService"
            android:exported="false"
            android:singleUser="true" />

        <!-- 监听开机广播 -->
        <receiver
            android:name=".BootCompleteReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

### 3.2 BootCompleteReceiver
用于监听开机的广播，当前收到系统的开机广播后，会将HvacUiService拉起。
```
public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent hvacUiService = new Intent(context, HvacUiService.class);
        context.startService(hvacUiService);
    }
}
```

### 3.3 HvacUiService
HvacUiService 用来托管Hvac UI的Service。从名字上也能看出，整个HvacUiService都是围绕着如何将Hvac准确的绘制出来，基本不含其他的逻辑。

```
@Override
public void onCreate() {
    ...
    // 由于不存在从服务内部获取系统ui可见性的方法，因此我们将全屏放置一些东西，并检查其最终测量结果，作为获取该信息的黑客手段。
    // 一旦我们有了初始状态，我们就可以安全地从那时开始注册更改事件。
    View windowSizeTest = new View(this) {
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            Log.i(TAG, "onLayout: changed" + changed + ";left:" + left + ";top:" + top + ";right:" + right + ";bottom" + bottom);
            boolean sysUIShowing = (mDisplayMetrics.heightPixels != bottom);
            mInitialYOffset = (sysUIShowing) ? -mNavBarHeight : 0;
            Log.i(TAG, "onLayout: sysUIShowing:" + sysUIShowing + ";mInitialYOffset" + mInitialYOffset);
            layoutHvacUi();
            // 我们现在有了初始状态，因此不再需要这个空视图。
            mWindowManager.removeView(this);
            mAddedViews.remove(this);
        }
    };
    addViewToWindowManagerAndTrack(windowSizeTest, testparams);

    // 接收事件的广播
    IntentFilter filter = new IntentFilter();
    filter.addAction(CAR_INTENT_ACTION_TOGGLE_HVAC_CONTROLS);
    filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
    // 注册接收器，以便任何具有CONTROL_CAR_CLIMATE权限的用户都可以调用它。
    registerReceiverAsUser(mBroadcastReceiver, UserHandle.ALL, filter,
            Car.PERMISSION_CONTROL_CAR_CLIMATE, null);
}

private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "onReceive: " + action);
        // 自定义广播，用于展开Hvac的HMI
        if (action.equals(CAR_INTENT_ACTION_TOGGLE_HVAC_CONTROLS)) {
            mHvacPanelController.toggleHvacUi();
        } else if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
        // home 按键的广播，收起Hvac的HMI
            mHvacPanelController.collapseHvacUi();
        }
    }
};

// 添加View到WindowManager中
private void addViewToWindowManagerAndTrack(View view, WindowManager.LayoutParams params) {
    mWindowManager.addView(view, params);
    mAddedViews.add(view);
}
```
HvacUIService在onCreate()中主要完成两件事：
1.注册事件广播。这个事件实际并没有发送源，因为SystemUI中额外写了一个Hvac，不过正是这个广播让我们可以把这个单独的Hvac调出。
2.绘制UI。HvacUIService在被拉起后并没有立即开始UI的绘制，而是在屏幕上临时放置一个用于测量窗口的 windowSizeTest ，当windowSizeTestView开始测量后，通过比对View的高度和屏幕的高度，即可判断出systemUI是否已经显示，这时就可以开始着手绘制真正的Hvac的UI了，并且可以更安全的操作UI。
接下来就是绘制真正的Hvac界面：
```
/**
 * 在确定最小偏移量后调用。
 * 这将生成HVAC UI所需的所有组件的布局。
 * 启动时，折叠视图所需的所有窗口都可见，而展开视图的窗口已创建并调整大小，但不可见。
 */
private void layoutHvacUi() {
    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    & ~WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);

    params.packageName = this.getPackageName();
    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
    params.x = 0;
    params.y = mInitialYOffset;
    params.width = mScreenWidth;
    params.height = mScreenBottom;
    params.setTitle("HVAC Container");
    disableAnimations(params);
    // required of the sysui visiblity listener is not triggered.
    params.hasSystemUiListeners = true;

    mContainer = inflater.inflate(R.layout.hvac_panel, null);
    mContainer.setLayoutParams(params);
    mContainer.setOnSystemUiVisibilityChangeListener(visibility -> {
        Log.i(TAG, "layoutHvacUi: visibility:" + visibility);
        boolean systemUiVisible = (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0;
        int y = 0;
        if (systemUiVisible) {
            // 当systemUi可见时，窗口系统坐标从系统导航栏上方的0开始。因此，如果我们想获得屏幕底部的实际高度，我们需要将y值设置为导航栏高度的负值。
            y = -mNavBarHeight;
        }
        setYPosition(mDriverTemperatureBar, y);
        setYPosition(mPassengerTemperatureBar, y);
        setYPosition(mDriverTemperatureBarCollapsed, y);
        setYPosition(mPassengerTemperatureBarCollapsed, y);
        setYPosition(mContainer, y);
    });

    // 顶部填充应根据屏幕高度和扩展hvac面板的高度进行计算。由填充物定义的空间意味着可以单击以关闭hvac面板。
    int topPadding = mScreenBottom - mPanelFullExpandedHeight;
    mContainer.setPadding(0, topPadding, 0, 0);

    mContainer.setFocusable(false);
    mContainer.setFocusableInTouchMode(false);

    View panel = mContainer.findViewById(R.id.hvac_center_panel);
    panel.getLayoutParams().height = mPanelCollapsedHeight;

    addViewToWindowManagerAndTrack(mContainer, params);
    // 创建温度计bar
    createTemperatureBars(inflater);

    // UI状态控制器，用来控制展开/收起时UI的各种状态并执行动画
    mHvacPanelController = new HvacPanelController(this /* context */, mContainer,
            mWindowManager, mDriverTemperatureBar, mPassengerTemperatureBar,
            mDriverTemperatureBarCollapsed, mPassengerTemperatureBarCollapsed
    );
    // 绑定 HvacController Service
    Intent bindIntent = new Intent(this /* context */, HvacController.class);
    if (!bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
        Log.e(TAG, "Failed to connect to HvacController.");
    }
}
```
HvacPanelController是空调的面板控制器，在与HvacController绑定成功后，将HvacController的实例传递给HvacPanelController。
```
private ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        mHvacController = ((HvacController.LocalBinder) service).getService();
        final Context context = HvacUiService.this;

        final Runnable r = () -> {
            // hvac控制器从车辆刷新其值后，绑定所有值。
            mHvacPanelController.updateHvacController(mHvacController);
        };

        if (mHvacController != null) {
            mHvacController.requestRefresh(r, new Handler(context.getMainLooper()));
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        mHvacController = null;
        mHvacPanelController.updateHvacController(null);
        //TODO:b/29126575重新启动后重新连接控制器
    }
};
```
我们接着看HvacPanelController

### 3.4 HvacPanelController
HvacPanelController 主要作用是初始化其他界面Controller，并从HvacController中获取数据，显示在UI上。
```
private FanSpeedBarController mFanSpeedBarController;
private FanDirectionButtonsController mFanDirectionButtonsController;
private TemperatureController mTemperatureController;
private TemperatureController mTemperatureControllerCollapsed;
private SeatWarmerController mSeatWarmerController;

public void updateHvacController(HvacController controller) {
    mHvacController = controller;

    mFanSpeedBarController = new FanSpeedBarController(mFanSpeedBar, mHvacController);
    mFanDirectionButtonsController
            = new FanDirectionButtonsController(mFanDirectionButtons, mHvacController);
    mTemperatureController = new TemperatureController(
            mPassengerTemperatureBarExpanded,
            mDriverTemperatureBarExpanded,
            mPassengerTemperatureBarCollapsed,
            mDriverTemperatureBarCollapsed,
            mHvacController);
    mSeatWarmerController = new SeatWarmerController(mPassengerSeatWarmer,
            mDriverSeatWarmer, mHvacController);

    // 切换按钮不需要额外的逻辑来映射硬件和UI设置。只需使用ToggleListener来处理点击。
    mAcButton.setIsOn(mHvacController.getAcState());
    mAcButton.setToggleListener(new ToggleButton.ToggleListener() {
        @Override
        public void onToggled(boolean isOn) {
            mHvacController.setAcState(isOn);
        }
    });
    ...

    setAutoMode(mHvacController.getAutoModeState());

    mHvacPowerSwitch.setIsOn(mHvacController.getHvacPowerState());
    mHvacPowerSwitch.setToggleListener(isOn -> mHvacController.setHvacPowerState(isOn));

    mHvacController.registerCallback(mToggleButtonCallbacks);
    mToggleButtonCallbacks.onHvacPowerChange(mHvacController.getHvacPowerState());
}
```
Hvac界面展开和收起的动画也是在HvacPanelController 中处理的，不过关于动画部分打算以后再开个新坑讲一讲。

### 3.5 HvacController
HvacController是HvacApp与CarService之间的信息传输控制器，本质上也是一个Service。
```
public class HvacController extends Service {

    private final Binder mBinder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        HvacController getService() {
            return HvacController.this;
        }
    }
    ...
}
```
在Hvac中的设置及获取数据的操作都是通过HvacController进行的，在HvacController启动时会获取一个Car实例，并通过connect方法连接CarService。当连接CarService成功后初始化CarHvacManager并通过CarHvacManager获取车辆支持的属性列表，以及获取界面所需的基础数据。
```
@Override
public void onCreate() {
    super.onCreate();
    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
        // 连接 CarService
        mCarApiClient = Car.createCar(this, mCarServiceConnection);
        mCarApiClient.connect();
    }
}

private final ServiceConnection mCarServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (mHvacManagerReady) {
            try {
                // 连接上CarService后，获取到其中的HvacManager.
                initHvacManager((CarHvacManager) mCarApiClient.getCarManager(Car.HVAC_SERVICE));
                // 连接成功后，唤醒正在等待CarHvacManager的线程
                mHvacManagerReady.notifyAll();
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in onServiceConnected");
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
};
```
向CarService获取数据需要先得到CarHvacManager的实例，所以在连接成功后，调用**mHvacManagerReady**.notifyAll() 唤醒所有之前等待CarHvacManager实例的线程
```
// HvacUiService.java - mServiceConnection
{
    final Runnable r = () -> {
        // hvac控制器从车辆刷新其值后，绑定所有值。
        mHvacPanelController.updateHvacController(mHvacController);
    };

    if (mHvacController != null) {
        mHvacController.requestRefresh(r, new Handler(context.getMainLooper()));
    }
}

// HvacController.java
public void requestRefresh(final Runnable r, final Handler h) {
    final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... unused) {
            synchronized (mHvacManagerReady) {
                while (mHvacManager == null) {
                    try {
                        mHvacManagerReady.wait();
                    } catch (InterruptedException e) {
                        // We got interrupted so we might be shutting down.
                        return null;
                    }
                }
            }
            // 刷新数据
            fetchTemperature(DRIVER_ZONE_ID);
            fetchTemperature(PASSENGER_ZONE_ID);
            fetchFanSpeed();
            ...
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            // 切换到主线程中执行runnable
            h.post(r);
        }
    };
    task.execute();
}

private void fetchFanSpeed() {
    if (mHvacManager != null) {
        int zone = SEAT_ALL; //特定于汽车的解决方法。
        try {
            int speed = mHvacManager.getIntProperty(CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT, zone);
            mDataStore.setFanSpeed(speed);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanSpeed");
        }
    }
}
```
上面的代码就是利用AsyncTask在子线程中等待CarHvacManager的实例，然后刷新数据并存储在DatStore中。
需要注意一点的是`while (mHvacManager == null)`不能替换成`if(mHvacManager == null)`，这是因为Java有个叫“spurious wakeup”的现象，即线程在不该醒过来的时候醒过来。

> A thread can wake up without being notified, interrupted, or timing out, a so-called *spurious wakeup*. While this will rarely occur in practice, applications must guard against it by testing for the condition that should have caused the thread to be awakened, and continuing to wait if the condition is not satisfied.
> 一个线程有可能会在未被通知、打断、或超时的情况下醒来，这就是所谓的“spurious wakeup”。尽管实际上这种情况很少发生，应用程序仍然必须对此有所防范，手段是检查正常的导致线程被唤醒的条件是否满足，如果不满足就继续等待。

### 3.6 Car API
`Car`是Android汽车平台最高等级的API，为外界提供汽车所有服务和数据访问的接口，提供了一系列与汽车有关的API。它不仅仅可以提供HvacManger，像车辆的速度、档位状态等等所有与汽车有关的信息都可以从Car API中获取。
Hvac中的CarHvacManager实现了`CarManagerBase`接口，并且只要是作为CarXXXManager, 都需要实现`CarManagerBase`接口，如`CarCabinManager`，`CarSensorManager`等都实现了该接口。
CarHvacManager的控制操作是通过`CarPropertyManager`来完成的，`CarPropertyManager`统一控制汽车属性相关的操作。CarHvacManager只是控制与Hvac相关的操作，在汽车中还有很多属性控制的Manager，如传感器，座舱等属性的控制，他们都是通过`CarPropertyManager`进行属性操作，通过在操作时传入的属性ID，属性区域以及属性值，在`CarPropertyManager`中会将这些参数转化为一个`CarPropertyValue`对象继续往`CarService`传递。
```
mHvacManager.getIntProperty(CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT, zone);

private final CarPropertyManager mCarPropertyMgr;

public int getIntProperty(int propertyId, int area) {
    return this.mCarPropertyMgr.getIntProperty(propertyId, area);
}
```
CarHvacManager也是通过注册一个callback来得到 Car API 的数据回调。
```
mHvacManager.registerCallback(mHardwareCallback);

private final CarHvacManager.CarHvacEventCallback mHardwareCallback = new CarHvacManager.CarHvacEventCallback() {
    @Override
    public void onChangeEvent(final CarPropertyValue val) {
        int areaId = val.getAreaId();
        switch (val.getPropertyId()) {
            case CarHvacManager.ID_ZONED_AC_ON:
                handleAcStateUpdate(getValue(val));
                break;
            case CarHvacManager.ID_ZONED_FAN_DIRECTION:
                handleFanPositionUpdate(areaId, getValue(val));
                break;
            case CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT:
                handleFanSpeedUpdate(areaId, getValue(val));
                break;
            case CarHvacManager.ID_ZONED_TEMP_SETPOINT:
                handleTempUpdate(val);
                break;
            case CarHvacManager.ID_WINDOW_DEFROSTER_ON:
                handleDefrosterUpdate(areaId, getValue(val));
                break;
            case CarHvacManager.ID_ZONED_AIR_RECIRCULATION_ON:
                handleAirCirculationUpdate(getValue(val));
                break;
            case CarHvacManager.ID_ZONED_SEAT_TEMP:
                handleSeatWarmerUpdate(areaId, getValue(val));
                break;
            case CarHvacManager.ID_ZONED_AUTOMATIC_MODE_ON:
                handleAutoModeUpdate(getValue(val));
                break;
            case CarHvacManager.ID_ZONED_HVAC_POWER_ON:
                handleHvacPowerOn(getValue(val));
                break;
            default:
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Unhandled HVAC event, id: " + val.getPropertyId());
                }
        }
    }

    @Override
    public void onErrorEvent(final int propertyId, final int zone) {
    }
};
```
Hvac中每个Property对应的含义如下：
```
// 全局属性，只有一个
ID_MIRROR_DEFROSTER_ON  //视镜除雾
ID_STEERING_WHEEL_HEAT  //方向盘温度
ID_OUTSIDE_AIR_TEMP  //室外温度
ID_TEMPERATURE_DISPLAY_UNITS  //在使用的温度
// 区域属性，可在不同区域设置
ID_ZONED_TEMP_SETPOINT  //用户设置的温度
ID_ZONED_TEMP_ACTUAL  //区域实际温度
ID_ZONED_HVAC_POWER_ON  //HVAC系统电源开关
ID_ZONED_FAN_SPEED_SETPOINT  //风扇设置的速度
ID_ZONED_FAN_SPEED_RPM  //风扇实际的速度
ID_ZONED_FAN_DIRECTION_AVAILABLE  //风扇可设置的方向
ID_ZONED_FAN_DIRECTION  //现在风扇设置的方向
ID_ZONED_SEAT_TEMP  //座椅温度
ID_ZONED_AC_ON  //空调开关
ID_ZONED_AUTOMATIC_MODE_ON  //HVAC自动模式开关
ID_ZONED_AIR_RECIRCULATION_ON  //空气循环开关
ID_ZONED_MAX_AC_ON  //空调最大速度开关
ID_ZONED_DUAL_ZONE_ON  //双区模式开关
ID_ZONED_MAX_DEFROST_ON  //最大除雾开关
ID_ZONED_HVAC_AUTO_RECIRC_ON  //自动循环模式开关
ID_WINDOW_DEFROSTER_ON  //除雾模式开关
```
使用Car API时务必需要注意，注册的`callback`是有可能会非常频繁的产生回调的，应用层需要先将数据存储在`DataStore`中进行过滤，才能更新到UI上。而且也不要实时的打印日志，否则可能会导致日志缓冲区EOF，也会严重干扰其它进程的日志输出。
![](https://upload-images.jianshu.io/upload_images/3146091-2f98570d9ddf4aa5.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 3.7 DataStore
DataStore 用于存储`HvacController`从 Car API 中获取的属性值。
用户操作IVI界面和使用硬按键，都会更新Hvac的相关属性。这两种不同的更新方式都是从不同的线程更新到当前状态。此外，在某些情况下，Hvac系统可能会发送虚假的更新，因此这个类将所有内容更新管理合并，从而确保在用户看来应用程序的界面是正常的
```
@GuardedBy("mFanSpeed")
private Integer mFanSpeed = 0;
private static final long COALESCE_TIME_MS = 0L;

public int getFanSpeed() {
    synchronized (mFanSpeed) {
        return mFanSpeed;
    }
}

// 仅用于主动 获取、设定 数据时更新speed数据。
public void setFanSpeed(int speed) {
    synchronized (mFanSpeed) {
        mFanSpeed = speed;
        mLastFanSpeedSet = SystemClock.uptimeMillis();
    }
}

// 从callback中得到数据时，因为数据可能会刷新的很频繁，所以需要先判断时间戳，确定数据是否真的需要更新
public boolean shouldPropagateFanSpeedUpdate(int zone, int speed) {
    // TODO：我们暂时忽略风扇速度区域，因为我们没有多区域车。
    synchronized (mFanSpeed) {
        if (SystemClock.uptimeMillis() - mLastFanSpeedSet < COALESCE_TIME_MS) {
            return false;
        }
        mFanSpeed = speed;
    }
    return true;
}
```
在`HvacController`中我们从`callback`得到数据刷新时，先通过`DataStore`判断以下是否需要更新数据，如果确实需要更新，再将更新后的数据回调给其他的UI控制器。
```
// HvacController.java
private final CarHvacManager.CarHvacEventCallback mHardwareCallback = new CarHvacManager.CarHvacEventCallback() {
    @Override
    public void onChangeEvent(final CarPropertyValue val) {
        int areaId = val.getAreaId();
        switch (val.getPropertyId()) {
            case CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT:
                // 处理来自callback的数据
                handleFanSpeedUpdate(areaId, getValue(val));
                break;
                // ... 省略
            default:
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Unhandled HVAC event, id: " + val.getPropertyId());
                }
        }
    }
};

private void handleFanSpeedUpdate(int zone, int speed) {
    // 判断是否需要更新本地的数据
    boolean shouldPropagate = mDataStore.shouldPropagateFanSpeedUpdate(zone, speed);
    if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Fan Speed Update, zone: " + zone + " speed: " + speed +
                " should propagate: " + shouldPropagate);
    }
    if (shouldPropagate) {
        // 将更新后的数据回调给各个UI控制器
        synchronized (mCallbacks) {
            for (int i = 0; i < mCallbacks.size(); i++) {
                mCallbacks.get(i).onFanSpeedChange(speed);
            }
        }
    }
}

public void setFanSpeed(final int fanSpeed) {
    // 更新当前的数据
    mDataStore.setFanSpeed(fanSpeed);

    final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
        int newFanSpeed;

        protected Void doInBackground(Void... unused) {
            if (mHvacManager != null) {
                int zone = SEAT_ALL; // Car specific workaround.
                try {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Setting fanspeed to: " + fanSpeed);
                    }
                    mHvacManager.setIntProperty(
                            CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT, zone, fanSpeed);

                    newFanSpeed = mHvacManager.getIntProperty(
                            CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT, zone);
                } catch (android.car.CarNotConnectedException e) {
                    Log.e(TAG, "Car not connected in setFanSpeed");
                }
            }
            return null;
        }
    };
    task.execute();
}
```

##4. 总结
最后我们以一张从Car API的`callback`中的数据更新界面的**伪时序图**来把Hvac的几个核心组件串起来
![](https://upload-images.jianshu.io/upload_images/3146091-eb3c27e96e7d98f2.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
以上就是车载空调部分的讲解，实际开发中，空调模块功能性需求一般不会出现什么太大的技术性困难，空调模块的技术性难度几乎都体现在复杂的动画和交互上，有关车载应用的复杂动画技术，我们以后在来细讲解决方案。
