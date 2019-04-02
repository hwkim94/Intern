package com.hyunwoo.fasdmodule;

import sun.nio.cs.FastCharsetProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class FASDModule_user {
    private HashMap<Integer, FASDModule_profile> childModules = new HashMap<>();
    private DataSender dataSender = new DataSender();
    private HashMap<Long, HashMap<Long, Double[]>> timetable;
    private HashMap<Long, Double[]> longestTable;

    private String userID;
    private String cctvID;
    private int profileID;
    private Boolean result;
    private Date date;
    private int time;

    public FASDModule_user(){}
    public FASDModule_user(String userID, DataSender dataSender, HashMap timetable, HashMap longestTable){
        this.userID = userID;
        this.dataSender = dataSender;
        this.timetable = timetable;
        this.longestTable = longestTable;

        for(int i=1; i<=4; i++){
            childModules.put(i, new FASDModule_profile(this.userID, i, this.dataSender, this.timetable, this.longestTable));
        }
    }

    public void checkDB(String[] rawData){
        profileID = makingProfile(rawData[2]);
        childModules.get(profileID).checkDB(rawData);
    }

    public HashMap<Integer, FASDModule_profile>getChildMap(){
        return childModules;
    }

    public void add(String[] rawData){
        // raw data schema : [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]
        profileID = makingProfile(rawData[2]);
        childModules.get(profileID).add(rawData);

    }

    private int makingProfile(String timestamp){
        date = new Date(Long.parseLong(timestamp));
        time = Integer.parseInt(date.toString().split(" ")[3].split(":")[0]);

        if (7 <=time && time <12){
            // 출근시간
            return 1;
        }else if(12 <= time && time < 18){
            // 점심시간
            return 2;
        }else if (18 <= time && time < 24){
            // 퇴근시간
            return 3;
        }else{
            // 새벽시간
            return 4;
        }
    }
}
