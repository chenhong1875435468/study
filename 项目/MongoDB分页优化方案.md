# MongoDB分页优化方案详解

## 问题背景

在HiAgent项目中，Agent调用日志查询面临大数据量分页性能问题：
- MongoDB在数据量达到约10,000条时，翻页查询性能急剧下降
- 传统`skip()`操作需要扫描并丢弃前面的所有文档
- 随着页码增加，性能呈指数级下降，频繁出现超时和请求失败

## 传统分页的问题分析

### 1. 性能瓶颈
```javascript
// 传统分页方式 - 性能差
db.agent_logs.find().skip(10000).limit(10)
```

**问题根源**：
- `skip()`操作需要遍历所有跳过的文档
- 无法有效利用索引
- 内存消耗高，容易超时
- 时间复杂度为O(n)，n为跳过的文档数量

### 2. 性能测试数据
| 跳过文档数 | 查询时间 | 内存消耗 | 成功率 |
|------------|----------|----------|--------|
| 1,000      | 50ms     | 低       | 100%   |
| 5,000      | 200ms    | 中       | 95%    |
| 10,000     | 800ms    | 高       | 80%    |
| 50,000     | 5s+      | 很高     | 30%    |
| 100,000    | 15s+     | 极高     | 10%    |

## 优化策略详解

### 1. 基于游标的分页（Cursor-based Pagination）

#### 1.1 核心原理
使用文档的唯一字段作为游标，避免skip操作，实现O(log n)的查询性能。

#### 1.2 实现方案
```javascript
// 优化前：传统分页
db.agent_logs.find().skip(10000).limit(10)

// 优化后：基于游标分页
db.agent_logs.find({
  _id: {$gt: ObjectId("last_document_id")}
}).limit(10).sort({_id: 1})
```

#### 1.3 完整实现示例
```javascript
// 第一页查询
function getFirstPage(limit = 10) {
  return db.agent_logs.find()
    .limit(limit)
    .sort({_id: 1})
    .toArray();
}

// 后续页面查询
function getNextPage(lastId, limit = 10) {
  return db.agent_logs.find({
    _id: {$gt: ObjectId(lastId)}
  })
  .limit(limit)
  .sort({_id: 1})
  .toArray();
}

// 带条件的分页查询
function getFilteredPage(filters, lastId, limit = 10) {
  const query = {
    ...filters,
    _id: {$gt: ObjectId(lastId)}
  };
  
  return db.agent_logs.find(query)
    .limit(limit)
    .sort({_id: 1})
    .toArray();
}
```

### 2. 复合游标分页

#### 2.1 适用场景
需要按多个字段排序，如按时间倒序+ID倒序。

#### 2.2 实现方案
```javascript
// 按创建时间和ID排序的分页
function getNextPageByTime(lastCreatedAt, lastId, limit = 10) {
  return db.agent_logs.find({
    $or: [
      {created_at: {$lt: new Date(lastCreatedAt)}},
      {
        created_at: new Date(lastCreatedAt), 
        _id: {$lt: ObjectId(lastId)}
      }
    ]
  })
  .limit(limit)
  .sort({created_at: -1, _id: -1})
  .toArray();
}
```

#### 2.3 复杂查询示例
```javascript
// 多条件复合分页查询
function getComplexPage(filters, lastDoc, limit = 10) {
  const {agent_id, status, created_at, _id} = lastDoc;
  
  const query = {
    ...filters,
    $or: [
      {created_at: {$lt: new Date(created_at)}},
      {
        created_at: new Date(created_at),
        _id: {$lt: ObjectId(_id)}
      }
    ]
  };
  
  return db.agent_logs.find(query)
    .limit(limit)
    .sort({created_at: -1, _id: -1})
    .toArray();
}
```

### 3. 索引优化策略

#### 3.1 索引设计原则
- 为分页字段创建复合索引
- 索引顺序要与查询顺序一致
- 考虑查询条件的选择性

#### 3.2 索引创建示例
```javascript
// 基础分页索引
db.agent_logs.createIndex({_id: 1});

// 时间排序索引
db.agent_logs.createIndex({created_at: -1, _id: -1});

// 复合查询索引
db.agent_logs.createIndex({
  agent_id: 1, 
  status: 1, 
  created_at: -1, 
  _id: -1
});

// 全文搜索索引
db.agent_logs.createIndex({
  content: "text",
  created_at: -1,
  _id: -1
});
```

#### 3.3 索引性能分析
```javascript
// 查看查询计划
db.agent_logs.find({
  agent_id: "agent_123",
  created_at: {$lt: new Date()}
}).limit(10).explain("executionStats");

// 查看索引使用情况
db.agent_logs.getIndexes();
```

### 4. 聚合管道优化

#### 4.1 传统聚合分页
```javascript
// 性能差的聚合分页
db.agent_logs.aggregate([
  { $match: { status: "success" } },
  { $sort: { created_at: -1 } },
  { $skip: 10000 },
  { $limit: 10 }
]);
```

#### 4.2 优化后的聚合分页
```javascript
// 优化的聚合分页
db.agent_logs.aggregate([
  { $match: { 
    status: "success",
    $or: [
      {created_at: {$lt: new Date(lastCreatedAt)}},
      {
        created_at: new Date(lastCreatedAt), 
        _id: {$lt: ObjectId(lastId)}
      }
    ]
  }},
  { $sort: { created_at: -1, _id: -1 } },
  { $limit: 10 }
]);
```

#### 4.3 复杂聚合查询
```javascript
// 带统计的聚合分页
db.agent_logs.aggregate([
  { $match: { agent_id: "agent_123" } },
  { $sort: { created_at: -1, _id: -1 } },
  { $limit: 10 },
  {
    $facet: {
      data: [{ $limit: 10 }],
      totalCount: [{ $count: "count" }],
      stats: [
        { $group: { 
          _id: null, 
          avgDuration: {$avg: "$duration"},
          successRate: {
            $avg: {$cond: [{$eq: ["$status", "success"]}, 1, 0]}
          }
        }}
      ]
    }
  }
]);
```

## 实际应用方案

### 1. API设计调整

#### 1.1 请求参数设计
```javascript
// 传统分页参数
{
  "page": 1,
  "pageSize": 10,
  "agent_id": "agent_123"
}

// 优化后的游标分页参数
{
  "limit": 10,
  "cursor": "eyJjcmVhdGVkX2F0IjoiMjAyNC0wMS0wMVQwMDowMDowMC4wMDBaIiwiX2lkIjoiNjU5YjFhYjFhYjFhYjFhYiJ9",
  "agent_id": "agent_123"
}
```

#### 1.2 响应格式设计
```javascript
// 响应格式
{
  "data": [...],
  "next_cursor": "eyJjcmVhdGVkX2F0IjoiMjAyNC0wMS0wMVQwMDowMDowMC4wMDBaIiwiX2lkIjoiNjU5YjFhYjFhYjFhYjFhYiJ9",
  "has_more": true,
  "total_estimated": 50000
}
```

#### 1.3 游标编码实现
```javascript
// 游标编码
function encodeCursor(lastDoc) {
  const cursorData = {
    created_at: lastDoc.created_at.toISOString(),
    _id: lastDoc._id.toString()
  };
  return Buffer.from(JSON.stringify(cursorData)).toString('base64');
}

// 游标解码
function decodeCursor(cursor) {
  try {
    const cursorData = JSON.parse(Buffer.from(cursor, 'base64').toString());
    return {
      created_at: new Date(cursorData.created_at),
      _id: ObjectId(cursorData._id)
    };
  } catch (error) {
    throw new Error('Invalid cursor format');
  }
}
```

### 2. 缓存策略

#### 2.1 Redis缓存设计
```javascript
// 缓存键设计
const CACHE_KEY_PREFIX = "agent_logs:";
const CACHE_TTL = 300; // 5分钟

// 缓存查询结果
async function getCachedPage(cacheKey, queryFn) {
  const cached = await redis.get(cacheKey);
  if (cached) {
    return JSON.parse(cached);
  }
  
  const result = await queryFn();
  await redis.setex(cacheKey, CACHE_TTL, JSON.stringify(result));
  return result;
}

// 缓存键生成
function generateCacheKey(filters, cursor, limit) {
  const key = `${CACHE_KEY_PREFIX}${JSON.stringify(filters)}:${cursor}:${limit}`;
  return crypto.createHash('md5').update(key).digest('hex');
}
```

#### 2.2 缓存失效策略
```javascript
// 数据更新时清除相关缓存
async function invalidateCache(agentId) {
  const pattern = `${CACHE_KEY_PREFIX}*"agent_id":"${agentId}"*`;
  const keys = await redis.keys(pattern);
  if (keys.length > 0) {
    await redis.del(...keys);
  }
}
```

### 3. 性能监控

#### 3.1 查询性能监控
```javascript
// 查询性能监控
async function monitorQueryPerformance(queryFn, queryName) {
  const startTime = Date.now();
  try {
    const result = await queryFn();
    const duration = Date.now() - startTime;
    
    // 记录性能指标
    await recordMetrics({
      query_name: queryName,
      duration: duration,
      success: true,
      timestamp: new Date()
    });
    
    return result;
  } catch (error) {
    const duration = Date.now() - startTime;
    await recordMetrics({
      query_name: queryName,
      duration: duration,
      success: false,
      error: error.message,
      timestamp: new Date()
    });
    throw error;
  }
}
```

#### 3.2 慢查询告警
```javascript
// 慢查询告警
async function checkSlowQuery(duration, queryName) {
  const SLOW_QUERY_THRESHOLD = 1000; // 1秒
  
  if (duration > SLOW_QUERY_THRESHOLD) {
    await sendAlert({
      type: 'slow_query',
      query_name: queryName,
      duration: duration,
      threshold: SLOW_QUERY_THRESHOLD,
      timestamp: new Date()
    });
  }
}
```

## 混合优化策略

### 1. 数据分层策略

#### 1.1 冷热数据分离
```javascript
// 热数据（最近7天）
const HOT_DATA_COLLECTION = "agent_logs_hot";

// 冷数据（7天前）
const COLD_DATA_COLLECTION = "agent_logs_cold";

// 查询策略
async function queryLogs(filters, cursor, limit) {
  const {startDate, endDate} = filters;
  const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
  
  if (endDate > sevenDaysAgo) {
    // 查询热数据
    return await queryHotData(filters, cursor, limit);
  } else {
    // 查询冷数据
    return await queryColdData(filters, cursor, limit);
  }
}
```

#### 1.2 数据归档策略
```javascript
// 数据归档脚本
async function archiveOldData() {
  const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
  
  // 移动旧数据到归档集合
  await db.agent_logs.aggregate([
    { $match: { created_at: { $lt: thirtyDaysAgo } } },
    { $out: "agent_logs_archive" }
  ]);
  
  // 删除原集合中的旧数据
  await db.agent_logs.deleteMany({
    created_at: { $lt: thirtyDaysAgo }
  });
}
```

### 2. 读写分离

#### 2.1 主从分离
```javascript
// 读操作使用从库
const readDb = db.getSiblingDB('agent_logs_read');

// 写操作使用主库
const writeDb = db.getSiblingDB('agent_logs_write');

// 查询函数
async function queryLogs(filters, cursor, limit) {
  return await readDb.agent_logs.find(filters)
    .limit(limit)
    .sort({created_at: -1, _id: -1})
    .toArray();
}
```

### 3. 分片策略

#### 3.1 按时间分片
```javascript
// 按月分片
const monthlyCollections = {
  "2024-01": "agent_logs_2024_01",
  "2024-02": "agent_logs_2024_02",
  // ...
};

// 查询函数
async function queryLogsByMonth(filters, cursor, limit) {
  const {startDate, endDate} = filters;
  const collections = getCollectionsInRange(startDate, endDate);
  
  const results = [];
  for (const collection of collections) {
    const collectionResults = await db[collection].find(filters)
      .limit(limit)
      .sort({created_at: -1, _id: -1})
      .toArray();
    results.push(...collectionResults);
  }
  
  return results.slice(0, limit);
}
```

## 性能对比测试

### 1. 测试环境
- 数据量：100万条Agent调用日志
- 索引：复合索引 `{agent_id: 1, created_at: -1, _id: -1}`
- 硬件：4核8G内存

### 2. 测试结果

| 分页方式 | 第1页 | 第100页 | 第1000页 | 第10000页 |
|----------|-------|---------|----------|-----------|
| 传统skip | 50ms  | 200ms   | 800ms    | 5s+       |
| 游标分页 | 50ms  | 60ms    | 70ms     | 80ms      |
| 性能提升 | 0%    | 70%     | 91%      | 98%+      |

### 3. 内存使用对比

| 分页方式 | 内存使用 | 稳定性 |
|----------|----------|--------|
| 传统skip | 高       | 差     |
| 游标分页 | 低       | 好     |

## 实施建议

### 1. 渐进式迁移
1. **第一阶段**：新功能使用游标分页
2. **第二阶段**：核心查询接口迁移
3. **第三阶段**：全量迁移，移除skip操作

### 2. 兼容性处理
```javascript
// 兼容传统分页参数
function handlePagination(params) {
  if (params.page && params.pageSize) {
    // 传统分页，转换为游标分页
    return convertToCursorPagination(params);
  } else if (params.cursor) {
    // 游标分页
    return params;
  }
}
```

### 3. 监控和告警
- 设置查询性能监控
- 配置慢查询告警
- 定期分析查询模式

## 总结

MongoDB分页优化通过以下策略实现：

1. **游标分页**：避免skip操作，性能稳定
2. **索引优化**：创建合适的复合索引
3. **缓存策略**：减少重复查询
4. **数据分层**：冷热数据分离
5. **监控告警**：及时发现问题

通过这些优化，可以将分页查询性能从O(n)优化到O(log n)，显著提升大数据量下的查询效率，解决HiAgent项目中的性能瓶颈问题。 