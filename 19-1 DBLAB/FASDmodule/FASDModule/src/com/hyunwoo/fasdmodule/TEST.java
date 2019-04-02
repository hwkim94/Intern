package com.hyunwoo.fasdmodule;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class TEST {
    public static void main(String[] args) {
        try {





            //client가 보낸 데이터 출력



            while (true) {
                Socket socket = new Socket("127.0.0.1", 35002);
                BufferedReader bufReader = new BufferedReader( new InputStreamReader( socket.getInputStream()));
                String message = bufReader.readLine();
                System.out.println("Message : " + message);
            }

        }

        catch( Exception e ){

            e.printStackTrace();

        }
    }
}
