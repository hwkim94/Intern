package com.hyunwoo.fasdmodule;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;

public class DataReader extends Thread{
    // TODO : data set 선택
    private String path = "generated_cctv_log3.txt";
    private static final String SYSTEM_PROPERTIES_FILE = "system.properties";
    private static Properties systemProps = new Properties();
    int SOCKET_PORT;

    private ArrayList<String> lines = new ArrayList<>();
    private DataArrayMaker maker;
    private String data;


    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader bufReader;

    public DataReader(DataArrayMaker maker){
        try {
            systemProps.load(new InputStreamReader(new FileInputStream(new File(SYSTEM_PROPERTIES_FILE))));
        }catch (Exception e){
            e.printStackTrace();
        }

        SOCKET_PORT = Integer.parseInt(systemProps.getProperty("STREAM_PORT"));
        this.maker = maker;
    }

    @Override
    public void run() {
        try {
            // TODO : this.read()로 바꾸기
            this.readTxt();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void read() throws IOException{

        try {
            serverSocket = new ServerSocket(SOCKET_PORT);
            socket = serverSocket.accept();
            bufReader = new BufferedReader( new InputStreamReader( socket.getInputStream()));

            String line;
            while (true) {
                line = bufReader.readLine();
                maker.add(line);
            }

        } catch( Exception e ){
            bufReader.close();
            socket.close();
            serverSocket.close();
            e.printStackTrace();
        }
    }

    public void readTxt(){
        try{
            File file = new File(path);
            FileReader filereader = new FileReader(file);
            BufferedReader bufReader = new BufferedReader(filereader);

            String line;
            while((line = bufReader.readLine()) != null){
                maker.add(line);
            }

            bufReader.close();

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
