package com.hyunwoo.fasdmodule;

import java.util.*;

public class IntervalLeaner {
    private HashMap<String, Long[]> previousMap = new HashMap<>();
    private HashMap<Long, HashMap<Long, Double[]>> timeTable = new HashMap<>();
    private ArrayList<String[]> logList = new ArrayList<>();
    private String userID;
    private Long timestamp, cctvID;
    private Long lowCCTV, highCCTV;
    private Double gap, mean, std, count, new_mean, new_std, new_count;

    public IntervalLeaner(){}

    public ArrayList<String[]> getLogList(){
        return logList;
    }

    public HashMap<Long, HashMap<Long, Double[]>> getTimeTable(){
        return timeTable;
    }


    public void add(String[] data) throws Exception{
        // raw data schema : [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]
        userID = data[3];
        cctvID = Long.parseLong(data[1]);
        timestamp = Long.parseLong(data[2]);

        if (!previousMap.containsKey(userID)){
            previousMap.put(userID, new Long[]{cctvID, timestamp});

        }else{
            if(previousMap.get(userID)[0].equals(cctvID)){
                // 같은 카메라의 detection 정보일 경우
                // TODO : 기준 정하기, 현재는 1분
                if (timestamp-previousMap.get(userID)[1] >= 1000*60){
                    // 다른 곳에 들렸다 오는 경우(이전 카메라를 나온 후 너무 오랜 후에 등장할 경우)
                    previousMap.replace(userID, new Long[]{cctvID, timestamp});
                }

            }else {
                // 다른 카메라의 detection 정보일 경우
                // TODO : 기준 정하기, 현재는 1분
                if (timestamp-previousMap.get(userID)[1]>=1000*60){
                    // 다른 곳에 들렸다 오는 경우(이전 카메라를 나온 후 너무 오랜 후에 등장할 경우)
                    previousMap.replace(userID, new Long[]{cctvID, timestamp});

                }else {
                    // 이전 카메라 이후 바로 등장한 경우
                    addToTable(previousMap.get(userID), cctvID, timestamp);
                    previousMap.replace(userID, new Long[]{cctvID, timestamp});
                }
            }
        }
        logList.add(data);
    }

    public void addToTable(Long[] previous, Long current_cctvID, Long current_timestamp){
        lowCCTV = Math.min(previous[0], current_cctvID);
        highCCTV = Math.max(previous[0], current_cctvID);
        gap = current_timestamp-previous[1]+0.0;

        if(!timeTable.containsKey(lowCCTV)){
            // camera1이 등록되어있지 않을 경우
            timeTable.put(lowCCTV, new HashMap<>());
            timeTable.get(lowCCTV).put(highCCTV, new Double[]{gap, 0.0, 1.0});

        }else{
            // camera1가 등록되어있을 경우
            if (!timeTable.get(lowCCTV).containsKey(highCCTV)){
                // camera2가 등록되어있지 않을 경우
                timeTable.get(lowCCTV).put(highCCTV, new Double[]{gap, 0.0, 1.0});
            }else {
                // camera2도 등록되어있을 경우
                mean = timeTable.get(lowCCTV).get(highCCTV)[0];
                std = timeTable.get(lowCCTV).get(highCCTV)[1];
                count = timeTable.get(lowCCTV).get(highCCTV)[2];

                new_mean = (gap+(mean*count))/(count+1);
                new_std = Math.sqrt(((std*std + mean*mean)*count + gap*gap)/(count+1) - new_mean*new_mean);
                new_count = count+1;
                timeTable.get(lowCCTV).replace(highCCTV, new Double[]{new_mean, new_std, new_count});
            }
        }
    }

    public HashMap<Long, Double[]> makeLongestTimeTable(){
        HashMap<Long, Double[]> longTimeTable = new HashMap<>();
        HashSet<Long> keySet = new HashSet<>();

        // camera set & initialize timetable
        for (Long low : this.timeTable.keySet()){
            if (!keySet.contains(low)){
                keySet.add(low);
                longTimeTable.put(low, new Double[]{0.0, 0.0, 0.0});
            }

            for (Long high : this.timeTable.get(low).keySet()){
                if (!keySet.contains(high)){
                    keySet.add(high);
                    longTimeTable.put(high, new Double[]{0.0, 0.0, 0.0});
                }
            }
        }

        // longest timetable
        for (Long low : this.timeTable.keySet()){
            for (Long high : this.timeTable.get(low).keySet()){
                if(longTimeTable.keySet().contains(low) && longTimeTable.get(low)[0] < this.timeTable.get(low).get(high)[0]){
                    longTimeTable.replace(low, this.timeTable.get(low).get(high));
                }

                if(longTimeTable.keySet().contains(high) && longTimeTable.get(high)[0] < this.timeTable.get(low).get(high)[0]){
                    longTimeTable.replace(high, this.timeTable.get(low).get(high));
                }
            }
        }

        return longTimeTable;
    }
}
