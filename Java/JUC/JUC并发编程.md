# Java JUC并发编程

## 1. JUC概述

JUC (java.util.concurrent) 是Java提供的用于并发编程的工具包，位于java.util.concurrent包下。它提供了比synchronized和wait/notify等传统同步机制更高级、更灵活的并发工具。JUC于Java 5引入，并在后续版本中不断完善。

## 2. 线程与线程池

### 2.1 线程基础

线程是程序的执行单元，Java中使用Thread类表示线程。

```java
// 创建线程的方式
// 1. 继承Thread类
class MyThread extends Thread {
    @Override
    public void run() {
        // 线程执行的代码
    }
}

// 2. 实现Runnable接口
class MyRunnable implements Runnable {
    @Override
    public void run() {
        // 线程执行的代码
    }
}

// 3. 实现Callable接口（可以有返回值）
class MyCallable implements Callable<String> {
    @Override
    public String call() throws Exception {
        // 线程执行的代码
        return "线程执行结果";
    }
}

// 4. 使用Lambda表达式（Java 8+）
Thread thread = new Thread(() -> {
    // 线程执行的代码
});
```

### 2.2 线程状态

Java线程的六种状态（Thread.State枚举）：
- NEW：新创建，尚未启动
- RUNNABLE：可运行状态，可能正在运行也可能在等待CPU资源
- BLOCKED：阻塞状态，等待获取锁
- WAITING：等待状态，无限期等待另一个线程执行特定操作
- TIMED_WAITING：超时等待状态，在指定时间内等待
- TERMINATED：终止状态，线程执行完毕

### 2.3 线程池

线程池用于管理线程的创建和销毁，提高线程的复用性能。JUC中提供了ExecutorService接口及其实现类来支持线程池。

```java
// 常见线程池创建方式
// 1. 固定大小线程池
ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);

// 2. 缓存线程池（线程数量不固定，按需创建）
ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

// 3. 单线程线程池
ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

// 4. 定时任务线程池
ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);

// 5. 自定义线程池（推荐）
ThreadPoolExecutor customThreadPool = new ThreadPoolExecutor(
    5,                      // 核心线程数
    10,                     // 最大线程数
    60L,                    // 空闲线程存活时间
    TimeUnit.SECONDS,       // 时间单位
    new LinkedBlockingQueue<>(100), // 任务队列
    Executors.defaultThreadFactory(), // 线程工厂
    new ThreadPoolExecutor.AbortPolicy() // 拒绝策略
);
```

#### 面试常考点与答案
1. **线程的生命周期与状态转换有哪些？**
   - 答：Java线程有六种状态：NEW（新建）、RUNNABLE（运行/就绪）、BLOCKED（阻塞）、WAITING（等待）、TIMED_WAITING（超时等待）、TERMINATED（终止）。状态之间的转换由线程的启动、等待、唤醒、结束等操作触发。
2. **线程的创建方式有哪些？各自优缺点？**
   - 答：主要有三种方式：继承Thread类（不推荐，单继承限制）；实现Runnable接口（推荐，便于资源共享）；实现Callable接口（可有返回值和异常）；线程池（推荐，线程复用，资源可控，适合高并发场景）。
3. **线程池的核心参数有哪些？如何理解？**
   - 答：corePoolSize（核心线程数）、maximumPoolSize（最大线程数）、keepAliveTime（非核心线程空闲存活时间）、workQueue（任务队列）、threadFactory（线程工厂）、handler（拒绝策略）。
4. **线程池的工作原理？**
   - 答：任务提交后，先用核心线程处理，满了则进队列，队列满了用非核心线程，线程数到最大后再满则触发拒绝策略。
5. **常见线程池类型及适用场景？**
   - 答：FixedThreadPool（固定线程数，适合任务量已知、长期运行的场景）；CachedThreadPool（线程数不定，适合短任务、突发任务）；SingleThreadExecutor（单线程，适合顺序执行任务）；ScheduledThreadPool（定时/周期任务）；ThreadPoolExecutor（自定义参数，灵活控制）。
6. **线程池的关闭方式有哪两种？区别？**
   - 答：shutdown()：平滑关闭，等待已提交任务执行完；shutdownNow()：立即关闭，尝试中断正在执行的任务。
7. **线程池OOM的原因及如何避免？**
   - 答：原因：任务过多、队列无限、线程数过大。避免：合理设置队列容量、线程数，监控任务堆积，拒绝策略处理。
8. **线程安全问题有哪些？如何保证线程安全？**
   - 答：常见问题有竞态条件、可见性、原子性。可通过synchronized、Lock、原子类、并发集合等保证线程安全。
9. **线程池参数设置不合理会导致什么问题？**
   - 答：线程数过小导致任务堆积，过大导致资源耗尽，队列过大易OOM，拒绝策略不当会丢任务或阻塞主线程。

## 3. 并发集合类

JUC提供了线程安全的集合类，主要包括：

### 3.1 ConcurrentHashMap

线程安全的HashMap实现，采用分段锁机制，提供较高的并发性能。

```java
// 创建ConcurrentHashMap
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("key1", 100);
map.putIfAbsent("key2", 200); // 仅在key不存在时插入

// 原子操作
map.compute("key1", (k, v) -> v + 1); // 原子地计算新值
map.computeIfPresent("key1", (k, v) -> v * 2); // 仅在key存在时计算
map.computeIfAbsent("key3", k -> 300); // 仅在key不存在时计算并添加
```

### 3.2 CopyOnWriteArrayList

线程安全的ArrayList实现，适用于读多写少的场景。在写操作时复制整个数组，写完后替换。

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("item1");
list.add("item2");

// 线程安全地遍历
for (String item : list) {
    System.out.println(item);
}
```

### 3.3 CopyOnWriteArraySet

基于CopyOnWriteArrayList实现的线程安全Set。

### 3.4 ConcurrentLinkedQueue

线程安全的非阻塞队列，使用CAS操作实现。

```java
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
queue.offer("task1");
queue.offer("task2");
String task = queue.poll(); // 获取并移除头元素，无元素时返回null
```

### 3.5 阻塞队列

线程安全的队列，当队列满或空时，线程会阻塞等待。

```java
// 常见阻塞队列
ArrayBlockingQueue<String> arrayQueue = new ArrayBlockingQueue<>(100); // 固定容量
LinkedBlockingQueue<String> linkedQueue = new LinkedBlockingQueue<>(); // 容量可选
PriorityBlockingQueue<String> priorityQueue = new PriorityBlockingQueue<>(); // 带优先级
DelayQueue<Delayed> delayQueue = new DelayQueue<>(); // 延迟队列
SynchronousQueue<String> synchronousQueue = new SynchronousQueue<>(); // 无缓冲队列

// 阻塞操作
arrayQueue.put("task"); // 如果队列满，会阻塞
String task = arrayQueue.take(); // 如果队列空，会阻塞

// 非阻塞操作
arrayQueue.offer("task"); // 如果队列满，返回false
arrayQueue.offer("task", 1, TimeUnit.SECONDS); // 尝试指定时间
String task = arrayQueue.poll(); // 如果队列空，返回null
String task = arrayQueue.poll(1, TimeUnit.SECONDS); // 尝试指定时间
```

#### 面试常考点与答案
1. **HashMap和ConcurrentHashMap的区别？**
   - 答：HashMap线程不安全，ConcurrentHashMap线程安全。JDK7前者用分段锁，JDK8用CAS+链表/红黑树。HashMap允许null键值，ConcurrentHashMap不允许。
2. **ConcurrentHashMap的线程安全实现机制？**
   - 答：JDK7采用分段锁（Segment），JDK8采用CAS+链表/红黑树，节点数组+链表/红黑树+CAS无锁操作，提升并发性能。
3. **CopyOnWriteArrayList/Set的适用场景和原理？**
   - 答：适合读多写少场景。写操作时复制整个数组，写完后替换，读操作无锁，写操作开销大。
4. **BlockingQueue常见实现及应用场景？**
   - 答：ArrayBlockingQueue（有界，FIFO）、LinkedBlockingQueue（可选容量，FIFO）、SynchronousQueue（无缓冲）、DelayQueue（延迟队列）、PriorityBlockingQueue（优先级队列）。常用于线程池、生产者消费者模型。
5. **ConcurrentLinkedQueue和BlockingQueue的区别？**
   - 答：ConcurrentLinkedQueue是无界非阻塞队列，适合高并发场景；BlockingQueue有阻塞特性，适合生产者消费者。
6. **并发集合的弱一致性迭代器和fail-fast区别？**
   - 答：弱一致性（fail-safe）允许并发修改，遍历结果不抛异常；fail-fast在结构被修改时抛ConcurrentModificationException。
7. **CopyOnWriteArrayList的缺点？**
   - 答：写操作时复制整个数组，写多时性能极差，内存消耗大，不适合写多场景。
8. **ConcurrentHashMap的size()方法为什么不是实时的？**
   - 答：因为并发修改时，size()统计可能不准确，JDK8后有sumCount()方法提高准确性。

## 4. 原子类

JUC提供了原子操作类，基于CAS（Compare And Swap）实现，避免锁的开销。

### 4.1 基本类型原子类

```java
// 常见原子类
AtomicInteger atomicInt = new AtomicInteger(0);
AtomicLong atomicLong = new AtomicLong(0);
AtomicBoolean atomicBoolean = new AtomicBoolean(false);

// 原子操作
int value = atomicInt.get();
atomicInt.set(10);
int oldValue = atomicInt.getAndSet(20);
int newValue = atomicInt.incrementAndGet(); // 原子自增
int oldValue2 = atomicInt.getAndIncrement(); // 原子自增
atomicInt.addAndGet(5); // 原子加法
atomicInt.compareAndSet(25, 30); // CAS操作
```

### 4.2 数组类型原子类

```java
AtomicIntegerArray atomicArray = new AtomicIntegerArray(10);
atomicArray.set(0, 100);
atomicArray.getAndAdd(1, 50);
```

### 4.3 引用类型原子类

```java
AtomicReference<User> userRef = new AtomicReference<>(new User("张三"));
userRef.compareAndSet(userRef.get(), new User("李四"));

// 带版本号的引用类型原子类
AtomicStampedReference<String> stampedRef = new AtomicStampedReference<>("value", 0);
stampedRef.compareAndSet("value", "newValue", 0, 1); // 比较值和版本号
```

### 4.4 字段更新器

可以对指定类的指定字段进行原子操作。

```java
class User {
    public volatile int id;
    public volatile String name;
}

// 创建更新器
AtomicIntegerFieldUpdater<User> idUpdater = 
    AtomicIntegerFieldUpdater.newUpdater(User.class, "id");
AtomicReferenceFieldUpdater<User, String> nameUpdater = 
    AtomicReferenceFieldUpdater.newUpdater(User.class, String.class, "name");

// 使用更新器
User user = new User();
idUpdater.getAndIncrement(user);
nameUpdater.compareAndSet(user, null, "张三");
```

#### 面试常考点与答案
1. **CAS原理是什么？**
   - 答：CAS（Compare And Swap）即比较并交换，先比较内存中的值与期望值是否相等，相等则更新为新值，否则重试。保证了原子性。
2. **CAS会带来哪些问题？如何解决？**
   - 答：主要有ABA问题（值变了又变回去，CAS无法感知），可用AtomicStampedReference加版本号解决；自旋时间长开销大；只能保证单变量原子性。
3. **原子类的底层实现？**
   - 答：基于Unsafe类的native方法，利用volatile和CAS自旋实现无锁原子操作。
4. **AtomicInteger和synchronized的区别？**
   - 答：AtomicInteger基于CAS，适合高并发、无锁场景，性能高；synchronized是重量级锁，适合复杂同步，能保证多变量原子性。
5. **字段更新器的作用？**
   - 答：可对指定类的指定字段进行原子操作，适合批量对象的原子字段更新，减少对象数量。
6. **CAS只能保证单变量原子性，多个变量怎么办？**
   - 答：可用AtomicReference封装对象，或用锁保证多变量原子性。

## 5. 锁机制

JUC提供了功能更强大的锁机制，比synchronized更灵活。

### 5.1 ReentrantLock

可重入锁，功能与synchronized类似，但提供了更多选项。

```java
ReentrantLock lock = new ReentrantLock(); // 默认非公平锁
ReentrantLock fairLock = new ReentrantLock(true); // 公平锁

// 基本使用
lock.lock();
try {
    // 临界区代码
} finally {
    lock.unlock(); // 必须在finally中释放锁
}

// 尝试获取锁
if (lock.tryLock()) { // 非阻塞尝试
    try {
        // 获取锁成功
    } finally {
        lock.unlock();
    }
}

// 限时尝试获取锁
if (lock.tryLock(1, TimeUnit.SECONDS)) {
    try {
        // 获取锁成功
    } finally {
        lock.unlock();
    }
}

// 可中断获取锁
try {
    lock.lockInterruptibly();
    try {
        // 临界区代码
    } finally {
        lock.unlock();
    }
} catch (InterruptedException e) {
    // 响应中断
}
```

### 5.2 ReentrantReadWriteLock

读写锁，允许多个线程同时读，但只允许一个线程写。

```java
ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
ReadLock readLock = rwLock.readLock();
WriteLock writeLock = rwLock.writeLock();

// 读锁（共享锁）
readLock.lock();
try {
    // 读取操作
} finally {
    readLock.unlock();
}

// 写锁（排他锁）
writeLock.lock();
try {
    // 写入操作
} finally {
    writeLock.unlock();
}
```

### 5.3 StampedLock

Java 8引入的新锁，提供了乐观读的功能，性能更高。

```java
StampedLock lock = new StampedLock();

// 写锁
long writeStamp = lock.writeLock();
try {
    // 写入操作
} finally {
    lock.unlockWrite(writeStamp);
}

// 读锁
long readStamp = lock.readLock();
try {
    // 读取操作
} finally {
    lock.unlockRead(readStamp);
}

// 乐观读（无锁）
long optimisticStamp = lock.tryOptimisticRead();
// 读取数据
if (lock.validate(optimisticStamp)) {
    // 数据未被修改，可以继续使用
} else {
    // 数据已被修改，需要获取读锁重新读取
    readStamp = lock.readLock();
    try {
        // 重新读取
    } finally {
        lock.unlockRead(readStamp);
    }
}
```

### 5.4 Condition

与Lock配合使用的条件变量，类似于Object的wait/notify。

```java
ReentrantLock lock = new ReentrantLock();
Condition condition = lock.newCondition();

// 等待条件
lock.lock();
try {
    while (!满足条件) {
        condition.await(); // 释放锁并等待
    }
    // 条件满足后的操作
} finally {
    lock.unlock();
}

// 通知条件
lock.lock();
try {
    // 改变条件
    condition.signal(); // 唤醒一个等待线程
    condition.signalAll(); // 唤醒所有等待线程
} finally {
    lock.unlock();
}
```

#### 面试常考点与答案
1. **synchronized和Lock的区别？**
   - 答：synchronized是JVM内置关键字，自动加解锁，不能中断、不可定时、公平性不可控；Lock是接口，手动加解锁，可中断、可定时、公平性可选、可多条件等待。
2. **ReentrantLock有哪些特性？**
   - 答：可重入（同一线程可多次获取）、可中断、可定时、公平/非公平选择、支持Condition实现多条件等待。
3. **读写锁的应用场景？**
   - 答：读多写少场景，提升并发性能。多个线程可同时读，写时互斥。
4. **读写锁能否升级/降级？**
   - 答：支持降级（写锁降为读锁），不支持升级（读锁不能直接升级为写锁）。
5. **StampedLock的乐观读和悲观读区别？**
   - 答：乐观读无锁，适合读多写少，性能高；悲观读加锁，保证一致性。
6. **死锁的产生条件？如何避免？**
   - 答：互斥、不可抢占、请求与保持、循环等待。避免方法：破坏四个条件之一，如加锁顺序、定时锁、死锁检测等。
7. **Condition的作用？**
   - 答：实现多条件等待/通知，类似Object的wait/notify，但更灵活。

## 6. volatile关键字

volatile保证了变量的可见性和有序性，但不保证原子性。

```java
public class VolatileExample {
    private volatile boolean flag = false;
    
    public void setFlag() {
        flag = true; // 对所有线程立即可见
    }
    
    public boolean isFlag() {
        return flag;
    }
}
```

#### 面试常考点与答案
1. **volatile的作用是什么？**
   - 答：保证变量的可见性和禁止指令重排序（有序性），但不保证原子性。
2. **volatile能替代synchronized吗？**
   - 答：不能。volatile只能保证单变量的可见性和有序性，不能保证复合操作的原子性。
3. **volatile的实现原理？**
   - 答：底层通过内存屏障（Memory Barrier）实现，写操作会刷新主内存，读操作会从主内存读取。
4. **volatile适用场景？**
   - 答：状态标志、单例模式的双重检查锁、轻量级读写场景。

## 7. CAS操作原理

CAS（Compare And Swap）是一种原子操作，保证了在多线程环境下的数据一致性。

```java
// AtomicInteger内部使用CAS
public final boolean compareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

CAS原理：
1. 比较当前值和预期值是否相等
2. 如果相等，则将当前值设为新值（这一过程是原子的）
3. 如果不相等，则操作失败

CAS的问题：
1. ABA问题：通过版本号解决（AtomicStampedReference）
2. 循环时间长开销大：自旋次数控制
3. 只能保证一个共享变量的原子操作：将多个变量封装成一个对象

## 8. ThreadLocal原理

ThreadLocal提供线程局部变量，每个线程都有该变量的独立副本。

```java
// 创建ThreadLocal
ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

// 设置值
userThreadLocal.set(new User("张三"));

// 获取值
User user = userThreadLocal.get();

// 删除值（防止内存泄漏）
userThreadLocal.remove();

// 使用初始值
ThreadLocal<User> userThreadLocal2 = ThreadLocal.withInitial(() -> new User("默认用户"));
```

ThreadLocal工作原理：
1. 每个Thread内部有一个ThreadLocalMap
2. ThreadLocalMap以ThreadLocal为key，值为线程的变量副本
3. 调用set()时，以ThreadLocal为key，存储值到当前线程的ThreadLocalMap中
4. 调用get()时，以ThreadLocal为key，获取当前线程的变量副本

注意：使用完ThreadLocal后需要调用remove()方法，否则可能导致内存泄漏。

## 9. AQS（AbstractQueuedSynchronizer）

AQS是构建锁和同步器的框架，ReentrantLock、CountDownLatch、Semaphore等都基于AQS实现。

AQS核心原理：
1. 维护一个volatile int state（锁状态）
2. 提供队列方式管理线程等待
3. 提供独占/共享两种资源访问模式
4. 子类通过继承并实现相关方法管理状态

## 10. Fork/Join框架

Fork/Join是Java 7引入的用于并行执行任务的框架，特别适合"分治"算法。

```java
class SumTask extends RecursiveTask<Long> {
    private static final long THRESHOLD = 10000;
    private long start;
    private long end;
    
    public SumTask(long start, long end) {
        this.start = start;
        this.end = end;
    }
    
    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            // 小任务直接计算
            long sum = 0;
            for (long i = start; i <= end; i++) {
                sum += i;
            }
            return sum;
        } else {
            // 大任务拆分
            long middle = (start + end) / 2;
            SumTask leftTask = new SumTask(start, middle);
            SumTask rightTask = new SumTask(middle + 1, end);
            
            leftTask.fork(); // 提交子任务
            long rightResult = rightTask.compute(); // 计算右边
            long leftResult = leftTask.join(); // 获取左边结果
            
            return leftResult + rightResult;
        }
    }
}

// 使用Fork/Join
ForkJoinPool pool = new ForkJoinPool();
SumTask task = new SumTask(1, 1000000);
long result = pool.invoke(task);
```

#### 面试常考点与答案
1. **Fork/Join框架的原理？**
   - 答：采用分治思想，将大任务拆分为小任务并行执行，最终合并结果。底层用工作窃取算法（Work Stealing）。
2. **Fork/Join的核心类有哪些？**
   - 答：ForkJoinPool（线程池）、ForkJoinTask（任务抽象）、RecursiveTask（有返回值）、RecursiveAction（无返回值）。
3. **工作窃取算法是什么？**
   - 答：每个线程维护一个双端队列，空闲线程可从其他线程队列尾部"窃取"任务，提高CPU利用率。
4. **Fork/Join的适用场景？**
   - 答：大任务可拆分为小任务并行处理的场景，如大数据计算、递归分治。

## 11. CompletableFuture异步编程

CompletableFuture是Java 8引入的异步编程工具，提供了函数式编程的异步任务处理方式。

```java
// 创建异步任务
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    // 执行异步任务
    return "结果1";
});

// 转换结果
CompletableFuture<Integer> future2 = future1.thenApply(s -> s.length());

// 消费结果
future2.thenAccept(length -> System.out.println("长度: " + length));

// 组合两个Future
CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> future4 = CompletableFuture.supplyAsync(() -> "World");
CompletableFuture<String> result = future3.thenCombine(future4, (s1, s2) -> s1 + " " + s2);

// 等待多个Future完成
CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3, future4);
allFutures.join(); // 等待所有完成

// 异常处理
CompletableFuture<String> future5 = CompletableFuture.supplyAsync(() -> {
    if (Math.random() < 0.5) {
        throw new RuntimeException("错误");
    }
    return "成功";
}).exceptionally(ex -> "发生异常: " + ex.getMessage());
```

#### 面试常考点与答案
1. **CompletableFuture和Future的区别？**
   - 答：Future只能阻塞获取结果，不能链式编程；CompletableFuture支持链式、组合、异步回调、异常处理等，功能更强大。
2. **CompletableFuture的常用方法？**
   - 答：supplyAsync、runAsync、thenApply、thenAccept、thenCombine、thenCompose、exceptionally、whenComplete、allOf、anyOf等。
3. **如何实现多个异步任务并行、聚合结果？**
   - 答：用allOf聚合多个CompletableFuture，最后用join或get获取所有结果。
4. **CompletableFuture如何处理异常？**
   - 答：可用exceptionally、handle、whenComplete等方法处理异常。
5. **CompletableFuture的线程池如何指定？**
   - 答：supplyAsync、runAsync等方法可传入自定义Executor参数。

## 12. JUC工具类

### 12.1 CountDownLatch

用于等待一组线程完成操作。

```java
CountDownLatch latch = new CountDownLatch(3); // 初始计数为3

// 工作线程
new Thread(() -> {
    // 执行任务
    latch.countDown(); // 计数减1
}).start();

// 等待所有工作线程完成
latch.await(); // 阻塞直到计数为0
```

### 12.2 CyclicBarrier

让一组线程到达某个点后再一起继续执行。

```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    // 所有线程到达屏障后执行
    System.out.println("All threads arrived at barrier");
});

// 工作线程
new Thread(() -> {
    // 第一阶段工作
    barrier.await(); // 等待其他线程到达
    // 第二阶段工作
}).start();
```

### 12.3 Semaphore

控制同时访问某个资源的线程数量。

```java
Semaphore semaphore = new Semaphore(3); // 最多3个线程同时访问

// 工作线程
new Thread(() -> {
    try {
        semaphore.acquire(); // 获取许可
        // 访问受限资源
    } finally {
        semaphore.release(); // 释放许可
    }
}).start();
```

### 12.4 Exchanger

用于两个线程之间交换数据。

```java
Exchanger<String> exchanger = new Exchanger<>();

// 线程1
new Thread(() -> {
    try {
        String data1 = "数据1";
        String data2 = exchanger.exchange(data1); // 发送data1，接收data2
        System.out.println("线程1收到: " + data2);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}).start();

// 线程2
new Thread(() -> {
    try {
        String data2 = "数据2";
        String data1 = exchanger.exchange(data2); // 发送data2，接收data1
        System.out.println("线程2收到: " + data1);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}).start();
```

#### 面试常考点与答案
1. **CountDownLatch的原理和应用？**
   - 答：基于AQS实现，计数器归零前所有线程阻塞，常用于主线程等待子线程完成。
2. **CyclicBarrier和CountDownLatch的区别？**
   - 答：CyclicBarrier可循环使用，适合多线程分阶段同步；CountDownLatch一次性。
3. **Semaphore的作用？**
   - 答：控制同时访问某资源的线程数量，常用于限流、连接池等。
4. **Exchanger的应用场景？**
   - 答：用于两个线程间数据交换，如生产者-消费者模型中的缓冲区切换。

## 13. 并发编程最佳实践

#### 面试常考点与答案
1. **如何避免死锁？**
   - 答：加锁顺序一致、定时锁、死锁检测、减少锁粒度、使用并发集合等。
2. **如何选择合适的并发工具？**
   - 答：根据场景选择，如高并发用CAS/原子类，读多写少用读写锁，线程隔离用ThreadLocal，任务调度用线程池。
3. **并发编程中常见的性能优化点？**
   - 答：减少锁竞争、缩小锁粒度、使用无锁结构、合理配置线程池、避免频繁创建销毁线程。
4. **如何排查并发问题？**
   - 答：用日志、线程Dump、JStack、JVisualVM、Arthas等工具分析死锁、阻塞、线程泄漏等。 