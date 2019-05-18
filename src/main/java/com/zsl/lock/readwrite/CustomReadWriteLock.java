package com.zsl.lock.readwrite;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;


/**
 * ��νṹ��������ϮLock����ʵ��ϸ�ڲ���AbstractSync������ʵ��������������ʵ�ֹ�ƽ��ǹ�ƽ����
 * �ڲ���ReentrantReadLock������ǹ������Ķ�����ReentrantWriteLock�����ռ��д��
 * ����CSDN���ͣ�<a>https://blog.csdn.net/weixin_43495590/article/details/90172914</a>
 */

/**
 * TODO ʵ��ReentrantReadWriteLock��д��
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
         * ����س�������
         * AQS��Ȼʹ��int����state���Լ�¼����״̬
         * int��������4�ֽ�32λ����д�����ø�16λ��¼����״̬����16λ��¼д��״̬
         * SHARED_SHIFTL�����Ϊƽ�ֵĽڵ�
         * SHARED_UNIT  ���������ø�16λ���������Զ���������Ҫ������16λ��ʼ
         * MAX_COUNT    �������������ֵ
         * EXCLUSIVE_MASK �� ͳ�ƶ�ռ����������ʱʹ��&����ʹ�ã�����ֵ���� 1111_1111_1111_1111
         */
        static final int SHARED_SHIFT    = 16;
        static final int SHARED_UNIT     = (1 << SHARED_SHIFT);
        static final int MAX_COUNT       = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK  = (1 << SHARED_SHIFT) - 1;
    
        /**
         * ͳ�ƶ���д����������������
         * д�����ø�16λ���������Խ���16λֱ������16λ��ͳ�Ƶ����ݾ���д����ֵ
         * �������õ�ʮ�����������Խ�EXCLUSIVE_MASK��һ������state������&����Ϳ��Եõ�д������
         * ���õĲ���c��ʵ���Ǵ��ݵ�Ŀǰ��״̬����ֵstate
         */
        static int sharedCount(int c){
            return c >>> SHARED_SHIFT;
        }
    
        /**
         * ����stateΪ3ת������    ��0000_0000_0000_0000_0000_0000_0000_0011
         * ��EXCLUSIVE_MASK��&���� ��0000_0000_0000_0000_1111_1111_1111_1111
         * �����                ��0000_0000_0000_0000_0000_0000_0000_ 0011
         * ���ۣ���EXCLUSIVE_MASK������&������Ǳ�����ʮ��λ������
         */
        static int exclusiveCount(int c){
            return c & EXCLUSIVE_MASK;
        }
    
        /**
         * �������ܽ���������ʹ���߳�˽����ThreadLock�ﵽ��¼�̶߳���������Ч��
         * ReentraantReadWriteLock�е�ʵ���漰�����ط����������������һ������������
         * ����ֱ��ʹ��ThreadLocalʵ��
         */
        static final class HoldCounter{}
        static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter>{}
    
        /**
         * ���������ֱ�����������д���̼߳����Ƿ�Ӧ�ñ�����
         * ��Ϊ��ƽ��ʵ�ֺͷǹ�ƽ��ʵ�ֵĴ���������һ�£�����������о���ʵ��
         * д��������
         * @see NonFairSync#writerShouldBlock  �ǹ�ƽʵ�ֿ϶�����Ҫ�ж��Ƿ�ΪSyncQueueͷ��㣬������Զ����false
         * @see FairSync#writerShouldBlock     ��ƽʵ����Ҫ�жϵ�ǰ�߳��Ƿ�ΪSyncQueue����ͷ��㣬ʹ��AQS�ṩ����hasQueuedPredecessors()
         *
         * ����������
         * @see NonFairSync#readerShouldBlock �ǹ�ƽʵ����Ҫ����AQS�е�apparentlyFirstQueuedIsExclusive()
         * @see FairSync#readerShouldBlock ��ƽʵ����Ȼ������AQS�е�hasQueuedPredecessors()�ж�
         */
        abstract boolean readerShouldBlock();
        abstract boolean writerShouldBlock();
    
        /**
         *  ��д���ж��ڶ�ռ��д���Ļ�������tryAcquire()��ʵ����{@link com.zsl.lock.reentrant.CustomReentrantLock}��ʵ��
         *  ���˶�ռ��д������ʱ���ܴ����κζ������жϣ���Ϊ�����ܽ���
         *
         */
        @Override
        protected boolean tryAcquire (int count) {
            int state = super.getState();
            Thread currentThread = Thread.currentThread();
    
            /**
             * state������0��ʾ����
             * ����Ƕ��������ܼ�д������д���Ĺ���Ϊ������������Ϊд����д�����Խ���Ϊ����
             * �����д��������(�ȼ�д�����ټӶ���)������Ҫ�ж�����������
             * currentThread != getExclusiveOwnerThread()���ж������ǰ�̲߳��ǳ������߳��򷵻�
             * ����ǵ�ǰ�������߳̽�������������������Լ�����setState()Ҳ���̰߳�ȫ��
             */
            if (state != 0){
                // ��ȡд������
                int writeCount = exclusiveCount(state);
                if (writeCount == 0 || currentThread != getExclusiveOwnerThread()){
                    return false;
                }
                // �жϼ�������
                int nowWriteCount = count + exclusiveCount(state);
                if (nowWriteCount > MAX_COUNT){
                    throw new Error("д�������������ֵ");
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
         * �����Լ�д������{@link #tryAcquire}ʵ��Ч��һ�£������Ϊ��Դ�����滹��ר��ʵ�������ķ���
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
                    throw new Error("д�������������ֵ");
                }
            }
            if (!compareAndSetState(0,state+1)){
                return false;
            }
            super.setExclusiveOwnerThread(currentThread);
            return true;
        }
    
        /**
         * @see com.zsl.lock.reentrant.CustomReentrantLock.AbstractSync#tryRelease ʵ�����������ʵ���߼�һ��
         * Ψһ��Ĳ��������ڻ�ȡ��ǰд��������ʱ����Ҫʹ��{@link #exclusiveCount}����
         */
        @Override
        protected boolean tryRelease (int count) {
            Thread currentThread = Thread.currentThread();
            if (currentThread != super.getExclusiveOwnerThread()){
                throw new IllegalMonitorStateException("��ǰ�̲߳��ǳ������߳�");
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
