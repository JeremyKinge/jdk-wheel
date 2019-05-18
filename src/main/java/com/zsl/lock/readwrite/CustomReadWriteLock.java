package com.zsl.lock.readwrite;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;


/**
 * 层次结构分明，沿袭Lock锁的实现细节采用AbstractSync抽象类实现锁，两个子类实现公平与非公平策略
 * 内部类ReentrantReadLock代表的是共享锁的读锁，ReentrantWriteLock代表独占锁写锁
 * 关联CSDN博客：<a>https://blog.csdn.net/weixin_43495590/article/details/90172914</a>
 */

/**
 * TODO 实现ReentrantReadWriteLock读写锁
 * @author zsl
 * @version 1.0
 * @date: 2019/5/17 15:44
 **/
public class CustomReadWriteLock implements ReadWriteLock,Serializable{
    
    private static final long serialVersionUID = -7844973186970589013L;
    
    final AbstractSync abstractSync;
    
    private final CustomReadWriteLock.ReentrantReadLock readLock;
    
    private final CustomReadWriteLock.ReentrantWriteLock writeLock;
    
    public CustomReadWriteLock(){
        this(false);
    }
    
    public CustomReadWriteLock(boolean fair){
        abstractSync = fair ? new FairSync() : new NonFairSync();
        readLock = new ReentrantReadLock(abstractSync);
        writeLock = new ReentrantWriteLock(abstractSync);
    }
    
    @Override
    public ReentrantWriteLock writeLock(){
        return this.writeLock;
    }
    
    @Override
    public ReentrantReadLock readLock(){
        return this.readLock;
    }
    
    private abstract static class AbstractSync extends AbstractQueuedSynchronizer{
        
        private static final long serialVersionUID = -2180798633936565228L;
    
        /**
         * 锁相关常量定义
         * AQS依然使用int类型state属性记录锁的状态
         * int类型数据4字节32位，读写锁采用高16位记录读锁状态，低16位记录写锁状态
         * SHARED_SHIFTL：理解为平分的节点
         * SHARED_UNIT  ：读锁采用高16位计数，所以读锁加锁需要从左移16位开始
         * MAX_COUNT    ：锁的最大数量值
         * EXCLUSIVE_MASK ： 统计独占锁数量数量时使用&运算使用，该数值等于 1111_1111_1111_1111
         */
        static final int SHARED_SHIFT    = 16;
        static final int SHARED_UNIT     = (1 << SHARED_SHIFT);
        static final int MAX_COUNT       = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK  = (1 << SHARED_SHIFT) - 1;
    
        /**
         * 统计读、写锁数量的两个方法
         * 写锁采用高16位计数，所以将高16位直接右移16位后统计的数据就是写锁数值
         * 读锁采用低十六计数，所以将EXCLUSIVE_MASK这一常量与state属性做&运算就可以得到写锁数量
         * 采用的参数c其实都是传递的目前锁状态属性值state
         */
        static int sharedCount(int c){
            return c >>> SHARED_SHIFT;
        }
    
        /**
         * 假设state为3转二进制    ：0000_0000_0000_0000_0000_0000_0000_0011
         * 与EXCLUSIVE_MASK做&计算 ：0000_0000_0000_0000_1111_1111_1111_1111
         * 最后结果                ：0000_0000_0000_0000_0000_0000_0000_ 0011
         * 结论：与EXCLUSIVE_MASK常量做&运算就是保留低十六位的数据
         */
        static int exclusiveCount(int c){
            return c & EXCLUSIVE_MASK;
        }
    
        /**
         * 两个类总结起来就是使用线程私有量ThreadLock达到记录线程读锁数量的效果
         * ReentraantReadWriteLock中的实现涉及到本地方法，所以这里阐述一下两个类作用
         * 后面直接使用ThreadLocal实现
         */
        static final class HoldCounter{}
        static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter>{}
    
        /**
         * 两个方法分别是描述读、写锁线程加锁是否应该被阻塞
         * 因为公平锁实现和非公平锁实现的触达条件不一致，交给子类进行具体实现
         * 写锁阻塞：
         * @see NonFairSync#writerShouldBlock  非公平实现肯定不需要判断是否为SyncQueue头结点，所以永远返回false
         * @see FairSync#writerShouldBlock     公平实现需要判断当前线程是否为SyncQueue队列头结点，使用AQS提供方法hasQueuedPredecessors()
         *
         * 读锁阻塞：
         * @see NonFairSync#readerShouldBlock 非公平实现需要调用AQS中的apparentlyFirstQueuedIsExclusive()
         * @see FairSync#readerShouldBlock 公平实现依然借助于AQS中的hasQueuedPredecessors()判断
         */
        abstract boolean readerShouldBlock();
        abstract boolean writerShouldBlock();
    
        /**
         *  读写锁中对于独占锁写锁的基本方法tryAcquire()的实现与{@link com.zsl.lock.reentrant.CustomReentrantLock}中实现
         *  多了独占锁写锁加锁时不能存在任何读锁的判断，因为锁不能降级
         *
         */
        @Override
        protected boolean tryAcquire (int count) {
            int state = super.getState();
            Thread currentThread = Thread.currentThread();
    
            /**
             * state不等于0表示有锁
             * 如果是读锁，则不能加写锁。读写锁的规则为读锁不能升级为写锁、写锁可以降级为读锁
             * 如果读写锁都存在(先加写锁后再加读锁)，则需要判断重入锁条件
             * currentThread != getExclusiveOwnerThread()的判断如果当前线程不是持有锁线程则返回
             * 如果是当前持有锁线程进行下面加锁操作，所以加锁用setState()也是线程安全的
             */
            if (state != 0){
                // 获取写锁数量
                int writeCount = exclusiveCount(state);
                if (writeCount == 0 || currentThread != getExclusiveOwnerThread()){
                    return false;
                }
                // 判断加锁数量
                int nowWriteCount = count + exclusiveCount(state);
                if (nowWriteCount > MAX_COUNT){
                    throw new Error("写锁数量超出最大值");
                }
                super.setState(state+count);
            }
            if (!super.compareAndSetState(0,state+count)){
                return false;
            }
            super.setExclusiveOwnerThread(currentThread);
            return true;
        }
    
        /**
         * 尝试性加写锁，与{@link #tryAcquire}实现效果一致，不清楚为嘛源码里面还会专门实现这样的方法
         */
        protected boolean tryWriteLock(){
            int state = super.getState();
            Thread currentThread = Thread.currentThread();
            if (state != 0){
                int writeCount = exclusiveCount(state);
                if (writeCount == 0 || currentThread != super.getExclusiveOwnerThread()){
                    return false;
                }
                int nowWriteCount = exclusiveCount(state) + 1;
                if (nowWriteCount > MAX_COUNT){
                    throw new Error("写锁数量超过最大值");
                }
            }
            if (!compareAndSetState(0,state+1)){
                return false;
            }
            super.setExclusiveOwnerThread(currentThread);
            return true;
        }
    
        /**
         * @see com.zsl.lock.reentrant.CustomReentrantLock.AbstractSync#tryRelease 实现与这个方法实现逻辑一致
         * 唯一多的操作就是在获取当前写锁数量的时候需要使用{@link #exclusiveCount}处理
         */
        @Override
        protected boolean tryRelease (int count) {
            Thread currentThread = Thread.currentThread();
            if (currentThread != super.getExclusiveOwnerThread()){
                throw new IllegalMonitorStateException("当前线程不是持有锁线程");
            }
            int nowCount = getState() - count;
            boolean release = exclusiveCount(nowCount) == 0;
            if (release){
                super.setExclusiveOwnerThread(null);
            }
            super.setState(nowCount);
            return release;
        }
        
        protected ConditionObject newCondition(){
            return new ConditionObject();
        }
    }
    
    private static class NonFairSync extends AbstractSync{
        
        private static final long serialVersionUID = -7921132809014261123L;
    
        @Override
        boolean readerShouldBlock () {
            return false;
        }
    
        @Override
        boolean writerShouldBlock () {
            return false;
        }
    }
    
    private static class FairSync extends AbstractSync{
        private static final long serialVersionUID = -916928599598925649L;
    
        @Override
        boolean readerShouldBlock () {
            return super.hasQueuedPredecessors();
        }
    
        @Override
        boolean writerShouldBlock () {
            return super.hasQueuedPredecessors();
        }
    }
    
    private static class  ReentrantReadLock implements Lock, Serializable{
        
        private static final long serialVersionUID = 6576663568189405457L;
        
        private final AbstractSync abstractSync;
        
        private ReentrantReadLock(AbstractSync sync){
            this.abstractSync = sync;
        }
    
        @Override
        public void lock () {
        
        }
    
        @Override
        public void lockInterruptibly () throws InterruptedException {
        
        }
    
        @Override
        public boolean tryLock () {
            return false;
        }
    
        @Override
        public boolean tryLock (long time, TimeUnit unit) throws InterruptedException {
            return false;
        }
    
        @Override
        public void unlock () {
        
        }
    
        @Override
        public Condition newCondition () {
            return null;
        }
    }
    
    private static class ReentrantWriteLock implements Lock,Serializable{
        
        private static final long serialVersionUID = 943033369703714242L;
        
        private final AbstractSync abstractSync;
        
        private ReentrantWriteLock(AbstractSync sync){
            this.abstractSync = sync;
        }
    
        @Override
        public void lock () {
            this.abstractSync.acquire(1);
        }
    
        @Override
        public void lockInterruptibly () throws InterruptedException {
            this.abstractSync.acquireInterruptibly(1);
        }
    
        @Override
        public boolean tryLock () {
            return this.abstractSync.tryWriteLock();
        }
    
        @Override
        public boolean tryLock (long time, TimeUnit unit) throws InterruptedException {
            return this.abstractSync.tryAcquireNanos(1,unit.toNanos(time));
        }
    
        @Override
        public void unlock () {
            this.abstractSync.tryRelease(1);
        }
    
        @Override
        public Condition newCondition () {
            return this.abstractSync.newCondition();
        }
    }
    
}
