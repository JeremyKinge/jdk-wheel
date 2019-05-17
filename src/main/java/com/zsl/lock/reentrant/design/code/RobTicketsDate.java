package com.zsl.lock.reentrant.design.code;

/**
 * TODO 定义客户抢票数据操作
 * @author zsl
 * @version 1.0
 * @date: 2019/5/17 13:07
 **/
public interface RobTicketsDate {
    
    /**
     * 客户消费流水数据入库
     */
    void intoWaterConsumption();
    
    /**
     * 购票记录入库
     */
    void ticketRecord();
    
    /**
     * 新增消费积分
     */
    void newPoints();
    
}
