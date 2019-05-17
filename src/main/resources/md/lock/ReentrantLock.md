#### һ��ReentrantLockʵ���ܽ�
����CSDN������ReentrantLock���͵�ַ��https://blog.csdn.net/weixin_43495590/article/details/89709633

##### 1.1 ���˼·
Lock�����˼·����ģ�巽�����ģʽ��AQS�з�װ�ṩ��ͬ�������µ�ģ�巽�������ռ�����ж����ж�ռ������ʱ���Ի�ȡ���ȡ�
�����Զ���ʵ��ֻ��Ҫ��д��������tryAcquire()��tryRelease()��

##### 1.2 ���Ļ���
��ʵ�ֵĻ�������AQS�ṩ��int��������state��¼����״̬�����Ӧ�ṩ��ѯ����getState()��ԭ���޸ķ���compareState()��
stateʹ��volatile���α�֤�ɼ��ԣ�CAS������֤ԭ�ӻ����ԣ��ṩ���̰߳�ȫ��ʵ�֡�
���Ļ�ȡ���ͷž��Ƕ�state����ֵ�Ĳ���

##### 1.3 ������
��CustomReentrantLock���Ѿ�ע����������ʵ����ƣ�������������AbstractOwnerSynchronizer�������м�¼�ĵ�ǰ�������߳��뵱ǰ���Ի�ȡ���߳��Ƿ�һ�µ��ж�

##### 1.4 ��ƽ��ǹ�ƽ��
CustomReentrantLock��ģ��ReentrantLockʵ�ֹ�ƽ��ǹ�ƽ������ƣ�AQS���ö��д洢�ȴ�����ȡ���̡߳�
�����������Ƿ�Ϊ��һ���ȴ�����Դ�ڵ���жϾ��ǹ�ƽ����ʵ��
���ڹ�ƽ��ע������Ҫ���ĸ������������ڷǹ�ƽ������

#### ����acquire()����ע��

��ռ����ȡ��ʱ��ʹ�û�������tryAcquire()�����Լ�����ʧ������Ҫ����acquireQueued()�ȴ�����
```java
   public abstract class AbstractQueuedSynchronizer{
    
       public final void acquire(int arg) {
           
           /**
           * AQS�����������У�һ���Ǿ�������Դʱʹ��LockSupport�����Ķ���SyncQueue������һ����Condition�еȴ��̶߳���ConditionQueue
           * addWaiter()����EXCLUSIVE������ռ��״̬���̵߳ȴ����ڵ�
           */          
           if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
               selfInterrupt();
       }
    
       final boolean acquireQueued(final Node node, int arg) {
            boolean failed = true;
            try {
                boolean interrupted = false;
                
                /**
                * NodeΪAQS���ṩ���߳̽ڵ��ڲ��࣬�����ṩ�������������ȴ����߳̽ڵ�״̬
                * CANCELLED 1�������߳��Ѿ���ȡ��
                * SIGNAL -1��ָʾ�����߳���Ҫunpark()��Ҳ�������߳�park()��
                * CONDITIONA -2���߳���Condition�µȴ�
                * PROPAGATE -3�����acquireShared��ʾ����������
                */
                
                /**
                * �������������Լ���ֱ�������ɹ������Ե���tryAcquire()��ǰ���Ƕ���ͷhead�ĺ�̽ڵ㣬�����ͷŵ�ʱ��Ҳ�ỽ��head�ĺ�̽ڵ�
                * shouldParkAfterFailedAcquire()���ݵȴ��߲�ڵ������ж��Ƿ���Ҫʹ��LockSupport���߳��ڵڶ��ν���÷�����״̬���ᱻ�޸�ΪSIGNAL
                * parkAndCheckInterrupt()����LuckSupport��park()���������̲߳�����interrupted()״̬
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
#### ����release()����ע��
```java
public abstract class AbstractQueuedSynchronizer{
    
        public final boolean release(int arg) {
            
            /**
            * �����Զ���ʵ�ֵĻ�������tryRelease()�޸���״̬��־����state
            * ��Ϻ���ж�SyncQueue�������Ƿ񻹴��ڵȴ����̶߳���
            * unparkSuccessor()��Ҫ�������ǵ���LuckSupport��unpark()���ѵ�ǰ�������ڵ����һ�ڵ��߳�
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

 