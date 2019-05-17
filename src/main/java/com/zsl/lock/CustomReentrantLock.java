package com.zsl.lock;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO 实现ReentrantLock锁
 * @author zsl
 * @version 1.0
 * @date: 2019/5/16 15:00
 **/
public class CustomReentrantLock implements Lock, Serializable {
    
    /**
     * 序列化版本号
     * IDEA默认对于实现Serialize接口没有serialVersionUID属性无警告
     * File -> settings -> Editor -> Inspections -> Serialization issues -> Serialization class without 'serialVersionUID'
     */
    private static final long serialVersionUID = 5620081294534636810L;
    
    /**
     * 锁的公平实现与非公平实现实例
     */
    private final AbstractSync abstractSync;
    
    /**
     * 默认提供非公平实现
     */
    public CustomReentrantLock(){
        this(Boolean.FALSE);
    }
    
    /**
     * 根据参数fair实例化CustomReentrantLock,属性abstractSync赋值
     * @param fair {@code true) 实现公平锁 {@code false} 实现非公平锁
     */
    public CustomReentrantLock(boolean fair){
        abstractSync = fair ?  new FairSync() :  new NonFairSync();
    }
    
    abstract static class AbstractSync extends AbstractQueuedSynchronizer{
        
        private static final long serialVersionUID = -8925767597015408078L;
        
        /**
         * TODO 对外部类提供锁，子类各自组装方法实现，屏蔽外部类具体实现细节
         * @author zsl
         * @date 2019/5/16 19:26
         * @return void
         **/
        abstract void lock();
    
        /**
         * TODO 实现独占锁的非公平加锁
         * @author zsl
         * @date 2019/5/16 16:46
         * @param count 加锁数量
         * @return boolean
         **/
        protected boolean nonfairTryAcquire (int count) {
             Thread currentThread = Thread.currentThread();
             
             /**
              *  获取加锁的状态，AQS中使用int类型数据state保存，该属性使用volatile关键字修饰保证线程可见性
              *  锁实现的关键方法就是AQS提供的compareAndSetState()方法用于CAS原子替换state
              */
             
             int state = super.getState();
             
             /**
               * state等于0表示未上锁
               * compareAndSetState()加锁，成功调用setExclusiveOwnerThread()设置当前持有锁线程
               * AQS继承抽象类AbstractOwnableSynchronizer，setExclusiveOwnerThread()由该类实现
              */
             
             if (state == 0){
                 if (super.compareAndSetState(0,count)){
                     setExclusiveOwnerThread(currentThread);
                     return true;
                 }
             }
             
             /**
               * 重入锁设计，持有锁线程重复加锁时会更新锁状态标志state
               * 重入锁的释放也需要多次释放，直到将state清零
             */
             
             /**
               * state数据类型为int，最大数值为2147483647，超过再加就会变为负数
               * 最大值+1变为负数的原理是因为计算机采用补码存储
               * 正数补码与原码一致
               * 负数原码转补码是  逐个取反除第一个符号位外后加1
               * 补码转原码是      减一后逐个取反
             */
             
             /**
               * 2147483647补码：0111_1111_1111_1111_1111_1111_1111_1111
               * 1补码         : 0000_0000_0000_0000_0000_0000_0000_0001
               * 相加补码      ：1000_0000_0000_0000_0000_0000_0000_0000
               * 原码          ：1000_0000_0000_0000_0000_0000_0000_0000
               * 十进制数      ：-2147483648
              */
             
             if (currentThread == super.getExclusiveOwnerThread()){
                int newState = state + count;
                if (newState < 0){
                    throw new Error("超过最大锁数量");
                }
                super.setState(newState);
                return true;
             }
             return false;
        }
    
        /**
         * TODO 实现独占锁公平加锁
         * @author zsl
         * @date 2019/5/16 18:47
         * @param count 加锁数量
         * @return boolean
         **/
        protected boolean fairTryAcquire (int count) {
            Thread currentThread = Thread.currentThread();
            int state = super.getState();
    
            /**
              * 公平锁与非公平锁的区别就在这一个方法hasQueuedPredecessors()的调用，由父类AbstractSynchronizer实现提供
              * 方法作用是判断当前线程是否为等待队列第一线程
              * 对于重入锁的实现设计并没有任何修改
             */
    
            if (state == 0){
                if (!super.hasQueuedPredecessors() && super.compareAndSetState(0,count)){
                    super.setExclusiveOwnerThread(currentThread);
                    return true;
                }
            }
            if (currentThread == super.getExclusiveOwnerThread()){
                int newState = state + count;
                if (newState < 0){
                    throw new Error("加锁数量超过最大值");
                }
                super.setState(newState);
                return true;
            }
            return false;
        }
    
        /**
         * TODO AQS释放锁模板方法release()调用的基本方法释放锁实现
         * @author zsl
         * @date 2019/5/16 19:51
         * @param count 释放锁的数量
         * @return boolean
         **/
        @Override
        protected boolean tryRelease (int count) {
            int newState = super.getState() - count;
            boolean release = false;
            Thread currentThread = Thread.currentThread();
    
            /**
              * 判断代码将持有锁线程与当前线程进行比对有两个效果
              * 1.规范必须在持有锁的时候才能释放锁，防止锁的误释放
              * 2.保证了锁状态state不会小于0,当state等于0是会将持有锁线程置为空，在这判断就会抛出异常
             */
            
            if (currentThread != super.getExclusiveOwnerThread()){
                throw new IllegalMonitorStateException("释放锁的线程非当前持有锁线程");
            }
            if (newState == 0){
                release = true;
                super.setExclusiveOwnerThread(null);
            }
            super.setState(newState);
            return release;
        }
    
        /**
         * TODO 获取Condition对象
         * @author zsl
         * @date 2019/5/16 20:25
         * @return java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject
         **/
        final ConditionObject newCondition() {
    
            /**
              * ConditionObject是AQS的一个实例内部类
              * Condition操作一般涉及到await()与signal()
              * Condition的操作必须是在获取锁的基础上进行操作
              * await() -> fullyRelease() -> release() -> tryRelease()，其中tryRealse()对非当前持有锁线程会抛出异常
             */
            return new ConditionObject();
        }
    }
    
    private static class NonFairSync extends AbstractSync{
        
        private static final long serialVersionUID = 6386273947522327876L;
    
        /**
         * TODO 根据AbstractSync中非公平独占锁的实现组装父类AbstractQueuedSynchronized模板方法acquire()的基本方法tryAcquire()
         * @author zsl
         * @date 2019/5/16 19:21
         * @param count 加锁数量
         * @return boolean
         **/
        @Override
        protected boolean tryAcquire (int count) {
            return super.nonfairTryAcquire(count);
        }
    
        /**
         * TODO
         * @author zsl
         * @date 2019/5/16 19:26
         * @return void
         **/
        @Override
        final void lock () {
            
            /**
              *相对于公平实现而言多了一次锁的尝试获取
             */
            
            if (super.compareAndSetState(0,1)){
                super.setExclusiveOwnerThread(Thread.currentThread());
            }
            super.acquire(1);
        }
        
    }
    
    private static class FairSync extends AbstractSync{
        
        private static final long serialVersionUID = -8629002988394190212L;
    
        /**
         * TODO 与非公平实现一致
         * @author zsl
         * @date 2019/5/16 19:21
         * @param count 加锁数量
         * @return boolean
         **/
        @Override
        protected boolean tryAcquire (int count) {
            return super.fairTryAcquire(count);
        }
    
        @Override
        final void lock () {
            super.acquire(1);
        }
    }
    
    
    
    @Override
    public void lock () {
        this.abstractSync.lock();
    }
    
    /**
     * TODO 线程中断敏感获取锁
     * @author zsl
     * @date 2019/5/16 20:02
     * @return void
     * @exception InterruptedException 线程中断敏感异常
     **/
    @Override
    public void lockInterruptibly () throws InterruptedException {
        /**
          * acquireInterruptibly()是AQS提供的线程中断敏感获取锁的模板方法
          * 线程中断敏感体现在两个地方：
          * 1. 进入方法首先调用interrupted()判断线程中断状态标志
          * 2. 在线程自旋操作获取锁也就是doAcquireInterruptibly()中判断线程中断状态标志
         */
        this.abstractSync.acquireInterruptibly(1);
    }
    
    /**
     * TODO 尝试性的获取锁，也就是线程不会阻塞等待锁资源
     * @author zsl
     * @date 2019/5/16 19:59
     * @return boolean
     **/
    @Override
    public boolean tryLock () {
        return this.abstractSync.nonfairTryAcquire(1);
    }
    
    /**
     * TODO 在time限定时长内尝试获取锁资源
     * @author zsl
     * @date 2019/5/16 20:12
     * @param  time 尝试获取锁等待时间长度 unit等待时间单位对象
     * @return boolean
     * @exception InterruptedException 线程中断敏感异常
     **/
    @Override
    public boolean tryLock (long time, TimeUnit unit) throws InterruptedException {
        /**
          * 限定时长内尝试获取锁资源与{@link #lockInterruptibly}敏感获取锁一致都会在方法前和自旋操作中做敏感判断
          * 限定时长也就是限定自旋操作的时长，多了如下代码判断
          * if (nanosTimeout <= 0L) return false;
         */
        return this.abstractSync.tryAcquireNanos(1,unit.toNanos(time));
    }
    
    @Override
    public void unlock () {
        this.abstractSync.release(1);
    }
    
    @Override
    public Condition newCondition () {
        return this.abstractSync.newCondition();
    }
}

