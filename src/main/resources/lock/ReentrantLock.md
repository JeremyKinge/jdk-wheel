#### 一：ReentrantLock实现总结
个人CSDN相关相关ReentrantLock博客地址：https://blog.csdn.net/weixin_43495590/article/details/89709633

##### 1.1 设计思路
Lock锁设计思路采用模板方法设计模式，AQS中封装提供不同锁场景下的模板方法，如独占锁、中断敏感独占锁、限时尝试获取锁等。
锁的自定义实现只需要重写基本方法tryAcquire()，tryRelease()等

##### 1.2 锁的基础
锁实现的基础依靠AQS提供的int类型属性state记录锁的状态，相对应提供查询方法getState()，原子修改方法compareState()等
state使用volatile修饰保证可见性，CAS操作保证原子互斥性，提供了线程安全的实现。
锁的获取、释放就是对state属性值的操作

##### 1.3 重入锁
类CustomReentrantLock中已经注释重入锁的实现设计，操作就是增加AbstractOwnerSynchronizer抽象类中记录的当前持有锁线程与当前尝试获取锁线程是否一致的判断

##### 1.4 公平与非公平锁
CustomReentrantLock中模仿ReentrantLock实现公平与非公平锁的设计，AQS采用队列存储等待锁获取的线程。
代码中增加是否为第一个等待锁资源节点的判断就是公平锁的实现
对于公平锁注定是需要消耗更多的性能相对于非公平锁而言

#### 二：acquire()补充注释

独占锁获取的时候使用基本方法tryAcquire()尝试性加锁，失败则需要调用acquireQueued()等待加锁
```java
   public abstract class AbstractQueuedSynchronizer{
    
       public final void acquire(int arg) {
           
           /**
           * AQS中有两个队列，一个是竞争锁资源时使用LockSupport阻塞的队列SyncQueue，还有一个是Condition中等待线程队列ConditionQueue
           * addWaiter()根据EXCLUSIVE创建独占锁状态的线程等待锁节点
           */          
           if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
               selfInterrupt();
       }
    
       final boolean acquireQueued(final Node node, int arg) {
            boolean failed = true;
            try {
                boolean interrupted = false;
                
                /**
                * Node为AQS中提供的线程节点内部类，其中提供四种属性描述等待锁线程节点状态
                * CANCELLED 1：后续线程已经被取消
                * SIGNAL -1：指示后续线程需要unpark()，也就是让线程park()先
                * CONDITIONA -2：线程在Condition下等待
                * PROPAGATE -3：针对acquireShared表示无条件传播
                */
                
                /**
                * 自旋操作尝试性加锁直到加锁成功，尝试调用tryAcquire()的前提是队列头head的后继节点，锁在释放的时候也会唤醒head的后继节点
                * shouldParkAfterFailedAcquire()根据等待线层节点类型判断是否需要使用LockSupport，线程在第二次进入该方法后状态都会被修改为SIGNAL
                * parkAndCheckInterrupt()调用LuckSupport的park()方法阻塞线程并返回interrupted()状态
                */
                
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null; // help GC
                        failed = false;
                        return interrupted;
                    }
                    if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                        interrupted = true;
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }
 }
```
#### 三：release()补充注释
```java
public abstract class AbstractQueuedSynchronizer{
    
        public final boolean release(int arg) {
            
            /**
            * 调用自定义实现的基本方法tryRelease()修改锁状态标志属性state
            * 完毕后会判断SyncQueue队列中是否还存在等待锁线程队列
            * unparkSuccessor()主要操作就是调用LuckSupport的unpark()唤醒当前持有锁节点的下一节点线程
            */
            if (tryRelease(arg)) {
                Node h = head;
                if (h != null && h.waitStatus != 0)
                    unparkSuccessor(h);
                return true;
            }
            return false;
        }      
}
```

 