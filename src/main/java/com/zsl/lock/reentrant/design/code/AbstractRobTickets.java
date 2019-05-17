package com.zsl.lock.reentrant.design.code;

import lombok.Getter;
import lombok.Setter;

/**
 * TODO �����û���Ʊ�࣬ʵ�ֹ����û����ݲ��֣���װ������Ʊģ�巽��
 * @author zsl
 * @version 1.0
 * @date: 2019/5/17 13:23
 **/
public abstract class AbstractRobTickets implements RobTicketsDate,RobTicketsBehavior{
    
    /**
     * ��¼��Ʊ���ٵ��ٶ�
     */
    @Getter
    @Setter
    private int accelerateLevel;
    
    @Override
    public void intoWaterConsumption () {
        System.out.println("���û���������ˮ��¼������ˮ��");
    }
    
    @Override
    public void ticketRecord () {
        System.out.println("����Ʊ��¼�������ݿ�");
    }
    
    @Override
    public void newPoints () {
        System.out.println("�����ͻ��ּ�¼�������ݿ�");
    }
    
    /**
     * TODO ��װ��Ʊ����
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
