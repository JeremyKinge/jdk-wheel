package com.zsl.lock.reentrant_lock.design_practice.code;

import lombok.Getter;
import lombok.Setter;

/**
 * TODO 抽象用户抢票类，实现公共用户数据部分，组装完整抢票模板方法
 * @author zsl
 * @version 1.0
 * @date: 2019/5/17 13:23
 **/
public abstract class AbstractRobTickets implements RobTicketsDate,RobTicketsBehavior{
    
    /**
     * 记录抢票加速的速度
     */
    @Getter
    @Setter
    private int accelerateLevel;
    
    @Override
    public void intoWaterConsumption () {
        System.out.println("将用户的消费流水记录进入流水表");
    }
    
    @Override
    public void ticketRecord () {
        System.out.println("将购票记录记入数据库");
    }
    
    @Override
    public void newPoints () {
        System.out.println("将赠送积分记录进入数据库");
    }
    
    /**
     * TODO 组装抢票方法
     * @author zsl
     * @date 2019/5/17 13:32
     * @return void
     **/
    public void robTicketTemplate(){
        if (this.robTickets()){
            intoWaterConsumption();
            givingIntegral();
            ticketRecord();
        }
    }
    
}
