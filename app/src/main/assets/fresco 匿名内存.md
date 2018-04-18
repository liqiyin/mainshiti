### 链接

<https://www.jianshu.com/p/d9bc9c668ba6>
<https://github.com/facebook/fresco/issues/1016>
<https://stackoverflow.com/questions/34283808/why-fresco-dose-not-put-bitmap-in-ashmem-on-android-5-0-or-higher>

---

5.0 之前 fresco 用的是 Android 匿名共享内存。
匿名共享内存不会占用Dalvik Heap与Native Heap，不会导致OOM，这是优点，同时也是缺点，因为如果肆意使用，会导致系统资源不足，性能下降。
5.0 之后就不能使用匿名共享内存了

```java
/**
* @deprecated As of {@link android.os.Build.VERSION_CODES#LOLLIPOP}, this is
* ignored.
*
* In {@link android.os.Build.VERSION_CODES#KITKAT} and below, if this
* is set to true, then the resulting bitmap will allocate its
* pixels such that they can be purged if the system needs to reclaim
* memory. In that instance, when the pixels need to be accessed again
* (e.g. the bitmap is drawn, getPixels() is called), they will be
* automatically re-decoded.
*
* <p>For the re-decode to happen, the bitmap must have access to the
* encoded data, either by sharing a reference to the input
* or by making a copy of it. This distinction is controlled by
* inInputShareable. If this is true, then the bitmap may keep a shallow
* reference to the input. If this is false, then the bitmap will
* explicitly make a copy of the input data, and keep that. Even if
* sharing is allowed, the implementation may still decide to make a
* deep copy of the input data.</p>
*
* <p>While inPurgeable can help avoid big Dalvik heap allocations (from
* API level 11 onward), it sacrifices performance predictability since any
* image that the view system tries to draw may incur a decode delay which
* can lead to dropped frames. Therefore, most apps should avoid using
* inPurgeable to allow for a fast and fluid UI. To minimize Dalvik heap
* allocations use the {@link #inBitmap} flag instead.</p>
*
* <p class="note"><strong>Note:</strong> This flag is ignored when used
* with {@link #decodeResource(Resources, int,
* android.graphics.BitmapFactory.Options)} or {@link #decodeFile(String,
* android.graphics.BitmapFactory.Options)}.</p>
*/
@Deprecated
public boolean inPurgeable;
```
