import java.util.*;
import java.util.concurrent.*;

/**
 * 并发集合示例代码
 * 演示了JUC中常用的并发集合类的使用方法
 */
public class ConcurrentCollectionExample {

    public static void main(String[] args) throws InterruptedException {
        // 1. ConcurrentHashMap示例
        concurrentHashMapExample();
        
        // 2. CopyOnWriteArrayList示例
        copyOnWriteArrayListExample();
        
        // 3. ConcurrentLinkedQueue示例
        concurrentLinkedQueueExample();
        
        // 4. 阻塞队列示例
        blockingQueueExample();
    }
    
    /**
     * ConcurrentHashMap示例
     * 线程安全的HashMap实现，采用分段锁机制，提供较高的并发性能
     */
    private static void concurrentHashMapExample() throws InterruptedException {
        System.out.println("=== ConcurrentHashMap示例 ===");
        
        // 创建ConcurrentHashMap
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        // 创建多个线程并发操作Map
        CountDownLatch latch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            final int index = i;
            new Thread(() -> {
                // 向Map添加元素
                map.put("key" + index, index);
                
                // 使用putIfAbsent（仅在key不存在时才添加）
                map.putIfAbsent("sharedKey", index * 10);
                
                // 原子更新操作
                map.compute("counter", (k, v) -> v == null ? 1 : v + 1);
                
                // 原子条件更新
                map.computeIfPresent("key" + (index % 5), (k, v) -> v + 100);
                
                System.out.println(Thread.currentThread().getName() + " 完成操作");
                latch.countDown();
            }).start();
        }
        
        // 等待所有线程完成
        latch.await();
        
        // 打印结果
        System.out.println("Map大小: " + map.size());
        System.out.println("counter值: " + map.get("counter"));
        System.out.println("sharedKey值: " + map.get("sharedKey"));
        System.out.println("Map内容: " + map);
        System.out.println();
    }
    
    /**
     * CopyOnWriteArrayList示例
     * 适用于读多写少的场景，在写操作时复制整个数组
     */
    private static void copyOnWriteArrayListExample() throws InterruptedException {
        System.out.println("=== CopyOnWriteArrayList示例 ===");
        
        // 创建普通ArrayList（非线程安全）与CopyOnWriteArrayList（线程安全）
        List<String> normalList = new ArrayList<>();
        List<String> concurrentList = new CopyOnWriteArrayList<>();
        
        // 创建多线程同时修改两种List
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(20);
        
        // 向两个List添加元素的任务
        Runnable normalListTask = () -> {
            try {
                for (int i = 0; i < 100; i++) {
                    normalList.add("item" + i);
                }
            } catch (Exception e) {
                System.out.println("普通ArrayList抛出异常: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        };
        
        Runnable concurrentListTask = () -> {
            for (int i = 0; i < 100; i++) {
                concurrentList.add("item" + i);
            }
            latch.countDown();
        };
        
        // 提交任务
        for (int i = 0; i < 10; i++) {
            executor.submit(normalListTask);
            executor.submit(concurrentListTask);
        }
        
        // 等待所有任务完成
        latch.await();
        executor.shutdown();
        
        // 打印结果
        System.out.println("普通ArrayList大小（可能发生并发异常）: " + normalList.size());
        System.out.println("CopyOnWriteArrayList大小: " + concurrentList.size());
        
        // 展示CopyOnWriteArrayList的迭代器快照特性
        System.out.println("\n演示CopyOnWriteArrayList的迭代器快照特性:");
        concurrentList.clear();
        for (int i = 0; i < 10; i++) {
            concurrentList.add("元素" + i);
        }
        
        // 获取迭代器
        Iterator<String> iterator = concurrentList.iterator();
        
        // 修改List
        concurrentList.add("新元素");
        concurrentList.remove(0);
        
        // 迭代器遍历的仍然是旧数据
        System.out.println("迭代器遍历结果（不包含修改后的内容）:");
        while (iterator.hasNext()) {
            System.out.print(iterator.next() + " ");
        }
        
        System.out.println("\n当前List内容: " + concurrentList);
        System.out.println();
    }
    
    /**
     * ConcurrentLinkedQueue示例
     * 线程安全的非阻塞队列，使用CAS操作实现
     */
    private static void concurrentLinkedQueueExample() throws InterruptedException {
        System.out.println("=== ConcurrentLinkedQueue示例 ===");
        
        // 创建ConcurrentLinkedQueue
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        
        // 创建多个生产者线程
        CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            final int producerId = i;
            new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    queue.offer("Producer" + producerId + "-Item" + j);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                latch.countDown();
            }).start();
        }
        
        // 创建多个消费者线程
        for (int i = 0; i < 2; i++) {
            final int consumerId = i;
            new Thread(() -> {
                while (true) {
                    String item = queue.poll();
                    if (item != null) {
                        System.out.println("Consumer" + consumerId + " 获取: " + item);
                    } else if (latch.getCount() == 0 && queue.isEmpty()) {
                        // 所有生产者完成且队列为空，退出循环
                        break;
                    }
                    
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("Consumer" + consumerId + " 完成");
            }).start();
        }
        
        // 等待所有生产者完成
        latch.await();
        
        // 等待消费者处理完成
        Thread.sleep(1000);
        System.out.println("队列最终大小: " + queue.size());
        System.out.println();
    }
    
    /**
     * 阻塞队列示例
     * 演示各种阻塞队列的特性
     */
    private static void blockingQueueExample() throws InterruptedException {
        System.out.println("=== 阻塞队列示例 ===");
        
        // 1. ArrayBlockingQueue - 固定容量的阻塞队列
        ArrayBlockingQueue<String> arrayQueue = new ArrayBlockingQueue<>(3);
        System.out.println("ArrayBlockingQueue（容量为3）:");
        
        // 添加元素
        arrayQueue.put("Item1");
        arrayQueue.put("Item2");
        arrayQueue.put("Item3");
        System.out.println("队列已满，大小: " + arrayQueue.size());
        
        // 使用offer尝试添加元素（非阻塞）
        boolean added = arrayQueue.offer("Item4");
        System.out.println("尝试添加Item4: " + (added ? "成功" : "失败"));
        
        // 使用offer尝试添加元素（超时方式）
        added = arrayQueue.offer("Item4", 1, TimeUnit.SECONDS);
        System.out.println("尝试添加Item4（等待1秒）: " + (added ? "成功" : "失败"));
        
        // 获取并移除元素
        String item = arrayQueue.take();
        System.out.println("获取元素: " + item + ", 剩余大小: " + arrayQueue.size());
        
        // 现在队列未满，可以添加元素
        arrayQueue.put("Item4");
        System.out.println("添加Item4后的队列: " + arrayQueue);
        
        // 2. DelayQueue - 延迟队列
        System.out.println("\nDelayQueue（延迟队列）:");
        DelayQueue<DelayedElement> delayQueue = new DelayQueue<>();
        
        // 添加延迟元素
        delayQueue.put(new DelayedElement("Task1", 2));
        delayQueue.put(new DelayedElement("Task2", 1));
        delayQueue.put(new DelayedElement("Task3", 3));
        
        System.out.println("延迟队列大小: " + delayQueue.size());
        
        // 获取延迟元素（按延迟时间排序）
        System.out.println("获取延迟元素（会按延迟时间顺序返回）:");
        for (int i = 0; i < 3; i++) {
            DelayedElement delayedItem = delayQueue.take();
            System.out.println("获取元素: " + delayedItem.getName());
        }
        
        // 3. SynchronousQueue - 没有容量的队列
        System.out.println("\nSynchronousQueue（同步队列）:");
        SynchronousQueue<String> synchronousQueue = new SynchronousQueue<>();
        
        // 启动消费者线程
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 先等待一秒
                System.out.println("消费者准备接收元素");
                String element = synchronousQueue.take();
                System.out.println("消费者接收到: " + element);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // 生产者线程
        System.out.println("生产者准备发送元素（会阻塞直到有消费者接收）");
        synchronousQueue.put("SyncItem"); // 会阻塞直到有消费者接收
        System.out.println("生产者已发送元素");
    }
    
    /**
     * 延迟队列元素
     */
    private static class DelayedElement implements Delayed {
        private final String name;
        private final long endTime;
        
        public DelayedElement(String name, long delaySeconds) {
            this.name = name;
            this.endTime = System.currentTimeMillis() + delaySeconds * 1000;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public long getDelay(TimeUnit unit) {
            long remainingTime = endTime - System.currentTimeMillis();
            return unit.convert(remainingTime, TimeUnit.MILLISECONDS);
        }
        
        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }
            long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(diff, 0);
        }
    }
} 