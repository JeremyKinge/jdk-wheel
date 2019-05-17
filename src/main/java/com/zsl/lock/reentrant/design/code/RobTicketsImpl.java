package com.zsl.lock.reentrant.design.code;

/**
 * TODO 用户抢票实现
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
     * 免费VIP实现
     */
    class FreeVip extends AbstractRobTickets implements RobTicketsBehavior{
    
        @Override
        public boolean robTickets () {
            System.out.println("使用免费VIP等级速度加上抢票加速等级" + super.getAccelerateLevel()+"进行抢票");
            return false;
        }
    
        @Override
        public void speedUp () {
            System.out.println("使用免费VIP的方式进行加速");
            int accelerateLevel = super.getAccelerateLevel();
            super.setAccelerateLevel(accelerateLevel + 1);
        }
    
        @Override
        public void givingIntegral () {
            System.out.println("计算出免费VIP的赠送积分");
            this.newPoints();
        }
    }
    
    /**
     * 花钱VIP实现
     */
    class SpendMoneyVip extends AbstractRobTickets implements RobTicketsBehavior{
        
        @Override
        public boolean robTickets () {
            System.out.println("使用花钱VIP等级速度加上抢票加速等级" + super.getAccelerateLevel()+"进行抢票");
            return false;
        }
        
        @Override
        public void speedUp () {
            System.out.println("使用花钱VIP的方式进行加速");
            int accelerateLevel = super.getAccelerateLevel();
            super.setAccelerateLevel(accelerateLevel + 2);
        }
        
        @Override
        public void givingIntegral () {
            System.out.println("计算出花钱VIP的赠送积分");
            this.newPoints();
        }
    }
    
    /**
     * TODO 对外提供抢票访问方法
     * @author zsl
     * @date 2019/5/17 13:56
     **/
    public void doRobTickets(){
        this.art.robTicketTemplate();
    }


}
