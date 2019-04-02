package com.hyunwoo.fasdmodule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;

public class Main {
    private static final String DB_PROPERTIES_FILE = "db.properties";
    private static final String SYSTEM_PROPERTIES_FILE = "system.properties";

    private static Properties dbProps = new Properties();
    private static Properties systemProps = new Properties();
    private static int COUNT = 0;
    private static Logger log = Logger.getLogger(Main.class.toString());

    public static void main(String[] args) throws IOException, InterruptedException{
        if (args.length != 7) {
            System.out.println("parameters: <STREAM_IP> <STREAM_PORT> <DAY_FOR_TRAIN> <DAY_FOR_TRAIN_INTERVAL> <FPMINER_SERVER> <FPMINER_PORT> <LEAVE_CRITICAL_VALUE>");
            return;
        }

        // -------------------------------------------------------------------------------------
        // -------------------------------------- SETTING --------------------------------------
        // -------------------------------------------------------------------------------------
        dbProps.load(new InputStreamReader(new FileInputStream(new File(DB_PROPERTIES_FILE))));
        systemProps.load(new InputStreamReader(new FileInputStream(new File(SYSTEM_PROPERTIES_FILE))));

        systemProps.setProperty("STREAM_IP", args[0]);
        systemProps.setProperty("STREAM_PORT", args[1]);
        systemProps.setProperty("DAY_FOR_TRAIN", args[2]);
        systemProps.setProperty("DAY_FOR_TRAIN_INTERVAK", args[3]);
        systemProps.setProperty("FPMINER_SERVER", args[4]);
        systemProps.setProperty("FPMINER_PORT", args[5]);
        systemProps.setProperty("LEAVE_CRITICAL_VALUE", args[6]);

        HashMap<Long, HashMap<Long, Double[]>> timeTable = new HashMap<>();
        HashMap<Long, Double[]> longestTimeTable = new HashMap<>();
        HashMap<String, FASDModule_user> userMap = new HashMap<>();
        DataSender dataSender = new DataSender();

        HashMap<Integer, FASDModule_profile> childMap;
        List<FASDModule_profile> childList = new ArrayList<>();

        String[] data;
        String userID;
        Long sDate = 0L;
        Long nDate = 0L;



        // ---------------------------------------------------------------------------------------
        // -------------------------------------- READ DATA --------------------------------------
        // ---------------------------------------------------------------------------------------
        DataArrayMaker maker = new DataArrayMaker();
        DataReader dataReader = new DataReader(maker);
        dataReader.start();



        // ----------------------------------------------------------------------------------------
        // -------------------------------------- TRAIN MODE --------------------------------------
        // ----------------------------------------------------------------------------------------
        log.info("[ModeLog] ------------------- Start Train Mode -------------------");
        IntervalLeaner leaner = new IntervalLeaner();
        boolean finishIntervalLearner = false;
        boolean getFirstTimestamp = false;

        while (true){

            // data array schema : [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]
            if(!maker.hasNext()){
                // 새로운 데이터가 없을 경우
                log.info("[Train mode] Waiting for User Appearance Space Log...");
                Thread.sleep(1000);

            }else {
                // 새로운 데이터가 있을 경우
                data = maker.next();
                if(!finishIntervalLearner){
                    // send to IntervalLearner
                    try {
                        leaner.add(data);
                        nDate = Long.parseLong(data[2]);
                        log.info("[Train mode] Data is stacked for calculate camera interval");

                        if (!getFirstTimestamp) {
                            sDate = Long.parseLong(data[2]);
                            getFirstTimestamp = true;
                        }
                    }catch (Exception e) {
                        System.out.println("Error-A");
                    }

                } else {
                    // send to FASDModule_user for collect train data
                    try {
                        userID = data[3];
                        if (!userMap.containsKey(userID)) {
                            userMap.put(userID, new FASDModule_user(userID, dataSender, timeTable, longestTimeTable));
                        }

                        userMap.get(userID).add(data);
                        nDate = Long.parseLong(data[2]);
                        log.info("[Train mode] Data is stacked");
                    }catch (Exception e){
                        System.out.println("Error-B");
                    }
                }
            }


            // finish IntervalLearner after <DAY_FOR_TRAIN_INTERVAL>
            if(!finishIntervalLearner && (nDate-sDate)/(1000.0*60.0*60.0*24) >= Long.parseLong(systemProps.getProperty("DAY_FOR_TRAIN_INTERVAL"))){
                log.info("[Training] ------------------- Start Interval Learning -------------------");
                finishIntervalLearner = true;

                // get IntervalTable
                timeTable = leaner.getTimeTable();
                longestTimeTable = leaner.makeLongestTimeTable();

                // IntervalLearner에 쌓아뒀던 데이터를 FASDModule_user에 넣어줌
                for (String[] stacked_data : leaner.getLogList()){
                    userID = stacked_data[3];
                    if (!userMap.containsKey(userID)) {
                        userMap.put(userID, new FASDModule_user(userID, dataSender, timeTable, longestTimeTable));
                    }
                    userMap.get(userID).add(stacked_data);
                }
                log.info("[Training] ------------------- Start Stacking -------------------");
            }

            // break while-loop after <DAY_FOR_TRAIN>
            // after <DAY_FOR_TRAIN>, pass transaction to FPMiner
            if((nDate-sDate)/(1000.0*60.0*60.0*24)>= Long.parseLong(systemProps.getProperty("DAY_FOR_TRAIN"))){
                log.info("[Training] ------------------- Start FP-MINER -------------------");
                for(String key : userMap.keySet()){
                    childMap = userMap.get(key).getChildMap();
                    for(int key2 : childMap.keySet()){
                        childMap.get(key2).runMiner();
                        childList.add(childMap.get(key2));
                        COUNT +=1 ;
                    }
                }

                log.info("[Training] ------------------- Train is finished -------------------");
                break;
            }
        }

        // 학습된 결과 출력
        log.info("[Training] ------------------- Timetable -------------------");
        for(Long row : timeTable.keySet()) {
            for (Long high : timeTable.get(row).keySet()) {
                System.out.println( row + " " + high + "  " + timeTable.get(row).get(high)[0] + "  " + timeTable.get(row).get(high)[1] + "  " + timeTable.get(row).get(high)[2]);
            }
        }

        log.info("[Training] ------------------- Longest table -------------------");
        for(Long key : longestTimeTable.keySet()) {
            System.out.println(key + " " + longestTimeTable.get(key)[0] + " " + longestTimeTable.get(key)[1] + " " + longestTimeTable.get(key)[2]);
        }



        // -----------------------------------------------------------------------------------------
        // ------------------------------------ MONITORING MODE ------------------------------------
        // -----------------------------------------------------------------------------------------
        log.info("[ModeLog] ------------------- Start Monitoring Mode -------------------");
        TransactionChecker transactionChecker = new TransactionChecker();
        transactionChecker.setModuleList(childList);
        Thread checker = new Thread(transactionChecker);

        // TODO : 일정시간마다 작동하는 transactionChecker를 사용하려면 주석풀기
        //checker.start();

        while (true){
            // get Stream Data with schema [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]
            if(!maker.hasNext()){
                log.info("[Train mode] Waiting for User Appearance Space Log...");
                Thread.sleep(1000);
                continue;
            }

            // parse string data to send FASDModule_user for check DB
            data = maker.next();
            try {
                userID = data[3];
                userMap.get(userID).checkDB(data);
                //log.info("[Monitoring mode] Data is sent to Monitoring System");

            }catch (Exception e){
                e.printStackTrace();
                log.info("[Monitoring mode] Data is not found");
            }

        }
    }
}
