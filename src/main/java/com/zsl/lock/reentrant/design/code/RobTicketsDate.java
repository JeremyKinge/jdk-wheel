package com.zsl.lock.reentrant.design.code;

/**
 * TODO ����ͻ���Ʊ���ݲ���
 * @author zsl
 * @version 1.0
 * @date: 2019/5/17 13:07
 **/
public interface RobTicketsDate {
    
    /**
     * �ͻ�������ˮ�������
     */
    void intoWaterConsumption();
    
    /**
     * ��Ʊ��¼���
     */
    void ticketRecord();
    
    /**
     * �������ѻ���
     */
    void newPoints();
    
}
