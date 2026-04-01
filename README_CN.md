# FPTD - 超快速隐私保护可靠真值发现系统

## 项目概述

FPTD（Fast Privacy-preserving Truth Discovery）是一个为众感应用设计的隐私保护真值发现系统。该项目实现了论文《FPTD: Super Fast Privacy-Preserving and Reliable Truth Discovery for Crowdsensing》中提出的算法。

**核心特性：**
- 超快速处理速度
- 强隐私保护（基于安全多方计算）
- 高度可靠的真值发现机制
- 支持数值和分类数据

**系统配置：**
- 支持7个服务器的分布式部署（可配置）
- 采用阈值秘密分享（T = 4/7）
- 支持多轮迭代的真值发现（默认3轮）

---

## 目录结构与模块说明

### `/datasets/` - 数据集目录
存储用于测试和演示的数据集。

**子目录：**
- `weather/` - 天气数据集
  - `answer.csv` - 众包工作者提供的答案数据
  - `truth.csv` - 真实标准答案

**说明：** 数据集包含众感应用中收集的原始数据，用于验证真值发现算法的准确性。

---

### `/src/main/java/fptd/` - 核心源代码目录

#### **1. 核心主类**

- **`Main.java`** - 项目入口点
  - 主函数执行流程：加载数据 → 离线阶段 → 在线阶段
  - 调用 `runTDOffline()` 和 `runTDOnline()` 方法
  - 读取 `Params.java` 中的配置参数

- **`Params.java`** - 全局配置参数类
  - `NUM_SERVER` / `N` - 服务器数量（默认7）
  - `T` - 阈值（恢复数据所需最少服务器数，默认4）
  - `ITER_TD` - 真值发现迭代次数（默认3）
  - `P` - 大素数（用于有限域运算）
  - `IP_King` / `Port_King` - 协调服务器的IP和端口
  - `PRECISE_ROUND` - 浮点精度（10^5）
  - `sensingDataFile` / `truthFile` - 数据文件路径
  - `isCategoricalData` - 数据类型标记（分类/数值）

- **`Share.java`** - 秘密份额表示类
  - 表示单个服务器持有的秘密份额
  - 用于秘密分享协议

#### **2. 网络通信模块**

- **`EdgeServer.java`** - 边界服务器类
  - 管理本地计算和通信
  - 维护与其他服务器的连接
  - 处理离线和在线通信

- **`ServerThread.java`** - 服务器线程类
  - 处理单个服务器的并发请求
  - 管理每个服务器连接的生命周期

#### **3. 计算协议模块 - `protocols/` 目录**

实现了各种密码学计算门电路，支持隐私保护的算术和逻辑运算。

**核心类：**

- **`Gate.java`** - 门电路基类（抽象）
  - 定义了所有门的通用接口
  - 关键方法：
    - `doRunOffline()` - 离线阶段执行（生成随机数和掩码）
    - `doReadOfflineFromFile()` - 从文件读取预计算数据
    - `doRunOnline()` - 在线阶段执行（使用真实数据）

- **`Circuit.java`** - 电路类
  - 组织和连接多个门电路
  - 管理电路的整体数据流

**基础算术门电路：**

- **`InputGate.java`** - 输入门
  - 电路的数据输入点
  - 将原始数据转换为隐私保护的表示

- **`OutputGate.java`** - 输出门
  - 电路的数据输出点
  - 恢复最终计算结果

- **`AddGate.java`** - 加法门
  - 计算两个隐私数据的和：c = a + b
  - 不需要交互（本地可完成）

- **`AddConstantGate.java`** - 加常数门
  - 计算隐私数据与公开常数的和：c = a + k
  - 简单的本地操作

- **`SubtractGate.java`** - 减法门
  - 计算两个隐私数据的差：c = a - b

- **`ScalingGate.java`** - 缩放门
  - 计算隐私数据与公开缩放因子的乘积：c = a × k
  - 用于浮点精度调整

**高级计算门电路：**

- **`ElemWiseMultiplyGate.java`** - 元素逐位乘法门
  - 计算两个向量的逐元素乘积：c[i] = a[i] × b[i]
  - 需要交互和秘密分享

- **`DotProductGate.java`** - 点积门 ⚠️ 关键
  - 计算两个向量的点积：result = Σ(a[i] × b[i])
  - 真值发现中的核心运算
  - 需要多轮秘密分享

- **`ReduceGate.java`** - 求和门
  - 计算向量元素求和：result = Σa[i]
  - 用于数据聚合

- **`DivisionGate.java`** - 除法门
  - 计算两个隐私数据的商：c = a / b
  - 涉及复杂的非线性运算

- **`LogarithmGate.java`** - 对数门
  - 计算隐私数据的对数：c = log(a)
  - 用于真值发现中的权重计算（MV算法）

**复合计算门电路：**

- **`DotProdThenDivGate.java`** - 点积后除法
  - 复合运算：result = DotProduct(a,b) / c
  - 真值发现中的评分计算

- **`DotProdWithFilterGate.java`** - 带过滤的点积
  - 条件点积：result = Σ(filter[i] × a[i] × b[i])
  - 用于特定工作者的选择性计算

- **`ElemMulThenDivGate.java`** - 元素乘法后除法
  - 复合运算：c[i] = (a[i] × b[i]) / d

- **`ElemWiseMulThenMulConstGate.java`** - 元素乘法后乘常数
  - 复合运算：c[i] = (a[i] × b[i]) × k

- **`CombinationGate.java`** - 组合门
  - 支持多个输入的组合运算
  - 灵活的数据融合

#### **4. 离线处理模块 - `offline/` 目录**

离线阶段在真实数据到达前执行，用于生成可复用的随机掩码和辅助数据，提升在线阶段的效率。

**核心类：**

- **`FakeParty.java`** - 虚拟方类
  - 模拟安全多方计算中的虚拟参与者
  - 生成并分配秘密份额给各个真实服务器
  - 执行秘密重构和验证

- **`OfflineCircuit.java`** - 离线电路类
  - 组织离线阶段的计算逻辑
  - 管理离线输入、输出和中间数据
  - 支持链式调用构建复杂计算

- **`OfflineGate.java`** - 离线门基类（抽象）
  - 定义离线计算的通用接口
  - 方法：`runOffline()` - 执行离线计算
  - 存储预计算的随机数和掩码

**离线门电路实现** - 与 `protocols/` 中的类一一对应：

- `OfflineInputGate` - 离线输入
- `OfflineOutputGate` - 离线输出
- `OfflineAddGate` - 离线加法
- `OfflineAddConstantGate` - 离线加常数
- `OfflineSubtractGate` - 离线减法
- `OfflineScalingGate` - 离线缩放
- `OfflineElemWiseMultGate` - 离线逐元素乘法
- `OfflineDotProductGate` - 离线点积 ⚠️ 关键
- `OfflineReduceGate` - 离线求和
- `OfflineDivisionGate` - 离线除法
- `OfflineLogGate` - 离线对数
- `OfflineDotProdThenDivGate` - 离线点积后除法
- `OfflineDotProdWithFilterGate` - 离线带过滤点积
- `OfflineElemMulThenDivGate` - 离线元素乘后除
- `OfflineElemWiseMultThenMulConstGate` - 离线逐元素乘后乘常数
- `OfflineCombinationGate` - 离线组合门

#### **5. 秘密分享模块 - `sharing/` 目录**

实现秘密分享协议，用于安全地将秘密分散到多个服务器。

- **`Sharing.java`** - 秘密分享接口（抽象）
  - 定义分享和恢复的通用接口
  - 方法：
    - `share(secret, threshold, partyNum)` - 将秘密分享
    - `recover(shares)` - 恢复秘密

- **`ShamirSharing.java`** - Shamir秘密分享实现 ⚠️ 关键
  - 基于多项式插值的秘密分享
  - **(n, t)-threshold方案**：n个份额中任意t个可恢复秘密，少于t个无法恢复
  - 数学基础：
    - 在有限域GF(p)（p为素数）中工作
    - 选择t-1次随机多项式 f(x) = s + a₁x + a₂x² + ... + a_{t-1}x^{t-1}
    - 秘密 s = f(0)
    - 第i个份额为 share_i = f(i)
  - 恢复使用Lagrange插值公式

#### **6. 真值发现模块 - `truthDiscovery/optimized/` 目录**

实现真值发现算法，从众多有质量差异的工作者答案中推断真实标签。

**核心类：**

- **`TDOfflineOptimal.java`** - 离线真值发现类
  - 离线阶段的真值发现逻辑
  - 预计算可复用的掩码和随机数
  - 构建离线电路并执行
  - **⚠️ 重要约束**：拓扑结构必须与 `TDOnlineOptimal.java` 一致

- **`TDOnlineOptimal.java`** - 在线真值发现类 ⚠️ 关键
  - 在线阶段的真值发现逻辑
  - 使用真实众感数据
  - 执行迭代的真值推断
  - 计算工作者可靠性权重和真实标签
  - **迭代流程**（默认3轮）：
    1. **权重估计**：基于工作者与中间真值的一致性计算权重
    2. **真值推断**：使用权重聚合工作者答案
    3. **收敛判断**：检查真值是否收敛
  - 算法流程：
    ```
    初始化权重 w[i] ← 1
    for 每一轮迭代:
        真值[j] ← sum(w[i] × answer[i][j]) / sum(w[i])
        w[i] ← 更新基于与真值的相似度
    返回最终真值
    ```

#### **7. 工具类模块 - `utils/` 目录**

提供系统运行所需的各种工具函数。

- **`DataManager.java`** - 数据管理器
  - 读取CSV格式的众感数据
  - 解析 `answer.csv` 和 `truth.csv`
  - 维护数据矩阵：`sensingDataMatrix[工作者][检验][答案]`
  - 提供数据验证和转换功能

- **`LinearAlgebra.java`** - 线性代数工具
  - 矩阵和向量运算
  - 支持有限域中的运算
  - 矩阵求逆、转置等操作

- **`Metric.java`** - 评估指标计算
  - 计算精度（Accuracy）
  - 计算F1分数
  - 评估真值发现的质量

- **`Tool.java`** - 通用工具类
  - 文件I/O操作
  - 数据格式转换
  - 通用数学函数

- **`TwoTuple.java`** - 二元组类
  - 存储两个相关联的值
  - 用于返回多值结果

---

### `/src/test/java/` - 测试目录

包含单元测试，验证各个门电路的正确性。

#### **离线测试 - `offline/` 目录**
验证离线阶段各个操作的正确性：
- `TestAdditionOffline.java` - 离线加法门测试
- `TestSubtractOffline.java` - 离线减法门测试
- `TestScalingOffline.java` - 离线缩放门测试
- `TestElemWiseMultiplyOffline.java` - 离线逐元素乘测试
- `TestDotProductOffline.java` - 离线点积测试
- `TestDotProdThenDivOffline.java` - 离线点积后除测试
- `TestDotProdWithFilterOffline.java` - 离线过滤点积测试
- `TestElemMulThenDivOffline.java` - 离线元素乘后除测试
- `TestElemWiseMulThenMulConstOffline.java` - 离线元素乘后乘常数测试
- `TestDivisionOffline.java` - 离线除法门测试
- `TestLogOffline.java` - 离线对数门测试

#### **在线测试 - `online/` 目录**
验证在线阶段各个操作的正确性：
- `TestAdditionCircuit.java`
- `TestSubtractCircuit.java`
- `TestScalingCircuit.java`
- `TestElemWiseMultiplyOnline.java`
- `TestDotProductCircuit.java`
- `TestDotProdThenDivOnline.java`
- `TestDotProdWithFilterOnline.java`
- `TestElementMulThenDivOnline.java`
- `TestElemWiseMulThenMulConstOnline.java`
- `TestDivisionCircuit.java`
- `TestLogCircuit.java`

---

## 文件之间的关系与数据流

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        Main.java (入口点)                     │
│                    ↓ 读取配置参数 ↓                           │
│                    Params.java (全局配置)                     │
└───────────────────┬──────────────────┬──────────────────────┘
                    │                  │
         ┌──────────▼─────────┐  ┌────▼──────────────┐
         │  DataManager       │  │  EdgeServer       │
         │  (读取数据集)      │  │  (网络通信)       │
         │                    │  │  ServerThread     │
         └──────────┬─────────┘  └────┬──────────────┘
                    │                  │
                    ▼                  ▼
        ┌───────────────────────────────────────┐
        │   离线阶段 (TDOfflineOptimal)         │
        │   1. 创建虚拟方 (FakeParty)           │
        │   2. 构建离线电路 (OfflineCircuit)    │
        │   3. 执行预计算 (所有OfflineGate)    │
        │   4. 保存预计算数据                   │
        └────────────────┬──────────────────────┘
                         │
                    ┌────▼─────────┐
                    │ 离线数据文件 │
                    │ (预计算掩码)  │
                    └────┬─────────┘
                         │
        ┌────────────────▼──────────────────┐
        │   在线阶段 (TDOnlineOptimal)      │
        │   1. 加载离线数据                  │
        │   2. 构建在线电路 (Circuit)       │
        │   3. 使用真实数据执行计算          │
        │   4. 迭代真值发现 (3轮)           │
        │   5. 恢复最终真值                  │
        └────────────────┬─────────────────┘
                         │
        ┌────────────────▼──────────────────┐
        │   输出结果                         │
        │   评估指标 (Metric)               │
        └────────────────────────────────────┘
```

### 关键数据流

#### 1. **离线阶段流程**

```
初始化
  ↓
FakeParty (虚拟方)
  ├─ 为每个服务器生成秘密份额
  ├─ 使用 ShamirSharing 分发份额
  │
OfflineCircuit (离线电路)
  ├─ OfflineInputGate (生成随机输入)
  │  ├─ 通过 ShamirSharing 分享
  │  └─ 分配给各服务器
  │
  ├─ 链式执行运算门
  │  ├─ OfflineAddGate (a + b)
  │  ├─ OfflineDotProductGate (a·b) ← 核心
  │  ├─ OfflineScalingGate (a × k)
  │  └─ 其他OfflineGate...
  │
  └─ OfflineOutputGate (恢复中间结果)
      └─ 保存预计算随机数到文件

输出：离线数据文件 (./offline_data/)
```

#### 2. **在线阶段流程**

```
读取真实数据
  ↓
DataManager 解析数据集
  ├─ answer.csv (众包工作者答案)
  └─ truth.csv (真实标签用于验证)
  ↓
TDOnlineOptimal (真值发现-在线)
  ├─ 加载离线预计算数据
  │
  ├─ 第一次迭代 (初始权重w=1)
  │  ├─ INPUT: 工作者答案矩阵
  │  ├─ DotProductGate: 计算权重×答案 (Σ w[i]×ans[i])
  │  ├─ ReduceGate: 求和聚合
  │  ├─ DivisionGate: 计算平均值
  │  └─ OUTPUT: 初步真值
  │
  ├─ 第二/三次迭代 (更新权重)
  │  ├─ LogarithmGate: 计算相似度权重
  │  ├─ DotProductGate: 加权聚合
  │  └─ OUTPUT: 更新的真值
  │
  └─ 收敛检查
      └─ 返回最终真值

数据流：
工作者答案 → Circuit → 门电路链 → 秘密分享 + 恢复 → 明文结果
         ↑ 使用离线掩码进行隐私保护
```

### 门电路之间的连接关系

**示例：真值发现中的一个计算步骤**

```
工作者答案矩阵 A[6×100]    权重向量 W[6]
       │                        │
       └────────┬────────────────┘
                │
        DotProductGate (点积)
        计算：result[j] = Σ(w[i] × a[i][j])
                │
        ReduceGate (求和)
        计算：sum = Σ result[j]
                │
        DivisionGate (除法)
        计算：average = sum / |工作者数|
                │
        OutputGate (输出)
                │
                ▼
            真值结果
```

### 离线与在线的对应关系

| 离线门 | 在线门 | 用途 | 数据流 |
|--------|--------|------|--------|
| `OfflineInputGate` | `InputGate` | 数据输入 | 生成/加载秘密份额 |
| `OfflineAddGate` | `AddGate` | 加法运算 | 本地计算，无交互 |
| `OfflineDotProductGate` | `DotProductGate` | 点积（核心） | 需要秘密重构 |
| `OfflineDivisionGate` | `DivisionGate` | 除法运算 | 交互式计算 |
| `OfflineLogGate` | `LogarithmGate` | 对数（权重） | 使用预计算近似 |
| `OfflineOutputGate` | `OutputGate` | 输出结果 | 恢复明文 |

---

## 秘密分享机制 - Shamir分享详解

### 原理
在 $\mathbb{F}_p$（模p的有限域）中：
1. 选择秘密 $s$ 和 $t-1$ 个随机系数 $a_1, a_2, \ldots, a_{t-1}$
2. 构造多项式：$f(x) = s + a_1x + a_2x^2 + \ldots + a_{t-1}x^{t-1}$
3. 计算 $n$ 个份额：$share_i = f(i) \mod p$，其中 $i = 1,2,\ldots,n$
4. 恢复秘密：任意 $t$ 个份额可通过Lagrange插值恢复 $s = f(0)$

### 配置参数
- **n = 7**：7个服务器
- **t = 4**：任意4个服务器可恢复秘密
- **p**：大素数（1024比特）

### 安全性保证
- 少于 $t$ 个份额无法恢复秘密
- 每个份额在信息论上独立
- 即使 $t-1$ 个服务器被攻击者控制，秘密仍然安全

---

## 真值发现算法流程

### 多数投票（MV）算法变体

```
初始化：w[i] ← 1 (所有工作者权重相等)

for iter = 1 to ITER_TD (通常3轮):
    # 步骤1：加权真值推断
    for j = 1 to examNum:
        truth[j] ← Σ(w[i] × answer[i][j]) / Σ(w[i])
                   (在隐私保护下计算)
    
    # 步骤2：权重更新（基于与推断真值的一致性）
    for i = 1 to workerNum:
        accuracy[i] ← Σ(answer[i][j] == truth[j]) / examNum
        w[i] ← accuracy[i]  # 或其他相似度度量
    
    # 步骤3：收敛判断
    if truth 与上一轮收敛:
        break

return truth[]
```

---

## 系统执行流程

### 完整执行步骤

1. **初始化与配置读取**
   - `Main.main()` 启动
   - 读取 `Params.java` 配置（服务器数、阈值、数据文件等）

2. **数据加载**
   - `DataManager` 读取 `datasets/weather/answer.csv`
   - 解析工作者答案矩阵（6个工作者 × 100个检验样本）

3. **离线阶段执行**
   - `TDOfflineOptimal.runTDOffline()`
   - 创建 `FakeParty` 虚拟方
   - 构建 `OfflineCircuit` 离线电路
   - 执行所有 `OfflineGate` 预计算随机掩码
   - 保存预计算数据到 `./offline_data/`

4. **在线阶段执行**
   - `TDOnlineOptimal.runTDOnline()`
   - 加载离线预计算数据
   - 构建 `Circuit` 在线电路
   - 使用真实数据执行 `DotProductGate` → `ReduceGate` → `DivisionGate` 链
   - 迭代3轮真值发现
   - 每轮使用 `LogarithmGate` 更新工作者权重

5. **结果输出**
   - 计算最终真值
   - 使用 `Metric` 计算精度、F1分数
   - 与 `truth.csv` 中的标准答案对比验证

---

## 核心模块关键特性

### 安全多方计算特性

| 特性 | 实现 | 用途 |
|------|------|------|
| **秘密分享** | `ShamirSharing.java` | 将数据分散到7个服务器 |
| **隐私计算** | 各种 `Gate.java` | 在密文/份额上进行计算 |
| **秘密恢复** | `OutputGate.java` | 安全恢复计算结果 |
| **阈值容错** | n=7, t=4 | 任意4个诚实服务器可恢复 |

### 真值发现特性

| 特性 | 实现 | 优势 |
|------|------|------|
| **迭代权重** | `TDOnlineOptimal.java` | 收敛到合理的真值 |
| **可靠性评估** | `LogarithmGate.java` | 自动识别不可靠工作者 |
| **隐私保护** | 全程使用 `Gate.java` | 真值和权重不暴露 |
| **快速处理** | 离线/在线分离 | 提高实时性能 |

---

## 测试验证

### 单元测试用途

离线和在线测试确保：
1. **正确性** - 每个门的计算结果正确
2. **一致性** - 离线预计算与在线使用一致
3. **隐私** - 中间结果保持隐密态

### 运行测试

```bash
# 运行单个测试
javac -cp . src/test/java/offline/TestDotProductOffline.java
java TestDotProductOffline

# 运行所有测试
for test in src/test/java/offline/*.java; do
    javac -cp . "$test"
    java $(basename "$test" .java)
done
```

---

## 重要约束与注意事项

⚠️ **关键约束**（来自原README）：
> **离线真值发现电路（TDOfflineOptimal.java）的拓扑结构必须与在线真值发现电路（TDOnlineOptimal.java）完全相同！**

这确保了：
- 离线预计算的随机掩码大小与在线使用相匹配
- 电路中间节点个数一致
- 门的连接顺序相同

---

## 快速开始指南

### 1. 配置参数
编辑 `Params.java`：
```java
// 选择数据集（已注释的选项）
public final static String sensingDataFile = "datasets/weather/answer.csv";
public final static String truthFile = "datasets/weather/truth.csv";
public final static boolean isCategoricalData = false;

// 调整迭代次数
public static int ITER_TD = 3;

// 调整服务器数量
public static final int NUM_SERVER = 7;
```

### 2. 运行程序
```bash
java fptd.Main
```

### 3. 查看输出
程序输出包括：
- 离线阶段进度
- 在线阶段真值发现迭代过程
- 最终精度指标

---

## 总结

FPTD系统通过以下创新设计实现了快速和隐私保护的真值发现：

1. **模块化设计** - 清晰的离线/在线分离，易于扩展
2. **安全多方计算** - 基于Shamir秘密分享的隐私保护
3. **算术电路** - 支持复杂的非线性计算（对数、除法）
4. **批量预计算** - 离线阶段生成可复用的掩码，加速在线执行
5. **分布式架构** - 支持7个服务器的分布式部署和fault tolerance

这些特性结合使得FPTD能够在保护众感数据隐私的同时，实现快速可靠的真值发现。
