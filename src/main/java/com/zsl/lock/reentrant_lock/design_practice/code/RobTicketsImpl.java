package com.zsl.lock.reentrant_lock.design_practice.code;

/**
 * TODO �û���Ʊʵ��
 * @author zsl
 * @version 1.0
 * @date: 2019/5/17 13:37
 **/
public class RobTicketsImpl{
    
    private AbstractRobTickets art;
    
    public RobTicketsImpl(){
        this(false);
    }
    
    public RobTicketsImpl(boolean free){
        this.art = free ? new FreeVip() : new SpendMoneyVip();
    }
    
    /**
     * ���VIPʵ��
     */
    class FreeVip extends AbstractRobTickets implements RobTicketsBehavior{
    
        @Override
        public boolean robTickets () {
            System.out.println("ʹ�����VIP�ȼ��ٶȼ�����Ʊ���ٵȼ�" + super.getAccelerateLevel()+"������Ʊ");
            return false;
        }
    
        @Override
        public void speedUp () {
            System.out.println("ʹ�����VIP�ķ�ʽ���м���");
            int accelerateLevel = super.getAccelerateLevel();
            super.setAccelerateLevel(accelerateLevel + 1);
        }
    
        @Override
        public void givingIntegral () {
            System.out.println("��������VIP�����ͻ���");
            this.newPoints();
        }
    }
    
    /**
     * ��ǮVIPʵ��
     */
    class SpendMoneyVip extends AbstractRobTickets implements RobTicketsBehavior{
        
        @Override
        public boolean robTickets () {
            System.out.println("ʹ�û�ǮVIP�ȼ��ٶȼ�����Ʊ���ٵȼ�" + super.getAccelerateLevel()+"������Ʊ");
            return false;
        }
        
        @Override
        public void speedUp () {
            System.out.println("ʹ�û�ǮVIP�ķ�ʽ���м���");
            int accelerateLevel = super.getAccelerateLevel();
            super.setAccelerateLevel(accelerateLevel + 2);
        }
        
        @Override
        public void givingIntegral () {
            System.out.println("�������ǮVIP�����ͻ���");
            this.newPoints();
        }
    }
    
    /**
     * TODO �����ṩ��Ʊ���ʷ���
     * @author zsl
     * @date 2019/5/17 13:56
     **/
    public void doRobTickets(){
        this.art.robTicketTemplate();
    }


}
