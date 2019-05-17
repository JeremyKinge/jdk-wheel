package com.zsl.lock.reentrant_lock.design_practice.code;

/**
 * TODO  定义客户抢票行为操作
 * @author zsl
 * @version 1.0
 * @date: 2019/5/17 13:14
 **/
public interface RobTicketsBehavior {
    
    /**
     * 抢票
     */
    boolean robTickets();
    
    /**
     * 抢票加速
     */
    void speedUp();
    
    /**
     * 赠送积分
     */
    void givingIntegral();
    
}
