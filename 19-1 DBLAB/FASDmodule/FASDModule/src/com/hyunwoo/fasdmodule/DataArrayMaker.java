package com.hyunwoo.fasdmodule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataArrayMaker {
    private static final String FIELD_DELIMETER = "\t";
    private static final String ROW_DELIMETER = "\n";

    private ArrayList<String[]> dataLst = new ArrayList<>();
    private String[] data,linesArray, rawData;

    private String userID;
    private Long timestamp, cctvID;
    private HashMap<String, Long[]> previousMap = new HashMap<>();

    public DataArrayMaker(){}

    public void add(String lines){
        // raw data schema : [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]

        linesArray = lines.split(ROW_DELIMETER);
        for (String line : linesArray) {
            rawData = line.split(FIELD_DELIMETER);
            // TODO : timestamp의 단위가 ms인지 s인지에 따라 설정
            rawData[2] = rawData[2] + "000";

            if(filter(rawData)) {
                dataLst.add(rawData);
            }
        }
    }

    public boolean hasNext(){
        if(dataLst.isEmpty()){
            return false;
        }else {
            return true;
        }
    }

    public String[] next(){
        data = dataLst.get(0);
        dataLst.remove(0);

        return data;
    }

    public boolean filter(String[] data){
        userID = data[3];
        cctvID = Long.parseLong(data[1]);
        timestamp = Long.parseLong(data[2]);

        if (!previousMap.containsKey(userID)){
            previousMap.put(userID, new Long[]{cctvID, timestamp});
            return true;

        }else{
            if(previousMap.get(userID)[0].equals(cctvID)){
                // 같은 카메라의 detection 정보일 경우
                // TODO : 기준 정하기, 현재는 10분
                if (timestamp-previousMap.get(userID)[1]>=1000*60*10){
                    // 다른 곳에 들렸다 오는 경우(이전 카메라를 나온 후 너무 오랜 후에 등장할 경우)
                    previousMap.replace(userID, new Long[]{cctvID, timestamp});
                    return true;
                }else {
                    // 이전 카메라 이후 바로 등장한 경우
                    return false;
                }
            }else {
                // 다른 카메라의 detection 정보일 경우
                previousMap.replace(userID, new Long[]{cctvID, timestamp});
                return true;
            }
        }
    }
}
