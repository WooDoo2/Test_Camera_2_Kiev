package com.woodoo.testkiev;

import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread {
    public Socket socket;
    @Override public void run() {
        super.run();
        try {
            // отправляем сообщение клиенту
            bufferSender =
                    new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                            true);

            // читаем сообщение от клиента
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // в бесконечном цикле ждём сообщения от клиента и смотрим, что там
            while (running) {
                String message = null;
                try {
                    message = in.readLine();
                } catch (IOException e) {
                }

                // проверка на команды
                if (hasCommand(message)) {
                    continue;
                }

                if (message != null && managerDelegate != null) {
                    user.setMessage(message); // сохраняем сообщение
                    managerDelegate.messageReceived(user, null); // уведомляем сервер о сообщении
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        running = false;

        if (bufferSender != null) {
            bufferSender.flush();
            bufferSender.close();
            bufferSender = null;
        }

        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        socket = null;
    }

    public void sendMessage(String message) {
        if (bufferSender != null && !bufferSender.checkError()) {
            bufferSender.println(message);
            bufferSender.flush();
        }
    }
}