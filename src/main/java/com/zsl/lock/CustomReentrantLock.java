package com.zsl.lock;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO ʵ��ReentrantLock��
 * @author zsl
 * @version 1.0
 * @date: 2019/5/16 15:00
 **/
public class CustomReentrantLock implements Lock, Serializable {
    
    /**
     * ���л��汾��
     * IDEAĬ�϶���ʵ��Serialize�ӿ�û��serialVersionUID�����޾���
     * File -> settings -> Editor -> Inspections -> Serialization issues -> Serialization class without 'serialVersionUID'
     */
    private static final long serialVersionUID = 5620081294534636810L;
    
    /**
     * ���Ĺ�ƽʵ����ǹ�ƽʵ��ʵ��
     */
    private final AbstractSync abstractSync;
    
    /**
     * Ĭ���ṩ�ǹ�ƽʵ��
     */
    public CustomReentrantLock(){
        this(Boolean.FALSE);
    }
    
    /**
     * ���ݲ���fairʵ����CustomReentrantLock,����abstractSync��ֵ
     * @param fair {@code true) ʵ�ֹ�ƽ�� {@code false} ʵ�ַǹ�ƽ��
     */
    public CustomReentrantLock(boolean fair){
        abstractSync = fair ?  new FairSync() :  new NonFairSync();
    }
    
    abstract static class AbstractSync extends AbstractQueuedSynchronizer{
        
        private static final long serialVersionUID = -8925767597015408078L;
        
        /**
         * TODO ���ⲿ���ṩ�������������װ����ʵ�֣������ⲿ�����ʵ��ϸ��
         * @author zsl
         * @date 2019/5/16 19:26
         * @return void
         **/
        abstract void lock();
    
        /**
         * TODO ʵ�ֶ�ռ���ķǹ�ƽ����
         * @author zsl
         * @date 2019/5/16 16:46
         * @param count ��������
         * @return boolean
         **/
        protected boolean nonfairTryAcquire (int count) {
             Thread currentThread = Thread.currentThread();
             
             /**
              *  ��ȡ������״̬��AQS��ʹ��int��������state���棬������ʹ��volatile�ؼ������α�֤�߳̿ɼ���
              *  ��ʵ�ֵĹؼ���������AQS�ṩ��compareAndSetState()��������CASԭ���滻state
              */
             
             int state = super.getState();
             
             /**
               * state����0��ʾδ����
               * compareAndSetState()�������ɹ�����setExclusiveOwnerThread()���õ�ǰ�������߳�
               * AQS�̳г�����AbstractOwnableSynchronizer��setExclusiveOwnerThread()�ɸ���ʵ��
              */
             
             if (state == 0){
                 if (super.compareAndSetState(0,count)){
                     setExclusiveOwnerThread(currentThread);
                     return true;
                 }
             }
             
             /**
               * ��������ƣ��������߳��ظ�����ʱ�������״̬��־state
               * ���������ͷ�Ҳ��Ҫ����ͷţ�ֱ����state����
             */
             
             /**
               * state��������Ϊint�������ֵΪ2147483647�������ټӾͻ��Ϊ����
               * ���ֵ+1��Ϊ������ԭ������Ϊ��������ò���洢
               * ����������ԭ��һ��
               * ����ԭ��ת������  ���ȡ������һ������λ����1
               * ����תԭ����      ��һ�����ȡ��
             */
             
             /**
               * 2147483647���룺0111_1111_1111_1111_1111_1111_1111_1111
               * 1����         : 0000_0000_0000_0000_0000_0000_0000_0001
               * ��Ӳ���      ��1000_0000_0000_0000_0000_0000_0000_0000
               * ԭ��          ��1000_0000_0000_0000_0000_0000_0000_0000
               * ʮ������      ��-2147483648
              */
             
             if (currentThread == super.getExclusiveOwnerThread()){
                int newState = state + count;
                if (newState < 0){
                    throw new Error("�������������");
                }
                super.setState(newState);
                return true;
             }
             return false;
        }
    
        /**
         * TODO ʵ�ֶ�ռ����ƽ����
         * @author zsl
         * @date 2019/5/16 18:47
         * @param count ��������
         * @return boolean
         **/
        protected boolean fairTryAcquire (int count) {
            Thread currentThread = Thread.currentThread();
            int state = super.getState();
    
            /**
              * ��ƽ����ǹ�ƽ�������������һ������hasQueuedPredecessors()�ĵ��ã��ɸ���AbstractSynchronizerʵ���ṩ
              * �����������жϵ�ǰ�߳��Ƿ�Ϊ�ȴ����е�һ�߳�
              * ������������ʵ����Ʋ�û���κ��޸�
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
                    throw new Error("���������������ֵ");
                }
                super.setState(newState);
                return true;
            }
            return false;
        }
    
        /**
         * TODO AQS�ͷ���ģ�巽��release()���õĻ��������ͷ���ʵ��
         * @author zsl
         * @date 2019/5/16 19:51
         * @param count �ͷ���������
         * @return boolean
         **/
        @Override
        protected boolean tryRelease (int count) {
            int newState = super.getState() - count;
            boolean release = false;
            Thread currentThread = Thread.currentThread();
    
            /**
              * �жϴ��뽫�������߳��뵱ǰ�߳̽��бȶ�������Ч��
              * 1.�淶�����ڳ�������ʱ������ͷ�������ֹ�������ͷ�
              * 2.��֤����״̬state����С��0,��state����0�ǻὫ�������߳���Ϊ�գ������жϾͻ��׳��쳣
             */
            
            if (currentThread != super.getExclusiveOwnerThread()){
                throw new IllegalMonitorStateException("�ͷ������̷߳ǵ�ǰ�������߳�");
            }
            if (newState == 0){
                release = true;
                super.setExclusiveOwnerThread(null);
            }
            super.setState(newState);
            return release;
        }
    
        /**
         * TODO ��ȡCondition����
         * @author zsl
         * @date 2019/5/16 20:25
         * @return java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject
         **/
        final ConditionObject newCondition() {
    
            /**
              * ConditionObject��AQS��һ��ʵ���ڲ���
              * Condition����һ���漰��await()��signal()
              * Condition�Ĳ����������ڻ�ȡ���Ļ����Ͻ��в���
              * await() -> fullyRelease() -> release() -> tryRelease()������tryRealse()�Էǵ�ǰ�������̻߳��׳��쳣
             */
            return new ConditionObject();
        }
    }
    
    private static class NonFairSync extends AbstractSync{
        
        private static final long serialVersionUID = 6386273947522327876L;
    
        /**
         * TODO ����AbstractSync�зǹ�ƽ��ռ����ʵ����װ����AbstractQueuedSynchronizedģ�巽��acquire()�Ļ�������tryAcquire()
         * @author zsl
         * @date 2019/5/16 19:21
         * @param count ��������
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
              *����ڹ�ƽʵ�ֶ��Զ���һ�����ĳ��Ի�ȡ
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
         * TODO ��ǹ�ƽʵ��һ��
         * @author zsl
         * @date 2019/5/16 19:21
         * @param count ��������
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
     * TODO �߳��ж����л�ȡ��
     * @author zsl
     * @date 2019/5/16 20:02
     * @return void
     * @exception InterruptedException �߳��ж������쳣
     **/
    @Override
    public void lockInterruptibly () throws InterruptedException {
        /**
          * acquireInterruptibly()��AQS�ṩ���߳��ж����л�ȡ����ģ�巽��
          * �߳��ж����������������ط���
          * 1. ���뷽�����ȵ���interrupted()�ж��߳��ж�״̬��־
          * 2. ���߳�����������ȡ��Ҳ����doAcquireInterruptibly()���ж��߳��ж�״̬��־
         */
        this.abstractSync.acquireInterruptibly(1);
    }
    
    /**
     * TODO �����ԵĻ�ȡ����Ҳ�����̲߳��������ȴ�����Դ
     * @author zsl
     * @date 2019/5/16 19:59
     * @return boolean
     **/
    @Override
    public boolean tryLock () {
        return this.abstractSync.nonfairTryAcquire(1);
    }
    
    /**
     * TODO ��time�޶�ʱ���ڳ��Ի�ȡ����Դ
     * @author zsl
     * @date 2019/5/16 20:12
     * @param  time ���Ի�ȡ���ȴ�ʱ�䳤�� unit�ȴ�ʱ�䵥λ����
     * @return boolean
     * @exception InterruptedException �߳��ж������쳣
     **/
    @Override
    public boolean tryLock (long time, TimeUnit unit) throws InterruptedException {
        /**
          * �޶�ʱ���ڳ��Ի�ȡ����Դ��{@link #lockInterruptibly}���л�ȡ��һ�¶����ڷ���ǰ�������������������ж�
          * �޶�ʱ��Ҳ�����޶�����������ʱ�����������´����ж�
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

