package com.woodoo.testkiev.services;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class SocketExampleThread extends Thread {
    //public Socket socket;
    private String stringData = null;
    private boolean end = false;


    @Override
    public void run() {

        while (!end) {
            getCommandFromSocket();


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void getCommandFromSocket() {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("176.107.187.129", 1507), 5000);
            Log.d("mylog", "socket.connected");
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            stringData = input.readLine();
            if (stringData != null) {
                Log.d("mylog", stringData);
                JSONObject json = new JSONObject(stringData);
                Log.d("mylog", json.toString());
            }

        } catch (Exception e) {
            Log.e("mylog", e.toString());
        } finally {
            if(socket!=null){
                try {
                    socket.close();
                    socket=null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /*@Override
    public void run() {
        try {
            socket = new Socket();
            socket.setKeepAlive(true);
            socket.connect(new InetSocketAddress("176.107.187.129", 1507), 5000);
            Log.d("mylog", "socket.connected");

            while (!end) {
                Log.d("mylog", "start read socket");
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                stringData = input.readLine();
                if(stringData!=null){
                    Log.d("mylog", stringData);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        } catch (Exception e) {
            Log.e("mylog", e.toString());
        }
    }*/

    public void close() {


    }

    public void sendMessage(String message) {
        /*if (bufferSender != null && !bufferSender.checkError()) {
            bufferSender.println(message);
            bufferSender.flush();
        }*/
    }
}