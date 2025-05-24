import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池示例代码
 * 演示了不同类型线程池的创建和使用方法
 */
public class ThreadPoolExample {

    public static void main(String[] args) throws Exception {
        // 1. 固定大小线程池
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
        System.out.println("=== 固定线程池示例 ===");
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            fixedThreadPool.execute(() -> {
                String threadName = Thread.currentThread().getName();
                System.out.println(threadName + " 执行任务: " + taskId);
                try {
                    // 模拟任务执行
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        
        // 等待所有任务完成
        shutdownAndAwaitTermination(fixedThreadPool);
        
        // 2. 缓存线程池（线程数量不固定，按需创建）
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        System.out.println("\n=== 缓存线程池示例 ===");
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            cachedThreadPool.execute(() -> {
                String threadName = Thread.currentThread().getName();
                System.out.println(threadName + " 执行任务: " + taskId);
                try {
                    // 模拟任务执行
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        
        // 等待所有任务完成
        shutdownAndAwaitTermination(cachedThreadPool);
        
        // 3. 单线程线程池
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        System.out.println("\n=== 单线程池示例 ===");
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            singleThreadExecutor.execute(() -> {
                String threadName = Thread.currentThread().getName();
                System.out.println(threadName + " 执行任务: " + taskId);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        
        // 等待所有任务完成
        shutdownAndAwaitTermination(singleThreadExecutor);
        
        // 4. 定时任务线程池
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(3);
        System.out.println("\n=== 定时任务线程池示例 ===");
        
        // 延迟执行一次
        scheduledThreadPool.schedule(() -> {
            System.out.println(Thread.currentThread().getName() + " 延迟1秒执行");
        }, 1, TimeUnit.SECONDS);
        
        // 固定频率执行
        scheduledThreadPool.scheduleAtFixedRate(() -> {
            System.out.println(Thread.currentThread().getName() + " 固定频率执行");
        }, 1, 2, TimeUnit.SECONDS);
        
        // 固定延迟执行
        scheduledThreadPool.scheduleWithFixedDelay(() -> {
            System.out.println(Thread.currentThread().getName() + " 固定延迟执行");
        }, 1, 2, TimeUnit.SECONDS);
        
        // 让定时任务执行一段时间
        Thread.sleep(10000);
        
        // 关闭定时任务线程池
        shutdownAndAwaitTermination(scheduledThreadPool);
        
        // 5. 自定义线程池（推荐）
        System.out.println("\n=== 自定义线程池示例 ===");
        // 创建线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "自定义线程-" + threadNumber.getAndIncrement());
                if (t.isDaemon()) {
                    t.setDaemon(false);
                }
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            }
        };
        
        // 创建自定义线程池
        ThreadPoolExecutor customThreadPool = new ThreadPoolExecutor(
            2,                      // 核心线程数
            5,                      // 最大线程数
            60L,                    // 空闲线程存活时间
            TimeUnit.SECONDS,       // 时间单位
            new LinkedBlockingQueue<>(10), // 任务队列
            threadFactory,          // 线程工厂
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略: 调用者运行
        );
        
        // 提交20个任务，会触发拒绝策略
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            customThreadPool.execute(() -> {
                String threadName = Thread.currentThread().getName();
                System.out.println(threadName + " 执行任务: " + taskId);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        
        // 等待所有任务完成
        shutdownAndAwaitTermination(customThreadPool);
    }
    
    /**
     * 关闭线程池并等待所有任务完成
     */
    private static void shutdownAndAwaitTermination(ExecutorService pool) {
        // 禁止提交新任务
        pool.shutdown();
        try {
            // 等待现有任务完成
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                // 强制关闭
                pool.shutdownNow();
                // 等待线程响应中断
                if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("线程池没有正确终止");
                }
            }
        } catch (InterruptedException e) {
            // 如果当前线程被中断，重新取消
            pool.shutdownNow();
            // 保留中断状态
            Thread.currentThread().interrupt();
        }
    }
} 