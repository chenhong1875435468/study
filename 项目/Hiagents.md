- 项目简要介绍
- HiAgent 是火山引擎推出的企业 AI 中台，旨在帮助企业实现智能体的快速、高效交付，助力企业数字化转型
- HiAgent是火山引擎推出的企业AI中台，基于Agent DevOps理念，为企业提供智能体开发、评测、观测、优化的全生命周期管理，助力企业构建生产级高价值智能体，实现从模型到应用的全链路打通。

1. 功能特点
   - 低代码快速搭建智能体：提供100 +插件、MCP（模型压缩与加速技术）以及100 +企业场景智能体模板。企业可基于提示词、插件、MCP、知识库、工作流等组件，像“搭积木”一样快速搭建智能体。并且支持发布到飞书、钉钉、微信等IM渠道，也能通过API、WebSDK形式与企业存量系统集成 ，便于融入企业现有办公和业务流程。
   - 智能体和大模型全链路运维运营：评测系统从多维度定量评估智能体和大模型的表现效果；观测系统实时监控智能体性能、效果及用户反馈，积累高质量生产数据；数据工程系统助力生成优质数据，为评测集与训练集的构建提供支撑，形成一个完整的、闭环的运维运营体系，保障智能体和大模型持续优化。
   - 一站式模型训练和推理：支持各类三方模型接入，企业可以对比不同模型性能，按需切换使用。还可托管企业自有算力进行模型推理和训练，同时提供一站式模型推理、数据、精调服务，帮助企业低成本、低门槛接入并部署各类模型，训练企业专属模型。
   - 全生命周期安全防护：严格遵循数据不出域原则，确保企业数据隐私安全；审计日志可追溯，便于对操作和数据流向进行监管；大模型防火墙能有效阻断攻击，全方位保障系统安全合规，满足企业在数据安全和合规方面的严格要求。
   - 企业级服务：支持私有化部署，为企业提供丰富的配套管理功能和灵活的开放集成能力。凭借丰富的行业实践经验，能提供端到端AI咨询服务，覆盖200 +行业场景，已成功落地Agent 500 +款，帮助企业更好地实现AI转型。
2. 应用场景：在多个领域有着广泛的应用，如智能营销助手，可进行营销素材生成、产品卖点提炼、客群精准匹配；智慧学伴，作为学生的个性化学习伙伴，答疑解惑、推荐学习内容；HR助手，自动化处理招聘、考勤、员工咨询等事务性工作；智能客服，处理产品推荐、订单跟踪、投诉处理等常见问题，提升产品全链路服务体验等。
3. 行业案例
   - 浙江大学：携手火山引擎，依托HiAgent平台，7天高效落地“浙大先生”大模型应用体系，为5万 +在校师生打造智能化的教学教务、科研创新、校园生活体验。
   - 爱玛科技：通过HiAgent平台构建“AI智慧大脑”，打造四类AI场景，为B端、C端超40,000人提供服务，办公效率提升300%、跨国协作提效50%，员工服务首次联系解决率达90%。

- 
- 项目亮点
- 
- 
- 由于生成的代码太多了，thrift，mapstruct，最后导致出问题，OOM
- 
- 项目中的困难点
  - 环境问题
    - 网关 Janus 相关内容，网关上没有更新 idl，导致输出被网关过滤掉了
    - TLB 问题，内容分发 OpenAPI 接口没有配置接口，导致接口鉴权调不通
    - CDN 问题，回流的时候超时，默认是 30s，但是由于个别 Agent 用户的配置非常复杂，最终导致 Agent 执行结果返回给前端的时候，在 CDN 的地方超时了，出现了 504 的 Error，这种情况下没有 logId，问题不好复现，也不好排查，当时拉了非常多的其他部门的同学来解决
    - 
  - 代码逻辑问题
  - 问题排查相关
  - 具体问题
    - 在写联网搜索的接口的时候，发现异步的方式，查不到异步方法里面的日志
    - 项目由于 MapStruct 和 Thrift 生成的代码内容过多，项目过大，导致直接需要调整最大堆大小
    - 项目的 JDK 版本是 JDK17
- 内部流程
- 做过的需求：OpenAPI，Agent 联网搜索，RAG
- 做了哪些优化点
  - 关于联网搜索的时候，搜索时间过慢，已经在 20-30 秒左右，导致用户体验非常差
    - 从 30s 左右，优化到了 10s，并且提供了流式方式进行输出，大大改善体验
    - 改善：联网搜索调用服务和 RAG 服务进行异步，使用 CompletableFuture 进行操作
      - 进一步优化：线程池的方式
- 
- 
- 关于令牌桶限流
  - 用在了模型限流上面，防止有些模型的访问过多
  - package com.bytedance.aiagent.infra;

import com.bytedance.ies.cs.knowledge.common.redis.RedisClient;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**

* 实现基于令牌桶算法的redis分布式限流器
* 
* @author fengyang
* @date 2024/4/1
  */
  @Slf4j
  public class RedisRateLimiter {

  private static final String RATE_LIMITER_PREFIX = "dist_rate_limiter_v1:";
  private static final String ACQUIRE_SCRIPT = """
  local rate = redis.call('hget', KEYS[1], 'rate');
  local interval = redis.call('hget', KEYS[1], 'interval');
  local valueKey = KEYS[2];
  local permitsKey = KEYS[3];
  local currentValue = redis.call('get', valueKey);
  local res;
  local currentTimeMillis = tonumber(ARGV[2]);
  if currentValue ~= false then
  local expiredValues = redis.call('zrangebyscore', permitsKey, 0, tonumber(ARGV[2]) - interval);
  local releasedCount = 0;
  for i,expiredPermit in ipairs(expiredValues) do
  local permitId, permitCount = struct.unpack('Bc0I', expiredPermit);
  releasedCount = releasedCount + permitCount;
  end;

  if releasedCount > 0 then
  redis.call('zremrangebyscore', permitsKey, 0,  currentTimeMillis - interval);
  if tonumber(currentValue) + releasedCount > tonumber(rate) then
  currentValue = tonumber(rate) - redis.call('zcard', permitsKey);
  else
  currentValue = tonumber(currentValue) + releasedCount;
  end;
  redis.call('set', valueKey, currentValue)
  end;

  if tonumber(currentValue) < tonumber(ARGV[1]) then
  local oldestPermit = redis.call('zrange', permitsKey, 0, 0, 'withscores');
  local permitCreateTimeMillis = tonumber(oldestPermit[2]);
  res = 3 + interval - (currentTimeMillis - permitCreateTimeMillis);
  else
  redis.call('zadd', permitsKey, ARGV[2], struct.pack('Bc0I', string.len(ARGV[3]), ARGV[3], ARGV[1]));
  redis.call('decrby', valueKey, ARGV[1]);
  res = nil;
  end;
  else
  redis.call('set', valueKey, rate);
  redis.call('zadd', permitsKey, ARGV[2], struct.pack('Bc0I', string.len(ARGV[3]), ARGV[3], ARGV[1]));
  redis.call('decrby', valueKey, ARGV[1]);
  res = nil;
  end;

  local expireMillis = tonumber(interval) + 60000;
  redis.call('pexpire', KEYS[1], expireMillis);
  redis.call('pexpire', permitsKey, expireMillis);
  redis.call('pexpire', valueKey, expireMillis);

  return res;
  """;
  private static final String INIT_SCRIPT = """
  local oldRate = redis.call('hget', KEYS[1], 'rate');
  redis.call('hset', KEYS[1], 'rate', ARGV[1]);
  if oldRate ~= false and redis.call('get', KEYS[2]) ~= false then
  redis.call('incrby', KEYS[2], ARGV[1] - tonumber(oldRate))
  end;
  redis.call('pexpire', KEYS[1], tonumber(ARGV[2]) + 60000)
  return redis.call('hset', KEYS[1], 'interval', ARGV[2]);
  """;

  private final static HashedWheelTimer TIMER;

  static {
  ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("redis-rate-limiter-timer-%s").build();
  TIMER = new HashedWheelTimer(threadFactory, 1, TimeUnit.MILLISECONDS);
  }

  private RedisClient redisClient;

  private String name;

  private volatile Long rate;

  public RedisRateLimiter(RedisClient client, String name, Long rate, Long interval, TimeUnit intervalUnit) {
  this.redisClient = client;
  this.name = name;
  this.updateRate(rate, interval, intervalUnit);
  }

  public void updateRate(Long rate, Long interval, TimeUnit intervalUnit) {
  this.rate = rate;
  String rateKey = getRateLimiterName();
  String valueKey = getRateLimiterValueName();
  long intervalMillis = intervalUnit.toMillis(interval);
  redisClient.execute(j -> j.eval(INIT_SCRIPT, Lists.newArrayList(rateKey, valueKey), Lists.newArrayList(String.valueOf(rate), String.valueOf(intervalMillis))));
  }

  public boolean tryAcquire() {
  return tryAcquire(1);
  }

  public boolean tryAcquire(int permits) {
  return tryAcquire(permits, 0L).join();
  }

  public boolean tryAcquire(long timeout, TimeUnit unit) {
  return tryAcquire(1, timeout, unit);
  }

  public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
  return tryAcquire(permits, unit.toMillis(timeout)).join();
  }

  public CompletableFuture `<Boolean>` tryAcquire(int permits, long timeoutInMillis) {
  if (permits > rate) {
  return CompletableFuture.failedFuture(new RuntimeException("request permits amount could not exceed defined rate"));
  }

  long start = System.currentTimeMillis();
  String permitId = UUID.randomUUID().toString();
  Long waitMillis = redisClient.execute(j -> {
  return (Long) j.eval(ACQUIRE_SCRIPT, Lists.newArrayList(getRateLimiterName(), getRateLimiterValueName(), getRateLimiterPermitName()),
  Lists.newArrayList(String.valueOf(permits), String.valueOf(start), permitId));
  });
  if (waitMillis == null) {
  return CompletableFuture.completedFuture(true);
  }
  if (timeoutInMillis == -1) {
  // 一直等
  CompletableFuture `<Boolean>` res = new CompletableFuture<>();
  TIMER.newTimeout(timeout -> tryAcquire(permits, timeoutInMillis).whenComplete((r, e) -> {
  if (e != null) {
  res.completeExceptionally(e);
  } else {
  res.complete(r);
  }
  }), waitMillis, TimeUnit.MILLISECONDS);
  return res;
  }

  long elapsed = System.currentTimeMillis() - start;
  long remainMillis = timeoutInMillis - elapsed;
  if (remainMillis <= waitMillis) {
  log.info("rate limited,  remainMillis:{}, waitMillis:{}", remainMillis, waitMillis);
  return CompletableFuture.completedFuture(false);
  }

  CompletableFuture `<Boolean>` res = new CompletableFuture<>();
  long startMillis = System.currentTimeMillis();
  TIMER.newTimeout(t -> {
  long elapsedMillis = System.currentTimeMillis() - startMillis;
  if (remainMillis <= elapsedMillis) {
  res.complete(false);
  return;
  }
  tryAcquire(permits, remainMillis - elapsedMillis).whenComplete((r, e) -> {
  if (e != null) {
  res.completeExceptionally(e);
  } else {
  res.complete(r);
  }
  });
  }, waitMillis, TimeUnit.MILLISECONDS);

  return res;
  }

  private String getRateLimiterPermitName() {
  return "%s{%s}:permit".formatted(RATE_LIMITER_PREFIX, name);
  }

  private String getRateLimiterValueName() {
  return "%s{%s}:value".formatted(RATE_LIMITER_PREFIX, name);
  }

  private String getRateLimiterName() {
  return "%s{%s}".formatted(RATE_LIMITER_PREFIX, name);
  }
  }

- 
- 项目中用消息队列干什么了
- 
- 
- 项目 debug 接口调用图
  ---------------------
- 

* OpenAPI
  * 有一个需求，需要 Agent 的调用日志明细，用来进行数据分析，然后我们提供两种方案，一种是数据表的方式，提供 Mysql 和 MongoDB 给他们，但是由于字节云和火山云不互通，他们没办法兼容，然后考虑通过 OpenAPI的方式，提供 HTTP 接口给他们，然后走鉴权逻辑，接着提取数据，但是出现了其他问题，由于读数据是从 MongoDB 读的，而且有一部分比较热门的 Agent 调用日志量巨大，开发完成后，发现数据量很大的经常由于超时等原因，调用失败，需要对这一块进行优化
    * 日志量过大迁移问题
    * mongodb大数据量问题，打算迁移elastic search
      * 发现 mongoDB 翻页到 10000 条左右，性能很差，经常因为超时导致请求失败
      * 考虑改成 es，但是 es 实际上也是治标不治本，数据量大的情况下，如果用户一次请求所有调用记录，那么还是会因为数据量大的情况失败
      * 但是只能一步步改善，首先调整 mongoDB 连接时间，调到两分钟，但是也还是超时了，后面迁移到了 ES，出现了明显改善
  * AK SK鉴权问题，解析得到租户 ID
  * 504 问题排查 CDN 回流超时，当时没有 logId，也没有任何信息，排查很困难
    * Oncall 拉了很多部门的人，当时问题比较紧急，首先是 CDN > TLB > Janus > psm，结果请求在 CDN 的位置超时了
    * TLB 拿到的 logid
* 联网搜索
  * 联网搜索优化方案与实践总结
    一、核心问题定位
    在实现基于火山方舟搜索能力的联网搜索功能时，发现 **单次调用耗时长达20-30秒** ，且存在以下痛点：

    - **用户体验层面** ：无状态提示，用户无法感知搜索进度
    - **性能层面** ：
    - 火山方舟客户端重复创建导致资源浪费
    - 同步调用阻塞线程，多用户场景下响应延迟叠加
    - 异步日志无法追踪，故障排查困难二、优化策略与技术实现
      （一）用户体验优化：前置状态反馈
    - **方案** ：
    - 后端新增 supportWebSearch 标志位，标识联网搜索状态
    - 前端监听标志位，实时展示「联网搜索中...」loading提示
    - **效果** ：明确用户预期，减少焦虑感

    （二）性能优化：全链路效率提升

    1. **客户端资源复用：缓存火山方舟实例**

    - **实现** ：
    - 创建单例模式的ArkServiceCache缓存层
    - 首次调用时初始化火山方舟客户端，后续请求直接从缓存获取
    - **收益** ：消除客户端创建开销，单次调用基础耗时降低约3秒

    2. **异步化改造：CompletableFuture + 自定义线程池**

    - **核心逻辑** ：

    ```java
    // 改造前：同步阻塞调用
      String result = arkClient.search(query);

      // 改造后：异步非阻塞调用
      CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> 
          arkClient.search(query), customThreadPool
      );
    ```

    - **线程池优化** ：
    - 使用ThreadPoolExecutor自定义线程池，配置参数：
      - 核心线程数：Runtime.getRuntime().availableProcessors() * 2
      - 队列容量：1024
      - 拒绝策略：CallerRunsPolicy（避免任务丢失）
    - **收益** ：
      - 单次调用耗时从20-30秒降至**13-15秒**
      - 多用户并发场景下，线程创建开销降低90%

    3. **异步日志追踪：装饰器模式集成MDC**

    - **问题** ：异步线程中MDC（Mapped Diagnostic Context）上下文丢失，导致日志无法关联请求链路
    - **解决方案** ：
    - 创建MdcThreadPoolDecorator装饰器，包装线程池的execute和submit方法
    - 在任务提交前注入当前请求的traceId到MDC，确保异步日志携带完整上下文

    ```java
    public class MdcThreadPoolDecorator implements ExecutorService {
          private final ExecutorService delegate;
          private final String traceId;

          public MdcThreadPoolDecorator(ExecutorService delegate, String traceId) {
              this.delegate = delegate;
              this.traceId = traceId;
          }

          @Override
          public void execute(Runnable command) {
              MDC.put("traceId", traceId);
              delegate.execute(() -> {
                  try { command.run(); }
                  finally { MDC.remove("traceId"); }
              });
          }
      }
    ```

    - **效果** ：异步日志可通过traceId精准关联请求，故障排查效率提升50%

    4. **RAG流程优化：异步顺序调整**

    - **优化逻辑** ：
    - 原流程：联网搜索（同步）→RAG处理（同步）
    - 新流程：**联网搜索（异步）**   **RAG处理（异步）** （通过thenCompose链式调用）
    - **收益** ：两个耗时操作并行执行，端到端响应时间进一步缩短约2秒

    三、优化效果数据

    | 指标             | 优化前    | 优化后    | 提升幅度 |
    | ---------------- | --------- | --------- | -------- |
    | 单次调用耗时     | 20-30秒   | 13-15秒   | 40%-50%  |
    | 多用户并发吞吐量 | 5请求/秒  | 15请求/秒 | 200%     |
    | 日志排查耗时     | 30分钟/次 | 10分钟/次 | 66.7%    |

    四、后续优化方向


    1. **客户端缓存升级** ：引入Redis实现跨进程客户端实例缓存，支持集群环境
    2. **流式搜索支持** ：与火山方舟对接流式返回接口，实现结果边搜索边展示
    3. **熔断与降级** ：添加Hystrix熔断机制，避免搜索服务异常拖垮整个系统

    五、总结
    通过 **状态反馈前置化** 、 **资源复用** 、 **异步化改造** 、**日志链路优化**的组合策略，联网搜索功能在用户体验和性能层面均实现显著提升。其中，**装饰器模式与线程池的结合**为异步场景下的日志追踪提供了通用解决方案，可复用于其他异步处理模块。后续将持续探索流式处理和分布式缓存技术，进一步突破性能瓶颈。
  * 话术

    * 做联网搜索这个需求的时候，需要给 Agent 提供联网搜索能力，实现思路是调用火山方舟的搜索能力，在做这个需求的过程中，发现联网搜索的时间过长，一般在 20-30 之间，用户体验差，所以首先给到用户预期，通过设置联网搜索标记，让前端能够显示联网搜索中字样，提升用户体验，接着是在时间上进行优化，首先，通过建立 ArkServiceCache 的方式，建立缓存，防止反复创建火山方舟客户端，然后使用 CompletableFuture 将联网搜索变成异步的方式，大大降低了单次调用的时间，接着创建线程池，使用 CallerRunsPolicy策略，将 CompletableFuture 执行的线程池改成了自定义的线程池，优化多次调用线程的创建开销，最后，在提测之后，有问题需要排查和修复，发现异步的方法里面，打的日志在 Argos 上面排查不到，然后在 ByteTech 社区里面查了一下，发现异步的方式打日志检索不到，解决方案是使用装饰器模式，包装了线程池，提供了 MDC 的功能，最后能够在联网搜索中打日志排查，并且大幅度优化了时间效率
  * 联网搜索时长过久，思考优化策略
  * CompletableFuture优化异步进行联网搜索
  * 发现RAG可以一块进行异步逻辑
  * 调整顺序，先联网搜索的异步，然后rag的异步
  * 通过线程池 + 装饰器模式，一方面减少异步频繁创建线程的开销问题，另外一方面，通过装饰器模式，装配mdc，确保能够进行日志的跟踪

    * 这里可以丰富一下线程池的包装，装饰器模式
  * 后续联网搜索调用 火山方舟 服务，发现创建资源可以进一步优化，通过缓存的方式，缓存火山客户端，避免创建客户端的开销
  * 通过一系列优化措施，联网搜索从20-30S 优化到13-15s左右的单次耗时
  * 由于线程池的作用，多用户进行联网搜索的时候，平均优化时间
  * 为了提高用户体验，联网搜索时，会弹出联网搜索的提示标志，后端设置标志位进行返回，给到用户预期
  * 
* 全召回
* SOP流程引擎
* 119、ticket平台，关于 5k qps 到 10w qps优化
* 可跳过的阻塞式分布式锁
  * 绘制可跳过的阻塞式分布式锁的mermaid流程图
  * ```
    graph TD
        A[开始] --> B[每次RPC建立缓存]
        B --> C{设置标志位}
        C -->|是| D[查缓存]
        C -->|否| E[争夺锁]
        D --> F{缓存存在?}
        F -->|是| G[返回缓存中的内容]
        F -->|否| H[重建缓存]
        H --> I[返回结果]
        E --> D  %% 争夺锁后再查缓存
        G --> J[结束]
        I --> J
    ```
  * 大概流程是
  * 分布式锁，自旋阻塞
  * 每次rpc建立缓存的时候，会设置标志位，后续对于相同的内容，就不会通过循环的方式阻塞了，而是直接跳过争夺锁的情况，然后直接去查缓存，如果存在，就返回缓存中的内容，如果不存在，那么就重建缓存后返回结果


你提供的内容是关于Agent调用日志明细的需求、技术选型、遇到的性能瓶颈及排查过程。下面我将对内容进行结构优化、表达精炼，并补充一些建议，使其更适合文档或汇报使用。

---

## OpenAPI 日志明细需求与优化实践

### 1. 需求背景

为满足数据分析需求，需要记录并查询Agent的调用日志明细。我们最初提供了两种数据接入方案：

- 方案一：数据表方式，支持MySQL和MongoDB。
- 方案二：通过OpenAPI方式，提供HTTP接口，配合鉴权逻辑供外部系统调用。

由于字节云与火山云环境不互通，数据表方案难以兼容，最终选择了OpenAPI接口方案。

---

### 2. 技术挑战与优化过程

#### 2.1 日志量过大导致的迁移需求

- Agent调用日志中，部分热门Agent产生的日志量极大，导致数据查询频繁超时，影响数据分析和业务使用。

#### 2.2 MongoDB大数据量瓶颈

- MongoDB在数据量达到约10,000条时，翻页查询性能急剧下降，频繁出现超时和请求失败。
- 初步优化：尝试延长MongoDB连接超时时间至2分钟，但效果有限，超时问题依然存在。

#### 2.3 迁移至Elasticsearch（ES）

- 为提升大数据量下的查询性能，将日志数据迁移至Elasticsearch。
- 迁移后，查询性能有明显改善，但根本问题未完全解决：若用户一次性请求所有调用记录，数据量过大时仍可能失败。
- 结论：ES能缓解大部分性能瓶颈，但需进一步优化接口的分页、限流和数据分批导出等机制。

---

### 3. 其他相关问题

#### 3.1 AK/SK鉴权

- OpenAPI接口采用AK/SK鉴权机制，确保数据安全合规。

#### 3.2 504超时问题排查

- 曾遇到CDN回流超时（504），且无logId等排查信息，导致定位困难。
- 处理过程：
  - Oncall紧急拉齐多个部门协同排查。
  - 排查链路：CDN > TLB > Janus > PSM，最终确认请求在CDN处超时。
  - TLB能获取到logId，但CDN无相关日志，建议后续完善链路追踪和日志采集。

---

### 4. 优化建议

1. **接口分页与限流**：强制分页返回，限制单次最大查询量，避免一次性拉取全部数据。
2. **异步导出机制**：对于大批量数据需求，采用异步导出+通知机制，提升用户体验。
3. **链路追踪完善**：全链路打通logId，便于问题快速定位。
4. **冷热数据分层**：将历史冷数据归档，减少主库压力。
5. **监控与告警**：完善接口超时、异常的监控与自动告警，提前发现问题。

---

如需进一步细化某一部分内容或补充技术细节，请告知！
