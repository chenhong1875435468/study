import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * CompletableFuture异步编程示例
 * 演示CompletableFuture的各种操作和用法
 */
public class CompletableFutureExample {

    public static void main(String[] args) throws Exception {
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(5);

        try {
            // 1. 基本用法示例
            basicExample();
            
            // 2. 转换和组合示例
            transformAndCombineExample();
            
            // 3. 异常处理示例
            exceptionHandlingExample();
            
            // 4. 超时处理示例
            timeoutExample();
            
            // 5. 实际应用示例 - 模拟电商下单流程
            ecommerceOrderExample(executor);
        } finally {
            // 关闭线程池
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 基本用法示例
     */
    private static void basicExample() throws Exception {
        System.out.println("=== CompletableFuture基本用法示例 ===");
        
        // 1. 创建已完成的CompletableFuture
        CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("已完成的结果");
        System.out.println("1. 已完成的CompletableFuture结果: " + completedFuture.get());
        
        // 2. 运行异步任务（无返回值）
        CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
            sleep(100);
            System.out.println("2. runAsync: 在" + Thread.currentThread().getName() + "中执行");
        });
        
        // 等待无返回值的任务完成
        runAsync.join();
        
        // 3. 运行异步任务（有返回值）
        CompletableFuture<String> supplyAsync = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            System.out.println("3. supplyAsync: 在" + Thread.currentThread().getName() + "中执行");
            return "异步任务的结果";
        });
        
        // 获取结果（阻塞等待）
        String result = supplyAsync.get();
        System.out.println("   supplyAsync结果: " + result);
        
        // 4. 使用自定义线程池
        ExecutorService myExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "自定义线程");
            return t;
        });
        
        CompletableFuture<String> withExecutor = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            System.out.println("4. 使用自定义线程池: 在" + Thread.currentThread().getName() + "中执行");
            return "自定义线程池的结果";
        }, myExecutor);
        
        System.out.println("   自定义线程池结果: " + withExecutor.get());
        
        // 关闭自定义线程池
        myExecutor.shutdown();
        System.out.println();
    }
    
    /**
     * 转换和组合示例
     */
    private static void transformAndCombineExample() throws Exception {
        System.out.println("=== CompletableFuture转换和组合示例 ===");
        
        // 1. 使用thenApply转换结果
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "Hello";
        });
        
        CompletableFuture<Integer> lengthFuture = future1.thenApply(s -> {
            System.out.println("1. thenApply: 将字符串转换为长度");
            return s.length();
        });
        
        System.out.println("   转换结果: " + lengthFuture.get());
        
        // 2. 使用thenAccept消费结果
        CompletableFuture<Void> consumeFuture = future1.thenAccept(s -> {
            System.out.println("2. thenAccept: 消费结果 = " + s);
        });
        
        // 等待消费操作完成
        consumeFuture.get();
        
        // 3. 使用thenRun在前一个任务完成后执行操作（不使用结果）
        CompletableFuture<Void> runFuture = future1.thenRun(() -> {
            System.out.println("3. thenRun: 前一个任务完成后执行，不使用结果");
        });
        
        // 等待操作完成
        runFuture.get();
        
        // 4. 使用thenCompose组合两个有依赖关系的CompletableFuture
        CompletableFuture<String> composedFuture = future1.thenCompose(s -> {
            // 返回一个新的CompletableFuture
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("4. thenCompose: 使用上一个结果: " + s);
                return s + " World";
            });
        });
        
        System.out.println("   组合结果: " + composedFuture.get());
        
        // 5. 使用thenCombine组合两个独立的CompletableFuture
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "Hello";
        });
        
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "World";
        });
        
        CompletableFuture<String> combinedFuture = future2.thenCombine(future3, (s1, s2) -> {
            System.out.println("5. thenCombine: 组合两个独立结果: " + s1 + " 和 " + s2);
            return s1 + " " + s2;
        });
        
        System.out.println("   组合结果: " + combinedFuture.get());
        
        // 6. 使用allOf等待多个CompletableFuture都完成
        CompletableFuture<String> future4 = CompletableFuture.supplyAsync(() -> {
            sleep(50);
            System.out.println("6. allOf: 任务1完成");
            return "结果1";
        });
        
        CompletableFuture<String> future5 = CompletableFuture.supplyAsync(() -> {
            sleep(150);
            System.out.println("6. allOf: 任务2完成");
            return "结果2";
        });
        
        CompletableFuture<String> future6 = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            System.out.println("6. allOf: 任务3完成");
            return "结果3";
        });
        
        CompletableFuture<Void> allFuture = CompletableFuture.allOf(future4, future5, future6);
        
        // 等待所有任务完成
        allFuture.get();
        System.out.println("   所有任务都已完成");
        
        // 获取所有结果
        List<String> results = Arrays.asList(future4, future5, future6)
                .stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        
        System.out.println("   所有结果: " + results);
        
        // 7. 使用anyOf等待任意一个CompletableFuture完成
        CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> {
                    sleep(200);
                    System.out.println("7. anyOf: 慢任务完成");
                    return "慢任务";
                }),
                CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    System.out.println("7. anyOf: 中速任务完成");
                    return "中速任务";
                }),
                CompletableFuture.supplyAsync(() -> {
                    sleep(50);
                    System.out.println("7. anyOf: 快任务完成");
                    return "快任务";
                })
        );
        
        // 获取最先完成的任务结果
        System.out.println("   最先完成的任务: " + anyFuture.get());
        System.out.println();
    }
    
    /**
     * 异常处理示例
     */
    private static void exceptionHandlingExample() throws Exception {
        System.out.println("=== CompletableFuture异常处理示例 ===");
        
        // 1. 使用exceptionally处理异常
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            if (Math.random() < 0.7) {
                throw new RuntimeException("模拟异常");
            }
            return "正常结果";
        }).exceptionally(ex -> {
            System.out.println("1. exceptionally: 捕获异常 - " + ex.getMessage());
            return "异常后的默认值";
        });
        
        System.out.println("   结果: " + future1.get());
        
        // 2. 使用handle同时处理正常结果和异常
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            if (Math.random() < 0.7) {
                throw new RuntimeException("模拟异常");
            }
            return "正常结果";
        }).handle((result, ex) -> {
            if (ex != null) {
                System.out.println("2. handle: 捕获异常 - " + ex.getMessage());
                return "异常后的默认值";
            } else {
                System.out.println("2. handle: 处理正常结果 - " + result);
                return result.toUpperCase();
            }
        });
        
        System.out.println("   结果: " + future2.get());
        
        // 3. 使用whenComplete查看结果或异常，但不修改结果
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            if (Math.random() < 0.3) {
                throw new RuntimeException("模拟异常");
            }
            return "正常结果";
        }).whenComplete((result, ex) -> {
            if (ex != null) {
                System.out.println("3. whenComplete: 发现异常 - " + ex.getMessage());
            } else {
                System.out.println("3. whenComplete: 发现正常结果 - " + result);
            }
        }).exceptionally(ex -> {
            // 在whenComplete后仍然需要exceptionally来处理异常
            return "异常后的默认值";
        });
        
        System.out.println("   结果: " + future3.get());
        System.out.println();
    }
    
    /**
     * 超时处理示例
     */
    private static void timeoutExample() throws Exception {
        System.out.println("=== CompletableFuture超时处理示例 ===");
        
        // 创建一个可能超时的任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            // 随机任务执行时间
            int sleepTime = new Random().nextInt(1500) + 500; // 500ms到2000ms
            System.out.println("任务将运行 " + sleepTime + " 毫秒");
            sleep(sleepTime);
            return "任务完成，耗时 " + sleepTime + " 毫秒";
        });
        
        // 使用orTimeout方法设置超时
        // 注意：这是Java 9引入的方法，Java 8中需要使用其他方式实现超时
        try {
            CompletableFuture<String> futureWithTimeout = future.orTimeout(1000, TimeUnit.MILLISECONDS);
            String result = futureWithTimeout.get();
            System.out.println("任务在超时前完成: " + result);
        } catch (Exception e) {
            System.out.println("任务超时: " + e.getMessage());
        }
        
        // 使用completeOnTimeout提供默认值
        // 注意：这是Java 9引入的方法，Java 8中需要使用其他方式实现超时
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            int sleepTime = new Random().nextInt(1500) + 500; // 500ms到2000ms
            System.out.println("第二个任务将运行 " + sleepTime + " 毫秒");
            sleep(sleepTime);
            return "第二个任务完成，耗时 " + sleepTime + " 毫秒";
        });
        
        try {
            CompletableFuture<String> futureWithTimeout = future2.completeOnTimeout("超时默认值", 1000, TimeUnit.MILLISECONDS);
            String result = futureWithTimeout.get();
            System.out.println("结果 (可能是默认值): " + result);
        } catch (Exception e) {
            System.out.println("出现意外异常: " + e.getMessage());
        }
        
        // Java 8中实现超时的方式
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            int sleepTime = new Random().nextInt(1500) + 500; // 500ms到2000ms
            System.out.println("第三个任务将运行 " + sleepTime + " 毫秒");
            sleep(sleepTime);
            return "第三个任务完成，耗时 " + sleepTime + " 毫秒";
        });
        
        // 创建一个延迟完成的future
        CompletableFuture<String> timeout = new CompletableFuture<>();
        Executors.newScheduledThreadPool(1).schedule(
                () -> timeout.complete("超时了"),
                1000,
                TimeUnit.MILLISECONDS
        );
        
        // 使用applyToEither方法，哪个先完成就使用哪个的结果
        CompletableFuture<String> result = future3.applyToEither(timeout, s -> s);
        System.out.println("结果: " + result.get());
        System.out.println();
    }
    
    /**
     * 电商下单流程示例
     * 模拟一个电商下单的异步处理流程
     */
    private static void ecommerceOrderExample(ExecutorService executor) throws Exception {
        System.out.println("=== 电商下单流程示例 ===");
        
        // 模拟用户ID和商品ID
        long userId = 12345;
        long productId = 9876;
        
        // 1. 检查用户信息（异步）
        CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("检查用户信息...");
            sleep(200);
            return new User(userId, "张三", true);
        }, executor);
        
        // 2. 检查商品信息（异步）
        CompletableFuture<Product> productFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("检查商品信息...");
            sleep(300);
            return new Product(productId, "iPhone 13", 6799.00, 10);
        }, executor);
        
        // 3. 检查库存信息（依赖于商品信息）
        CompletableFuture<Boolean> inventoryFuture = productFuture.thenComposeAsync(product -> {
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("检查库存...");
                sleep(150);
                return product.getStock() > 0;
            }, executor);
        });
        
        // 4. 检查用户余额（依赖于用户信息）
        CompletableFuture<Double> balanceFuture = userFuture.thenComposeAsync(user -> {
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("查询用户余额...");
                sleep(250);
                return 10000.0; // 假设用户余额
            }, executor);
        });
        
        // 5. 组合商品信息和余额，检查是否能购买
        CompletableFuture<Boolean> canPurchaseFuture = productFuture.thenCombineAsync(balanceFuture, (product, balance) -> {
            System.out.println("检查是否能购买...");
            return balance >= product.getPrice();
        }, executor);
        
        // 6. 组合所有检查结果，确定是否可以下单
        CompletableFuture<Boolean> canOrderFuture = CompletableFuture.allOf(userFuture, inventoryFuture, canPurchaseFuture)
                .thenApplyAsync(v -> {
                    boolean validUser = userFuture.join().isActive();
                    boolean hasInventory = inventoryFuture.join();
                    boolean canPurchase = canPurchaseFuture.join();
                    
                    System.out.println("综合评估订单条件:");
                    System.out.println("- 用户状态正常: " + validUser);
                    System.out.println("- 有库存: " + hasInventory);
                    System.out.println("- 余额充足: " + canPurchase);
                    
                    return validUser && hasInventory && canPurchase;
                }, executor);
        
        // 7. 创建订单
        CompletableFuture<Order> orderFuture = canOrderFuture.thenComposeAsync(canOrder -> {
            if (canOrder) {
                return CompletableFuture.supplyAsync(() -> {
                    System.out.println("创建订单...");
                    sleep(300);
                    return new Order(System.currentTimeMillis(), userId, productId, 1);
                }, executor);
            } else {
                // 如果不满足下单条件，抛出异常
                CompletableFuture<Order> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("不满足下单条件"));
                return future;
            }
        });
        
        // 8. 处理支付
        CompletableFuture<String> paymentFuture = orderFuture.thenComposeAsync(order -> {
            return productFuture.thenComposeAsync(product -> {
                return CompletableFuture.supplyAsync(() -> {
                    System.out.println("处理支付...");
                    sleep(400);
                    return "订单 " + order.getOrderId() + " 支付成功，金额: " + product.getPrice();
                }, executor);
            });
        }).exceptionally(ex -> {
            return "下单失败: " + ex.getMessage();
        });
        
        // 9. 发送订单确认
        CompletableFuture<String> confirmationFuture = paymentFuture.thenApplyAsync(result -> {
            System.out.println("发送订单确认...");
            sleep(100);
            return result + "\n订单确认已发送";
        }, executor);
        
        // 获取最终结果
        String finalResult = confirmationFuture.get();
        System.out.println("\n最终结果:");
        System.out.println(finalResult);
    }
    
    /**
     * 休眠指定的毫秒数
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 用户类
     */
    private static class User {
        private final long id;
        private final String name;
        private final boolean active;
        
        public User(long id, String name, boolean active) {
            this.id = id;
            this.name = name;
            this.active = active;
        }
        
        public long getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isActive() {
            return active;
        }
    }
    
    /**
     * 商品类
     */
    private static class Product {
        private final long id;
        private final String name;
        private final double price;
        private final int stock;
        
        public Product(long id, String name, double price, int stock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.stock = stock;
        }
        
        public long getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public double getPrice() {
            return price;
        }
        
        public int getStock() {
            return stock;
        }
    }
    
    /**
     * 订单类
     */
    private static class Order {
        private final long orderId;
        private final long userId;
        private final long productId;
        private final int quantity;
        
        public Order(long orderId, long userId, long productId, int quantity) {
            this.orderId = orderId;
            this.userId = userId;
            this.productId = productId;
            this.quantity = quantity;
        }
        
        public long getOrderId() {
            return orderId;
        }
        
        public long getUserId() {
            return userId;
        }
        
        public long getProductId() {
            return productId;
        }
        
        public int getQuantity() {
            return quantity;
        }
    }
} 