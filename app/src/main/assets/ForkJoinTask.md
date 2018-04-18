### 链接

[JAVA并行计算之ForkJoinTask使用样例](http://blog.csdn.net/puhaiyang/article/details/77581296)  
[分解和合并：Java 也擅长轻松的并行编程！](http://www.oracle.com/technetwork/cn/articles/java/fork-join-422606-zhs.html)

---

### ForkJoinTask 简介

有些类型的算法要求任务创建子任务并与其他任务互相通信以完成任务。这些是“分而治之”的算法，也称为“映射归约”。其思路是将算法要处理的数据空间拆分成较小的独立块，这是“映射”阶段；一旦块集处理完毕之后，就可以将部分结果收集起来形成最终结果，这是“归约”阶段。

ForkJoinTask 对象支持创建子任务并等待子任务完成。

ForkJoinTask 对象有两种特定方法：

* fork() 方法允许计划 ForkJoinTask 异步执行。这允许从现有 ForkJoinTask 启动新的 ForkJoinTask。
* 而 join() 方法允许 ForkJoinTask 等待另一个 ForkJoinTask 完成。

### ForkJoinTask 使用示例

一个简单的示例是您希望计算一个大型整数数组的总和。假定加法是可交换的，可以将数组划分为较小的部分，并发线程对这些部分计算部分和。然后将部分和相加，计算总和。因为对于此算法，线程可以在数组的不同区域上独立运行，所以与对数组中每个整数循环执行的单线程算法相比，此算法在多核架构上可以看到明显的性能提升。

以下是计算 1 + 2 + 3 + …… + 10000000 的和的代码：

#### 不采用 ForkJoinTask 的实现方式

```java
private void notForkJoinTask() {
    Long sum = 0L;
    Long maxSize = 10000000L;
    for (Long i = 1L; i <= maxSize; i++) {
        sum += i;
    }
}
```

#### 采用 ForkJoinTask 的代码

```java
public class CountTask extends RecursiveTask<Long> {
    Long maxCountRange = 5000000L;//最大计算范围
    Long startNum;
    Long endNum;

    public CountTask(Long startNum, Long endNum) {
        this.startNum = startNum;
        this.endNum = endNum;
    }

    @Override
    protected Long compute() {
        Long range = endNum - startNum;
        long sum = 0;
        if (range >= maxCountRange) {
            Long middle = (startNum + endNum) / 2;
            CountTask subTask1 = new CountTask(startNum, middle);
            CountTask subTask2 = new CountTask(middle + 1, endNum);
            subTask1.fork();
            subTask2.fork();

            sum += subTask1.join();
            sum += subTask2.join();
        } else {
            for (; startNum <= endNum; startNum++) {
                sum += startNum;
            }
        }
        return sum;
    }
}

private void useForkJoinTask() {
    CountTask countTask = new CountTask(1L, maxSize);
    ForkJoinPool forkJoinPool = new ForkJoinPool();
    Future<Long> result = forkJoinPool.submit(countTask);
    try {
        MLog.d("结果：" + result.get());
    } catch (InterruptedException e) {
        e.printStackTrace();
    } catch (ExecutionException e) {
        e.printStackTrace();
    }
}
```
