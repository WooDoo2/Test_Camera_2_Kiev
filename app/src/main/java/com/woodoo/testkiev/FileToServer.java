package com.woodoo.testkiev;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class FileToServer extends AsyncTask<Void, Void, Void> {
    private File file;
    private boolean success;

    public FileToServer(String filePath) {
        Log.i("mylog", filePath);
        file = new File(filePath);
        execute();
    }

    @Override
    protected Void doInBackground(Void... params) {
        Socket socket = null;
        //DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;

        try {
            // Create a new Socket instance and connect to host
            //socket = new Socket("176.107.187.129", 1502);
            socket = new Socket();
            socket.connect(new InetSocketAddress("176.107.187.129", 1500), 5000);
            Log.i("mylog", "socket.connected");
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            //dataInputStream = new DataInputStream(socket.getInputStream());
            FileInputStream fileInputStream = new FileInputStream(file);

            byte[] buffer = new byte[4096];

            while (fileInputStream.read(buffer) > 0) {
                dataOutputStream.write(buffer);
            }
            fileInputStream.close();

            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("mylog", e.toString());
            success = false;
        } finally {
            // close socket
            if (socket != null) {
                try {
                    Log.i("mylog", "closing the socket");
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /*// close input stream
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/

            // close output stream
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }



    @Override
    protected void onPostExecute(Void result) {
        if (success) {

        } else {

        }
    }
}
