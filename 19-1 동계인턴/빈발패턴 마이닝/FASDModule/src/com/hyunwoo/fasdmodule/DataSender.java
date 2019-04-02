package com.hyunwoo.fasdmodule;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class DataSender {
    private static final String SYSTEM_PROPERTIES_FILE = "system.properties";
    private static Properties systemProps = new Properties();
    int SOCKET_PORT;

    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedWriter bufWriter;

    public DataSender(){
        try {
            systemProps.load(new InputStreamReader(new FileInputStream(new File(SYSTEM_PROPERTIES_FILE))));
        }catch (Exception e){
            e.printStackTrace();
        }

        SOCKET_PORT = Integer.parseInt(systemProps.getProperty("MONITORING_PORT"));
    }

    private int makingProfile(String timestamp){
        Date date = new Date(Long.parseLong(timestamp));
        int time = Integer.parseInt(date.toString().split(" ")[3].split(":")[0]);

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

    // 실시간 저장
    public void sendTxt(String timestamp, String userID, String cctvID, int result){
        // TODO : 모니터링 시스템으로 전송
        // TODO : 우선 로컬에 저장 구현해두고 나중에 바꾸기
        // output schema : [timestamp, userID, cctvID, result]
        try {
            FileWriter fw = new FileWriter("cctv_result_log.txt",true);
            fw.write(timestamp + " " + userID +  " " +  cctvID + " "+ result + "\n");
            fw.close();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // transaction 비교
    public void sendTxt(ArrayList<String[]> transactions, double[] transaction_result) {
        // TODO : 모니터링 시스템으로 전송
        // TODO : 우선 로컬에 저장 구현해두고 나중에 바꾸기
        // raw data schema : [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]
        int profile = makingProfile(transactions.get(0)[2]);

        if (transaction_result[2] == 1.0) {
            // DB 검색에 문제가 없었을 경우
            // route id랑 transaction의 camera 들을 전송
            // output schema : [timestamp, profileID, userID, routeID : [cctv list]]
            try {
                FileWriter fw = new FileWriter("transaction_result_log.txt", true);
                fw.write(transactions.get(0)[2] + " " + profile + " " + transactions.get(0)[3] + " " + transaction_result[1] + " : ");
                for (String[] transaction : transactions) {
                    fw.write(transaction[1] + " ");
                }
                fw.write("\n");
                fw.close();

            } catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            // DB 검색에 문제가 있었을 경우
            // transaction만 전송
            // output schema : [timestamp, userID : [cctv list]]
            try {
                FileWriter fw = new FileWriter("cctv_result_log.txt", true);
                fw.write(transactions.get(0)[2] + " " + transactions.get(0)[3] + " : ");
                for (String[] transaction : transactions) {
                    fw.write(transaction[1] + " ");
                }
                fw.write("\n");
                fw.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    // 실시간 저장
    public void send(String timestamp, String userID, String cctvID, int result){
        // output schema : [timestamp, userID, cctvID, result]
        try {
            serverSocket = new ServerSocket(SOCKET_PORT);
            socket = serverSocket.accept();
            bufWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            bufWriter.write(timestamp + " " + userID +  " " +  cctvID + " "+ result);
            bufWriter.newLine();

            bufWriter.close();
            socket.close();
            serverSocket.close();

        } catch( Exception e ){
            e.printStackTrace();
        }
    }

    // transaction 비교
    public void send(ArrayList<String[]> transactions, double[] transaction_result){
        // TODO : 모니터링 시스템으로 전송
        // TODO : 우선 로컬에 저장 구현해두고 나중에 바꾸기
        // raw data schema : [TYPE, cctvID, timestamp, userID, name, age, sex, matchPercent, etc]
        int profile = makingProfile(transactions.get(0)[2]);

        if (transaction_result[2] == 1.0){
            // DB 검색에 문제가 없었을 경우
            // route id랑 transaction의 camera 들을 전송
            // output schema : [timestamp, profileID, userID, routeID : [cctv list]]

            try {
                serverSocket = new ServerSocket(SOCKET_PORT);
                socket = serverSocket.accept();
                bufWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                bufWriter.write(transactions.get(0)[2] + " " + transactions.get(0)[3] + " : ");
                for (String[] transaction : transactions) {
                    bufWriter.write(transaction[1] + " ");
                }
                bufWriter.newLine();

                bufWriter.close();
                socket.close();
                serverSocket.close();

            } catch( Exception e ){
                e.printStackTrace();
            }

        }else{
            // DB 검색에 문제가 있었을 경우
            // transaction만 전송
            // output schema : [timestamp, userID : [cctv list]]
            try {
                bufWriter.write(transactions.get(0)[2] + " "+ profile +" " + transactions.get(0)[3] + " " + transaction_result[1] + " : ");
                for (String[] transaction : transactions) {
                    bufWriter.write(transaction[1] + " ");
                }
                bufWriter.newLine();

                bufWriter.close();
                socket.close();
                serverSocket.close();

            } catch( Exception e ){
                e.printStackTrace();
            }
        }
   }
}
