# Seata 分布式事务解决方案

Seata 是一款开源的分布式事务解决方案，致力于提供高性能和简单易用的分布式事务服务。Seata 将为用户提供了 AT、TCC、SAGA 和 XA 事务模式，为用户打造一站式的分布式解决方案。

### Seata 安装

官方 Github 仓库下载（v1.7.0）：https://github.com/apache/incubator-seata/releases/tag/v1.7.0

**配置 Seata**

解压缩后，在程序目录下找到 conf 文件夹

里面有一个配置文件 application.yaml 以及一份配置样例 application.example.yml

本次演示使用 Nacos 配置，将 application.yaml 下 seata.conf 和 seata.registry 的 type 修改为 nacos，并将配置样例中的 nacos 部分复制到配置文件内，修改对应的 server-addr 即可（如果有认证需放开 access-key 和 secret-key 配置并填入登录 nacos 的账号密码）。

```yaml
seata:
  config:
    # support: nacos, consul, apollo, zk, etcd3
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      namespace:
      group: SEATA_GROUP
      username:
      password:
      context-path:
      ##if use MSE Nacos with auth, mutex with username/password attribute
      #access-key:
      #secret-key:
      data-id: seataServer.properties
  registry:
    # support: nacos, eureka, redis, zk, consul, etcd3, sofa
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace:
      cluster: default
      username:
      password:
      context-path:
      ##if use MSE Nacos with auth, mutex with username/password attribute
      #access-key:
      #secret-key:
```

注意这里的 seata.conf 最后一行的 `data-id: seataServer.properties` 这里有一份 seata 在 nacos 的配置。此时我们需要在 nacos 创建一份同名文件（注意 Group 需要和 application.yaml 中的 seata.conf.group 一致）

seataServer.properties 同样有一份配置样例，在 seata 安装目录下的 script/config-center/config.txt 中，我们按需复制覆盖配置即可，例如：AT 模式需要的 db store 支持

**启动 seata**

window：双击 bin/seata-server.bat 文件

linux：sh seata-server.sh 

![image-20241105144053355](assets/README/image-20241105144053355.png)

验证 seata 启动成功的方式：

1. nacos 注册是否成功
2. 访问 seata 控制台：http://localhost:7091

![image-20241105144217272](assets/README/image-20241105144217272.png)

---

**Seata 的三大角色**

- TC (Transaction Coordinator) - 事务协调者
  维护全局和分支事务的状态，驱动全局事务提交或回滚
- TM (Transaction Manager) - 事务管理器
  定义全局事务的范围：开始全局事务、提交或回滚全局事务
- RM (Resource Manager) - 资源管理器
  管理分支事务处理的资源，与TC交谈以注册分支事务和报告分支事务的状态，并驱动分支事务提交或回滚

PS：TC 为单独部署的 Server 服务端，TM 和 RM 为嵌入到应用中的 Client 客户端

### Seata 项目集成

1）引入 spring cloud 和 spring cloud alibaba 集成

```xml
<!-- Spring Cloud 依赖 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>${springcloud.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<!-- Spring Cloud Alibaba 依赖 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-dependencies</artifactId>
    <version>${springcloudalibaba.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

2）添加 seata 依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<!-- 排除 spring cloud 版本内的 seata 版本 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
    <exclusions>
        <exclusion>
            <groupId>io.seata</groupId>
            <artifactId>seata-spring-boot-starter</artifactId>
        </exclusion>
        <exclusion>
            <groupId>io.seata</groupId>
            <artifactId>seata-all</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<!-- 指定与安装 seata 相同版本的依赖 -->
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>1.7.0</version>
</dependency>
```

3）配置 nacos 和 seata

```yaml
spring:
  application:
    name: order-server
  cloud:
    nacos:
      server-addr: localhost:8848
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: ${spring.application.name}-group
  service:
    vgroup-mapping:
      storage-server-group: default
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      namespace:
      group: SEATA_GROUP
```

启动项目，TM 和 RM 注册成功

```
2024-11-05 15:09:54.506  INFO 20576 --- [           main] i.s.c.r.netty.NettyClientChannelManager  : will connect to 10.0.0.197:8091
2024-11-05 15:09:54.638  INFO 20576 --- [           main] i.s.core.rpc.netty.NettyPoolableFactory  : NettyPool create channel to transactionRole:TMROLE,address:10.0.0.197:8091,msg:< RegisterTMRequest{version='1.7.0', applicationId='order-server', transactionServiceGroup='order-server-group', extraData='ak=null
digest=order-server-group,10.0.0.197,1730790594637
timestamp=1730790594637
authVersion=V4
vgroup=order-server-group
ip=10.0.0.197
'} >
2024-11-05 15:09:54.975  INFO 20576 --- [           main] i.s.c.rpc.netty.TmNettyRemotingClient    : register TM success. client version:1.7.0, server version:1.7.0,channel:[id: 0x177129d6, L:/10.0.0.197:61152 - R:/10.0.0.197:8091]
2024-11-05 15:09:54.979  INFO 20576 --- [           main] i.s.core.rpc.netty.NettyPoolableFactory  : register success, cost 81 ms, version:1.7.0,role:TMROLE,channel:[id: 0x177129d6, L:/10.0.0.197:61152 - R:/10.0.0.197:8091]
2024-11-05 15:09:54.980  INFO 20576 --- [           main] i.s.s.a.GlobalTransactionScanner         : Transaction Manager Client is initialized. applicationId[order-server] txServiceGroup[order-server-group]
2024-11-05 15:09:55.686  INFO 20576 --- [           main] i.s.c.r.netty.NettyClientChannelManager  : will connect to 10.0.0.197:8091
2024-11-05 15:09:55.686  INFO 20576 --- [           main] i.s.c.rpc.netty.RmNettyRemotingClient    : RM will register :jdbc:mysql://localhost:3306/se_order
2024-11-05 15:09:55.687  INFO 20576 --- [           main] i.s.core.rpc.netty.NettyPoolableFactory  : NettyPool create channel to transactionRole:RMROLE,address:10.0.0.197:8091,msg:< RegisterRMRequest{resourceIds='jdbc:mysql://localhost:3306/se_order', version='1.7.0', applicationId='order-server', transactionServiceGroup='order-server-group', extraData='null'} >
2024-11-05 15:09:55.699  INFO 20576 --- [           main] i.s.c.rpc.netty.RmNettyRemotingClient    : register RM success. client version:1.7.0, server version:1.7.0,channel:[id: 0x3ae041ca, L:/10.0.0.197:61158 - R:/10.0.0.197:8091]
2024-11-05 15:09:55.699  INFO 20576 --- [           main] i.s.core.rpc.netty.NettyPoolableFactory  : register success, cost 10 ms, version:1.7.0,role:RMROLE,channel:[id: 0x3ae041ca, L:/10.0.0.197:61158 - R:/10.0.0.197:8091]
```

### 事务模式

#### XA 模式

XA 规范 是 X/Open 组织定义的分布式事务处理（DTP，Distributed Transaction Processing）标准。Seata XA 模式是利用事务资源（数据库、消息服务等）对 XA 协议的支持，以 XA 协议的机制来管理分支事务的一种事务模式。

![img](assets/README/TB1hSpccIVl614jSZKPXXaGjpXa-1330-924.png)

优势

与 Seata 支持的其它事务模式不同，XA 协议要求事务资源本身提供对规范和协议的支持，所以事务资源（如数据库）可以保障从任意视角对数据的访问有效隔离，满足全局数据一致性。此外的一些优势还包括：

- 业务无侵入：和 AT 一样，XA 模式将是业务无侵入的，不给应用设计和开发带来额外负担。
- 数据库的支持广泛：XA 协议被主流关系型数据库广泛支持，不需要额外的适配即可使用。

缺点

XA prepare 后，分支事务进入阻塞阶段，收到 XA commit 或 XA rollback 前必须阻塞等待。事务资源长时间得不到释放，锁定周期长，而且在应用层上面无法干预，性能差。

**整体机制**

执行阶段

- 可回滚：业务 SQL 操作放在 XA 分支中进行，由资源对 XA 协议的支持来保证 可回滚
- 持久化：XA 分支完成后，执行 XA prepare，同样，由资源对 XA 协议的支持来保证 持久化（即，之后任何意外都不会造成无法回滚的情况）

完成阶段

- 分支提交：执行 XA 分支的 commit
- 分支回滚：执行 XA 分支的 rollback

**使用方式**

在有用到调用其他模块服务的更新操作出添加 @GlobalTransactional 注解即可，效果等同于 @Transactional，同时该注解实现了分布式事务能力

```java
@Override
@GlobalTransactional(rollbackFor = Exception.class)
public Order createOrder(CreateOrder createOrder) {
    // 查询商品
    Merchandise merchandise = apiCenter.getMerchandiseInfo(createOrder.getMerchandiseId());
    // 构建订单
    Order order = this.generateOrder(merchandise, createOrder.getQuantity());
    // 扣减库存
    apiCenter.merchandiseDeduct(createOrder.getMerchandiseId(), createOrder.getQuantity(), createOrder.getBuyerId());
    // 扣减余额
    apiCenter.accountPay(createOrder.getBuyerId(), order.getTotalPrice());
    // 保存订单
    orderMapper.save(order);
    return order;
}
```

PS：实操发现调用查询服务不可使用 @Transactional(readOnly = true)，否则会报错

#### AT 模式

AT 模式是 Seata 创新的一种非侵入式的分布式事务解决方案，Seata 在内部做了对数据库操作的代理层，我们使用 Seata AT 模式时，实际上用的是 Seata 自带的数据源代理 DataSourceProxy，Seata 在这层代理中加入了很多逻辑，比如插入回滚 undo_log 日志，检查全局锁等。

**整体机制**

两阶段提交协议的演变：

- 一阶段：业务数据和回滚日志记录在同一个本地事务中提交，释放本地锁和连接资源。
- 二阶段：
  - 提交异步化，非常快速地完成。
  - 回滚通过一阶段的回滚日志进行反向补偿。

AT 模式操作上像是 XA 模式的优化版本，同样是非侵入式，只需进行一些额外配置即可实现 XA 到 AT 模式的切换

**设置 seata.properties**

此处我们覆盖 store 存储方式为 db，将 store 的三个 mode 修改为 db，并且将 store.db 部分 copy 过来，修改数据库的 url、user 和 password 即可

```properties
#Transaction storage configuration, only for the server. The file, db, and redis configuration values are optional.
store.mode=db
store.lock.mode=db
store.session.mode=db
#Used for password encryption
store.publicKey=

#These configurations are required if the `store mode` is `db`. If `store.mode,store.lock.mode,store.session.mode` are not equal to `db`, you can remove the configuration block.
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.cj.jdbc.Driver
store.db.url=jdbc:mysql://127.0.0.1:3306/seata?useUnicode=true&rewriteBatchedStatements=true
store.db.user=root
store.db.password=123456
store.db.minConn=5
store.db.maxConn=30
store.db.globalTable=global_table
store.db.branchTable=branch_table
store.db.distributedLockTable=distributed_lock
store.db.queryLimit=100
store.db.lockTable=lock_table
store.db.maxWait=5000
```

数据库脚本：script/server/db/mysql.sql

为每一个业务数据库新增回滚日志表

```sql
CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
```

**修改 seata 数据源代理模式**

```yaml
seata:
  data-source-proxy-mode: AT
```

就这样，不需要修改业务代码，从配置上调整就可以无缝切换 AT！！！

---

参考资料：

[1]:https://blog.csdn.net/pi_tiger/article/details/131110412	"Seata 分布式事务搭建与使用详解"
[2]:https://www.bilibili.com/video/BV1K14y117gM	"Bilibili 浪飞yes 分布式事务与Seata落地"