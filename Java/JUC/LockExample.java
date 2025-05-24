import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * 锁机制示例代码
 * 演示了JUC中各种锁的使用方法和特性
 */
public class LockExample {

    public static void main(String[] args) throws InterruptedException {
        // 1. ReentrantLock示例
        reentrantLockExample();
        
        // 2. 读写锁示例
        readWriteLockExample();
        
        // 3. StampedLock示例
        stampedLockExample();
        
        // 4. Condition示例
        conditionExample();
    }
    
    /**
     * ReentrantLock示例
     * 演示可重入锁的基本使用、尝试获取锁、公平锁等特性
     */
    private static void reentrantLockExample() throws InterruptedException {
        System.out.println("=== ReentrantLock示例 ===");
        
        // 创建非公平锁（默认）
        final ReentrantLock nonFairLock = new ReentrantLock();
        // 创建公平锁
        final ReentrantLock fairLock = new ReentrantLock(true);
        
        // 用于计数的共享资源
        final Counter counter = new Counter();
        
        // 测试锁重入特性
        System.out.println("测试锁的重入特性:");
        nonFairLock.lock();
        try {
            System.out.println("获取锁第一次");
            nonFairLock.lock();
            try {
                System.out.println("获取锁第二次（重入）");
                System.out.println("当前持有锁数量: " + nonFairLock.getHoldCount());
            } finally {
                nonFairLock.unlock();
            }
            System.out.println("释放一次后，持有锁数量: " + nonFairLock.getHoldCount());
        } finally {
            nonFairLock.unlock();
        }
        System.out.println("完全释放锁后，持有锁数量: " + nonFairLock.getHoldCount());
        
        // 测试多线程竞争锁
        System.out.println("\n测试多线程竞争锁:");
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(10);
        
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // 基本锁获取
                    nonFairLock.lock();
                    try {
                        counter.increment();
                        System.out.println("线程" + taskId + " 使用普通方式获取锁并递增计数器: " + counter.getValue());
                        // 模拟工作
                        Thread.sleep(10);
                    } finally {
                        nonFairLock.unlock();
                    }
                    
                    // 尝试获取锁（不阻塞）
                    if (nonFairLock.tryLock()) {
                        try {
                            counter.increment();
                            System.out.println("线程" + taskId + " 使用tryLock成功获取锁: " + counter.getValue());
                        } finally {
                            nonFairLock.unlock();
                        }
                    } else {
                        System.out.println("线程" + taskId + " 尝试获取锁失败");
                    }
                    
                    // 限时获取锁
                    if (nonFairLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            counter.increment();
                            System.out.println("线程" + taskId + " 使用限时tryLock成功获取锁: " + counter.getValue());
                        } finally {
                            nonFairLock.unlock();
                        }
                    } else {
                        System.out.println("线程" + taskId + " 限时尝试获取锁失败");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有任务完成
        latch.await();
        System.out.println("最终计数器值: " + counter.getValue());
        
        // 测试可中断锁
        System.out.println("\n测试可中断锁:");
        Thread t1 = new Thread(() -> {
            try {
                nonFairLock.lock();
                try {
                    System.out.println("线程t1获取锁");
                    // 持有锁5秒
                    Thread.sleep(5000);
                } finally {
                    nonFairLock.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("线程t1被中断");
            }
        });
        
        Thread t2 = new Thread(() -> {
            try {
                System.out.println("线程t2尝试获取可中断锁");
                nonFairLock.lockInterruptibly();
                try {
                    System.out.println("线程t2获取锁（不应该到达这里）");
                } finally {
                    nonFairLock.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("线程t2在等待锁时被中断");
            }
        });
        
        t1.start();
        Thread.sleep(100); // 确保t1先获取锁
        t2.start();
        Thread.sleep(1000); // 让t2等待一段时间
        
        // 中断t2
        System.out.println("中断线程t2");
        t2.interrupt();
        
        // 等待t1完成
        t1.join();
        t2.join();
        
        // 关闭线程池
        executor.shutdown();
        System.out.println();
    }
    
    /**
     * 读写锁示例
     * 演示ReentrantReadWriteLock的使用，允许多个线程同时读取，但只允许一个线程写入
     */
    private static void readWriteLockExample() throws InterruptedException {
        System.out.println("=== ReadWriteLock示例 ===");
        
        // 创建读写锁
        ReadWriteLock rwLock = new ReentrantReadWriteLock();
        // 获取读锁和写锁
        Lock readLock = rwLock.readLock();
        Lock writeLock = rwLock.writeLock();
        
        // 共享数据
        final CachedData cache = new CachedData();
        
        // 创建多个读线程
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch readLatch = new CountDownLatch(20);
        
        System.out.println("启动10个读线程，2个写线程");
        
        // 读线程
        for (int i = 0; i < 10; i++) {
            final int readerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 2; j++) {
                        // 获取读锁
                        readLock.lock();
                        try {
                            // 多个线程可以同时持有读锁
                            System.out.println("读线程-" + readerId + " 获取数据: " + cache.getData());
                            Thread.sleep(100);
                        } finally {
                            readLock.unlock();
                        }
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    readLatch.countDown();
                }
            });
        }
        
        // 写线程
        CountDownLatch writeLatch = new CountDownLatch(2);
        for (int i = 0; i < 2; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 2; j++) {
                        // 获取写锁
                        writeLock.lock();
                        try {
                            // 只有一个线程可以持有写锁，并且此时没有读锁
                            String newData = "Data-Writer" + writerId + "-" + j;
                            System.out.println("写线程-" + writerId + " 更新数据: " + newData);
                            cache.setData(newData);
                            Thread.sleep(500);
                        } finally {
                            writeLock.unlock();
                        }
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    writeLatch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        readLatch.await();
        writeLatch.await();
        System.out.println("最终数据: " + cache.getData());
        System.out.println();
    }
    
    /**
     * StampedLock示例
     * 演示StampedLock的使用，包括悲观读锁、写锁和乐观读
     */
    private static void stampedLockExample() throws InterruptedException {
        System.out.println("=== StampedLock示例 ===");
        
        // 创建StampedLock
        final StampedLock lock = new StampedLock();
        // 共享的点对象（用于演示坐标更新）
        final Point point = new Point(0, 0);
        
        // 写线程 - 移动点的位置
        Thread writerThread = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                // 获取写锁
                long stamp = lock.writeLock();
                try {
                    System.out.println("写线程获取写锁，准备移动点");
                    // 修改点的坐标
                    point.move(i + 1, i + 1);
                    System.out.println("点已移动到: " + point);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 释放写锁
                    lock.unlockWrite(stamp);
                    System.out.println("写线程释放写锁");
                }
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        // 悲观读线程 - 计算点到原点的距离
        Thread pessimisticReaderThread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                // 获取读锁
                long stamp = lock.readLock();
                try {
                    System.out.println("悲观读线程获取读锁");
                    // 读取点的坐标并计算距离
                    double distance = point.distanceFromOrigin();
                    System.out.println("悲观读: 点到原点的距离 = " + distance);
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 释放读锁
                    lock.unlockRead(stamp);
                    System.out.println("悲观读线程释放读锁");
                }
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        // 乐观读线程 - 计算点到原点的距离（可能需要重试）
        Thread optimisticReaderThread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                // 尝试乐观读
                long stamp = lock.tryOptimisticRead();
                // 读取点的坐标
                double x = point.getX();
                double y = point.getY();
                
                // 检查在读取过程中是否有写操作发生
                if (lock.validate(stamp)) {
                    // 没有写操作发生，可以安全使用读取的值
                    double distance = Math.sqrt(x * x + y * y);
                    System.out.println("乐观读成功: 点到原点的距离 = " + distance);
                } else {
                    // 发生了写操作，需要获取悲观读锁并重试
                    System.out.println("乐观读失败，切换到悲观读");
                    stamp = lock.readLock();
                    try {
                        double distance = point.distanceFromOrigin();
                        System.out.println("切换后的悲观读: 点到原点的距离 = " + distance);
                    } finally {
                        lock.unlockRead(stamp);
                    }
                }
                
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        // 启动所有线程
        writerThread.start();
        Thread.sleep(100);
        pessimisticReaderThread.start();
        optimisticReaderThread.start();
        
        // 等待所有线程完成
        writerThread.join();
        pessimisticReaderThread.join();
        optimisticReaderThread.join();
        System.out.println();
    }
    
    /**
     * Condition示例
     * 演示Condition的使用，实现线程间的等待/通知机制
     */
    private static void conditionExample() throws InterruptedException {
        System.out.println("=== Condition示例 ===");
        
        // 创建锁和条件
        final ReentrantLock lock = new ReentrantLock();
        final Condition notEmpty = lock.newCondition();
        final Condition notFull = lock.newCondition();
        
        // 创建有界缓冲区
        final BoundedBuffer<Integer> buffer = new BoundedBuffer<>(5);
        
        // 生产者线程
        Thread producerThread = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    lock.lock();
                    try {
                        // 缓冲区已满，等待notFull条件
                        while (buffer.isFull()) {
                            System.out.println("生产者: 缓冲区已满，等待空间...");
                            notFull.await();
                        }
                        
                        // 添加元素
                        buffer.add(i);
                        System.out.println("生产者: 添加元素 " + i);
                        
                        // 通知消费者有新元素可以消费
                        notEmpty.signal();
                    } finally {
                        lock.unlock();
                    }
                    
                    // 生产者速度稍慢
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // 消费者线程
        Thread consumerThread = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    lock.lock();
                    try {
                        // 缓冲区为空，等待notEmpty条件
                        while (buffer.isEmpty()) {
                            System.out.println("消费者: 缓冲区为空，等待数据...");
                            notEmpty.await();
                        }
                        
                        // 移除元素
                        int value = buffer.remove();
                        System.out.println("消费者: 移除元素 " + value);
                        
                        // 通知生产者有新空间可以生产
                        notFull.signal();
                    } finally {
                        lock.unlock();
                    }
                    
                    // 消费者速度较快
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // 启动线程
        producerThread.start();
        consumerThread.start();
        
        // 等待线程完成
        producerThread.join();
        consumerThread.join();
    }
    
    /**
     * 计数器类，用于ReentrantLock示例
     */
    private static class Counter {
        private int value = 0;
        
        public void increment() {
            value++;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * 缓存数据类，用于ReadWriteLock示例
     */
    private static class CachedData {
        private String data = "Initial Data";
        
        public String getData() {
            return data;
        }
        
        public void setData(String data) {
            this.data = data;
        }
    }
    
    /**
     * 点类，用于StampedLock示例
     */
    private static class Point {
        private double x, y;
        
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
        
        public void move(double deltaX, double deltaY) {
            x += deltaX;
            y += deltaY;
        }
        
        public double distanceFromOrigin() {
            return Math.sqrt(x * x + y * y);
        }
        
        @Override
        public String toString() {
            return "Point{" + "x=" + x + ", y=" + y + '}';
        }
    }
    
    /**
     * 有界缓冲区，用于Condition示例
     */
    private static class BoundedBuffer<E> {
        private final E[] items;
        private int putIndex, takeIndex, count;
        
        @SuppressWarnings("unchecked")
        public BoundedBuffer(int capacity) {
            items = (E[]) new Object[capacity];
        }
        
        public boolean isEmpty() {
            return count == 0;
        }
        
        public boolean isFull() {
            return count == items.length;
        }
        
        public void add(E item) {
            items[putIndex] = item;
            putIndex = (putIndex + 1) % items.length;
            count++;
        }
        
        public E remove() {
            E item = items[takeIndex];
            items[takeIndex] = null; // 帮助GC
            takeIndex = (takeIndex + 1) % items.length;
            count--;
            return item;
        }
    }
} 