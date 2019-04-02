package com.hyunwoo.fasdmodule;

import java.util.Date;
import java.util.List;

public class TransactionChecker implements Runnable{
    private List<FASDModule_profile> child_lst;

    @Override
    public void run() {
        // TODO : 기준 정하기, 현재는 10분
        Date finishDate;
        Long fDate;
        while (true){
            try {
                Thread.sleep(1000*60*10);
            }catch (Exception e){
                e.printStackTrace();
            }

            finishDate = new Date();
            fDate = finishDate.getTime();

            for (FASDModule_profile module : child_lst){
                module.trackingTransaction(fDate);
            }
        }
    }

    public void setModuleList(List<FASDModule_profile> child_lst){
        this.child_lst = child_lst;
    }

}
