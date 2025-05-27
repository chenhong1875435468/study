# 电影推荐系统比较实验

本项目实现并比较了两种协同过滤电影推荐算法（User-CF和Item-CF）的性能。

## 项目结构

- `data/` - 包含训练和测试数据集
  - `recommendation-ratings-train.txt` - 训练集
  - `recommendation-ratings-test.txt` - 测试集
- `电影推荐算法比较.ipynb` - 主要的Jupyter Notebook实现

## 数据格式

数据格式为：userId,movieId,rating,timestamp

- userId: 用户ID
- movieId: 电影ID
- rating: 评分 (1-5)
- timestamp: 时间戳

## 实现的算法

1. User-CF（基于用户的协同过滤）
   - 使用皮尔逊相关系数和余弦相似度计算用户相似度
   - 实现Top-N用户邻居选择策略

2. Item-CF（基于物品的协同过滤）
   - 基于物品间评分行为相似度进行推荐
   - 实现物品相似度计算与评分预测

## 评估指标

使用均方根误差（RMSE）评估算法性能，并通过可视化展示两种算法在不同参数设置下的表现。

## 实验结果

详细实验结果和分析请查看 `电影推荐算法比较.ipynb` 文件。 