package com.hyunwoo.fasdmodule.db;

import com.hyunwoo.fasdmodule.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by KimDhanHee on 2018-05-21.
 */
public class DBManager {
    private static Logger log = Logger.getLogger(Main.class.toString());
    private static final String DB_PROPERTIES_FILE = "db.properties";
    private Connection conn;

    private static DBManager dbManager;

    private DBManager() {
        if (conn == null) {
            Properties props = new Properties();
            try {
                props.load(new InputStreamReader(new FileInputStream(new File(DB_PROPERTIES_FILE))));
                String driver = props.getProperty("driver");
                String url = props.getProperty("url");
                String user = props.getProperty("user");
                String passwd = props.getProperty("passwd");
                Class.forName(driver);

                conn = DriverManager.getConnection(url, user, passwd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static DBManager getInstance() {
        if (dbManager == null)
            dbManager = new DBManager();

        return dbManager;
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

    // transaction 이탈 비교(현재의 transaction과 기존 transaction을 비교하여 얼마나 다른지 계산)
    public double[] getTransactionComparison(ArrayList<String[]> transactions){
        String userID = transactions.get(0)[3];
        int profileID = makingProfile(transactions.get(0)[2]);

        // transaction에 있는 camera set
        HashSet<String> transactionCameraSet = new HashSet<>();
        for (String[] transaction : transactions){
            transactionCameraSet.add(transaction[1]);
        }

        String sql = "select route_id, camera_id from route_description where user_id=" + userID + " and profile_description_id=" + profileID;
        ResultSet resultSet;
        HashMap<Integer, HashSet<String>> resultMap =  new HashMap<>();
        int route_id, max_rid;
        double ratio, max_ratio;

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            resultSet = pstmt.executeQuery();

            // 각 route에 존재하는 camera들 모음
            while(resultSet.next()) {
                route_id = resultSet.getInt("route_id");
                 if (!resultMap.containsKey(route_id)){
                     resultMap.put(route_id, new HashSet<>());
                 }

                 if(resultSet.getString("camera_id").contains("-")) {
                     resultMap.get(route_id).add(resultSet.getString("camera_id").split("-")[1]);
                 }else{
                     resultMap.get(route_id).add(resultSet.getString("camera_id"));
                 }
            }

            // 현재 transaction과 route 사이에 얼마나 겹치는지 확인
            max_rid = -1;
            max_ratio = 0.0;
            for(int rid : resultMap.keySet()){
                // AnB / AuB
                ratio = intersection(transactionCameraSet, resultMap.get(rid)).size()*1.0 / union(transactionCameraSet, resultMap.get(rid)).size()*1.0;
                if (max_ratio <= ratio){
                    max_rid = rid;
                    max_ratio = ratio;
                }
            }

            return new double[]{max_rid*1.0, max_ratio, 1.0} ;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new double[]{0.0, 0.0, 0.0} ;
    }

    // 실시간 단일 cctv 비교
    public int getCCTVComparison(String userID, String cameraID) {
        String sql = "select * from route_description where user_id=" + userID + " and camera_id="+cameraID;
        ResultSet resultSet;
        int cnt = 0;

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            resultSet = pstmt.executeQuery();

            while(resultSet.next()) {
                cnt += 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(cnt>=1){
            // 해당 cctv가 경로에 있을 경우
            return 1;
        }else{
            // 해당 cctv가 경로에 없을 경우
            return 0;
        }
    }

    private HashSet<String> union(HashSet<String> a , HashSet<String> b) {
        HashSet<String> aa = new HashSet<>(a);
        aa.addAll(b);

        return aa;
    }

    private HashSet<String> intersection(HashSet<String> a , HashSet<String> b) {
        HashSet<String> aa = new HashSet<>(a);
        aa.retainAll(b);

        return aa;
    }
}
