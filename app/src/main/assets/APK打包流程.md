### 链接

[Apk 打包流程梳理](https://juejin.im/entry/58b78d1b61ff4b006cd47e5b)  
[Android APK打包流程](http://shinelw.com/2016/04/27/android-make-apk/)

---

### Apk 打包流程

1. 通过 aapt 打包 res **资源文件** ，生成 R.java、resources.arsc 和 res 文件（二进制 & 非二进制如 res/raw 和 pic 保持原样）
1. 处理 **.aidl 文件**，生成对应的 Java 接口文件
1. 通过 Java Compiler 编译 R.java、Java 接口文件、Java 源文件，**生成 .class 文件**
1. 通过 dex 命令，将 .class 文件和第三方库中的 .class 文件处理 **生成classes.dex**
1. 通过 apkbuilder 工具，将 aapt 生成的 resources.arsc 和 res 文件、assets 文件和 classes.dex 一起 **打包生成apk**
1. 通过 Jarsigner 工具，对上面的 apk 进行 debug 或 release **签名**
1. 通过 zipalign 工具，将签名后的 apk 进行 **对齐处理**。

---

### 打包资源文件，生成 R.java 文件

打包资源的工具是 aapt（The Android Asset Packaing Tool），位于 android-sdk/platform-tools 目录下。在这个过程中，项目中的 AndroidManifest.xml 文件和布局文件 XML 都会编译，然后生成相应的 R.java。

### 处理 aidl 文件，生成相应的 Java 文件

这一过程中使用到的工具是 aidl（Android Interface Definition Language），即 Android 接口描述语言。位于 android-sdk/platform-tools 目录下。aidl 工具解析接口定义文件然后生成相应的 Java 代码接口供程序调用。

### 编译项目源代码，生成 class 文件

项目中所有的 Java 代码，包括 R.java 和 .aidl 文件，都会被 Java 编译器（javac）编译成 .class 文件，生成的 class 文件位于工程中的 bin/classes 目录下。

### 转换所有的 class 文件，生成 classes.dex 文件

dx 工具生成可供 Android 系统 Dalvik 虚拟机执行的 classes.dex 文件，该工具位于 android-sdk/platform-tools 目录下。

任何第三方的 libraries 和 .class 文件都会被转换成 .dex 文件。

dx 工具的主要工作是将 Java 字节码转换成 Dalvik 字节码、压缩常量池、消除冗余信息等。

### 打包生成 Apk 文件

所有没有编译的资源（如 images 等）、编译过的资源和 .dex 文件都会被 apkbuilder 工具打包到最终的 .apk 文件中。

打包的工具 apkbuilder 位于 android-sdk/tools 目录下。apkbuilder 为一个脚本文件，实际调用的是 android-sdk/tools/lib/sdklib.jar 文件中的 com.android.sdklib.build.ApkbuilderMain 类。

### 对 Apk 文件进行签名

一旦 Apk 文件生成，它必须被签名才能被安装在设备上。

在开发过程中，主要用到的就是两种签名的 keystore。一种是用于调试的 debug.keystore，它主要用于调试，在 Eclipse 或者 Android Studio 中直接 run 以后跑在手机上的就是使用的 debug.keystore。另一种就是用于发布正式版本的 keystore。

### 对签名后的 Apk 文件进行对齐处理

如果你发布的 Apk 是正式版的话，就必须对 Apk 进行对齐处理，用到的工具是 zipalign，它位于 android-sdk/tools 目录下。

对齐的主要过程是将 Apk 包中所有的资源文件距离文件起始偏移为 4 字节整数倍，这样通过内存映射访问 Apk 文件时的速度会更快。

对齐的作用就是减少运行时内存的使用。
