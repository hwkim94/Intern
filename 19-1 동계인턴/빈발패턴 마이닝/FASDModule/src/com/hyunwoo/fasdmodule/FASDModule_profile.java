package com.hyunwoo.fasdmodule;

import com.hyunwoo.fasdmodule.db.DBManager;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class FASDModule_profile {
    private static Logger log = Logger.getLogger(Main.class.toString());

    private static final String DB_PROPERTIES_FILE = "db.properties";
    private static final String SYSTEM_PROPERTIES_FILE = "system.properties";
    private static Properties dbProps = new Properties();
    private static Properties systemProps = new Properties();

    private String FPMINER_SERVER, FPMINER_PORT;

    private ArrayList<String[]> rawData = new ArrayList<>();
    private ArrayList<String> transactions = new ArrayList<>();
    private DataSender dataSender;
    private HashMap<Long, HashMap<Long, Double[]>> timeTable;
    private HashMap<Long, Double[]> longestTable;

    private String transaction;
    private int transactionID = 0;
    private String userID;
    private int result, profile;
    private double[] result_transaction;

    private Long previousTimestamp;
    private Long previousCCTV;

    public FASDModule_profile(){}
    public FASDModule_profile(String userID, int profile, DataSender dataSender, HashMap<Long, HashMap<Long, Double[]>> timeTable, HashMap<Long, Double[]> longestTable){
        this.userID = userID;
        this.profile = profile;
        this.dataSender = dataSender;
        this.timeTable = timeTable;
        this.longestTable = longestTable;

        try {
            dbProps.load(new InputStreamReader(new FileInputStream(new File(DB_PROPERTIES_FILE))));
            systemProps.load(new InputStreamReader(new FileInputStream(new File(SYSTEM_PROPERTIES_FILE))));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void add(String[] rawData){
        this.rawData.add(rawData);
    }

    public void makingTransactionID(ArrayList<String[]> rawData){
        // raw data schema : [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]
        // transaction schema [timestamp, userID, transactionID, cctvID]
        Long previousCCTV = null;
        Long previousTimestamp = null;
        Long currentCCTV, currentTimestamp, lowCCTY, highCCTV;
        Double mean, std;
        int id=1;

        for(String[] data : rawData){
            currentCCTV = Long.parseLong(data[1]);
            currentTimestamp = Long.parseLong(data[2]);

            if(previousCCTV==null){
                previousCCTV = currentCCTV;
                previousTimestamp = currentTimestamp;
                continue;
            }

            if(previousCCTV.equals(currentCCTV)){
                // 같은 카메라의 detection 정보일 경우
                // TODO : 기준 정하기, 현재는 1분
                if (currentTimestamp-previousTimestamp>=1000*60){
                    // 다른 곳에 들렸다 오는 경우(이전 카메라를 나온 후 너무 오랜 후에 등장할 경우)
                    this.transactionID += 1;
                    id=1;

                    transaction = currentTimestamp + "\t" + data[3] + "\t" + this.transactionID + "\t" + currentCCTV;
                    this.transactions.add(transaction);
                }
                previousTimestamp = currentTimestamp;

            }else {
                // 다른 카메라의 detection 정보일 경우
                // TODO : 기준 정하기, 현재는 2.5sigma
                lowCCTY = Math.min(previousCCTV, currentCCTV);
                highCCTV = Math.max(previousCCTV, currentCCTV);

                try {
                    mean = this.timeTable.get(lowCCTY).get(highCCTV)[0];
                    std = this.timeTable.get(lowCCTY).get(highCCTV)[1];

                    if (((currentTimestamp - previousTimestamp) - mean) / std > 2.5) {
                        // 다른 곳에 들렸다 오는 경우(이전 카메라를 나온 후 너무 오랜 후에 등장할 경우)
                        this.transactionID += 1;
                        id=1;
                    }
                }catch (Exception e){
                    // previous-current camera가 처음 등장하는 조합일 경우
                    // 일단은 다른 transaction으로 처리
                    //System.out.println("Error-C : " +lowCCTY + " -> " + highCCTV);
                    this.transactionID += 1;
                    id=1;
                }

                previousCCTV = currentCCTV;
                previousTimestamp = currentTimestamp;

                transaction = currentTimestamp + "\t" + data[3] + "\t" + this.transactionID + "\t" +id+"-"+ currentCCTV;
                this.transactions.add(transaction);
                id++;
            }
        }
    }

    public void runMiner(){
        log.info("[Training] ------------------- Start FPMiner with (UserID, ProfileID) : (" + this.userID  + ", " + this.profile +") -------------------");
        makingTransactionID(this.rawData);

        FPMINER_SERVER = systemProps.getProperty("FPMINER_SEVER");
        FPMINER_PORT = systemProps.getProperty("FPMINER_PORT");

        // run FPMiner
        FPMinerThread fpMinerThread = new FPMinerThread();
        fpMinerThread.setInfo(this.userID, String.valueOf(this.profile), systemProps.getProperty("FPMINER_SERVER"), systemProps.getProperty("FPMINER_PORT"));
        Thread miner = new Thread(fpMinerThread);

        // TODO : 직접 콘솔/스크립트로 실행시킬 때는 주석처리
        miner.start();

        // send to FPMiner
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(FPMINER_PORT));
            Socket socket = serverSocket.accept();
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream());

            String allTransactionString = "";
            for (String tran : this.transactions) {
                allTransactionString += (tran + ",");
            }
            printWriter.write(allTransactionString);

            // TODO : Thread 종료시키기
            //miner.join();
            printWriter.flush();
            printWriter.close();
            socket.close();
            serverSocket.close();

        } catch( Exception e ){
            e.printStackTrace();
        }

        // clear all log data
        this.rawData.clear();
    }

    public void checkDB(String[] rawData){
        // raw data schema : [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]
        String timestamp = rawData[2];
        String userID = rawData[3];
        String cctvID = rawData[1];

        // 실시간 단일 CCTV 비교
        result = DBManager.getInstance().getCCTVComparison(userID, cctvID);

        // TODO : need to change sendTxt() -> send()
        dataSender.sendTxt(timestamp, userID, cctvID, result);

        // transaction 전체 비교
        trackingTransaction(rawData);

    }

    // 데이터가 들어올 때마다 검사
    private void trackingTransaction(String[] rawData){
        // 같은 transaction 인지 검사
        // raw data schema : [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]
        // TODO : 기준 정하기, 현재는 2.5sigma

        if (previousTimestamp == null){
            previousTimestamp = Long.parseLong(rawData[2]);
            previousCCTV = Long.parseLong(rawData[1]);
            this.rawData.add(rawData);
            return;
        }
        Long currentTimestamp = Long.parseLong(rawData[2]);

        try {
            // longestTable에 있는 cctv의 경우
            if (((currentTimestamp - previousTimestamp) - longestTable.get(previousCCTV)[0]) / longestTable.get(previousCCTV)[1] >= 2.5 * longestTable.get(previousCCTV)[1]) {
                // 다른 transaction인 경우, 학습된 transaction이랑 비교하여 전송
                previousTimestamp = Long.parseLong(rawData[2]);
                previousCCTV = Long.parseLong(rawData[1]);

                result_transaction = DBManager.getInstance().getTransactionComparison(this.rawData);

                // TODO : need to change sendTxt() -> send()
                dataSender.sendTxt(this.rawData, result_transaction);
                this.rawData.clear();

            } else {
                // 같은 transaction인 경우, 현재 transaction에 추가
                previousTimestamp = Long.parseLong(rawData[2]);
                previousCCTV = Long.parseLong(rawData[1]);
                this.rawData.add(rawData);
            }
        }catch (Exception e){
            // longestTable에 없는 cctv의 경우
            previousTimestamp = Long.parseLong(rawData[2]);
            previousCCTV = Long.parseLong(rawData[1]);
        }
    }

    // 정기적으로 검사
    public void trackingTransaction(Long currentTimestamp){

        // 같은 transaction 유지 중인지 검사
        // TODO : 기준 정하기, 현재는 2.5sigma

        if (((currentTimestamp-previousTimestamp)-longestTable.get(previousCCTV)[0])/longestTable.get(previousCCTV)[1] >= 2.5*longestTable.get(previousCCTV)[1]) {
            // 다른 transaction인 경우, 학습된 transaction이랑 비교하여 전송
            result_transaction = DBManager.getInstance().getTransactionComparison(this.rawData);
            dataSender.send(this.rawData, result_transaction);
        }
    }
}
