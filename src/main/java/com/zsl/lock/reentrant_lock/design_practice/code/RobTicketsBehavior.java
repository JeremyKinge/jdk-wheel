package com.zsl.lock.reentrant_lock.design_practice.code;

/**
 * TODO  ����ͻ���Ʊ��Ϊ����
 * @author zsl
 * @version 1.0
 * @date: 2019/5/17 13:14
 **/
public interface RobTicketsBehavior {
    
    /**
     * ��Ʊ
     */
    boolean robTickets();
    
    /**
     * ��Ʊ����
     */
    void speedUp();
    
    /**
     * ���ͻ���
     */
    void givingIntegral();
    
}
