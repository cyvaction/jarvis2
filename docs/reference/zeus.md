# Zeus


## 文档链接

https://github.com/alibaba/zeus
https://github.com/michael8335/zeus2


## 杂项内容收集


* 多任务依赖的周期触发问题解决

比如任务D 分别依赖A,B,C ,假设这是一个每天都执行的任务, 设在第一天A,B成功执行，C没有执行。那么第一天无法触发D执行，到了第二天C执行成功，此时A,B还没有执行。但是D收到C的成功消息后，认为集齐了A,B,C的任务，将会触发D执行。 D的触发在我们看来是一个异常，因为第二天的A,B还没有执行，会导致D异常失败。为了解决这个问题，我们需要增加一个依赖周期的概念，我们需要为任务D增加一个依赖周期，配置为：同一天如此配置后，同一个案例，当第二天D收到C的成功消息后。发现A,B,C都成功了，然后对A,B,C的成功时间进行计算，发现A,B的成功时间是昨天的，则认为A,B的消息已经失效了，丢弃掉。 最后D只记录到C成功了，还会继续等待A,B任务的成功，才会触发。目前的依赖周期只有“同一天”，后续如果需求其他类型的，则需要自己升级添加。就我们的经验来说，大部分都是每日运行的，基本可以解决这个问题。


* 队列的处理

宙斯系统有三种类型的队列，分别是：自动任务队列 手动任务队列 调试任务队列
自动任务队列是指又宙斯系统自动调度触发的任务，或者用户点击手动恢复的任务队列
手动任务队列是指用户在调度中心手动执行的任务队列
调试任务队列是指用户在开发中心手动执行的任务队列

* zeus有提供pre和post processor接口, 可以借鉴一下.

比如实现了下载,cleanup之类的各种processor和job, 看起来是使用processor存信息, 再根据具体processor创建pre-job/post job, 逻辑上感觉有一点绕,可以不这么做,不过还是可以参考一下思想.

* 有触发方式这个字段, 这样立即执行的任务有两种, 一是可以触发后续任务, 二是不触发后续依赖任务. -> 这个要考虑一下我们是要这样做, 还是通过别的机制也能实现, 这个需求是存在的. -> 也就是说要提供一个手动run任务的可选执行方式机制

* run job的接口

基本接口只提供了, run/cancel等接口, 没有中途控制和反向反馈接口, 看看这两个需求是通过其它什么途径实现的.

* hive udf

执行hive 脚本时, 走 -i 的方式初始化.

* mr的jar依赖

通过resource结构传hdfs上的jar文件路径

* 调度这块, 看起来任务的成功失败, 触发后续任务, 是发广播消息给后续任务, 后续任务自己决定要不要启动运行的...

