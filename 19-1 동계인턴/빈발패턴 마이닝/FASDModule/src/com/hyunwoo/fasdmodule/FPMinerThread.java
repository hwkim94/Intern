package com.hyunwoo.fasdmodule;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class FPMinerThread implements Runnable{
    private static Logger log = Logger.getLogger(Main.class.toString());
    private String userID, profileID, server_ip, server_port;

    @Override
    public void run() {
        try {
            String[] cmd = new String[] {"java", "-jar", "./FPMiner/fpminer.jar", this.userID, this.profileID, this.server_ip, this.server_port, "1"};
            Process process = new ProcessBuilder(cmd).start();

            BufferedReader reader = new BufferedReader( new InputStreamReader(process.getInputStream()) );
            String str = "";
            String newLine;
            while( (newLine = reader.readLine()) != null ) {
                str += (newLine+"\n");

                if(str.contains("Results")){
                    break;
                }
            }
            log.info("[Training] ------------------- FPMiner Log (" + this.userID + ", " + this.profileID +") -------------------\n"+str);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setInfo(String userID, String profileID, String server_ip, String server_port){
        this.userID = userID;
        this.profileID = profileID;
        this.server_ip = server_ip;
        this.server_port = server_port;
    }
}