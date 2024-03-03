package org.example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private List<ConnectionHandler> connections;
    private boolean isDone;
    private ServerSocket sever;
    private ExecutorService pool;

    public Server() {
        this.connections = new ArrayList<>();
        isDone = false;
    }

    @Override
    public void run() {
        try {
            sever = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!isDone) {
                Socket client = sever.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                this.connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            try {
                shutdown();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler connection : connections) {
            if (connection != null) {
                connection.sendMessage(message);
            }
        }
    }

    public void shutdown() throws IOException {
        isDone = true;
        pool.shutdown();
        if (!sever.isClosed()) {
            sever.close();
        }
        for (ConnectionHandler connection : connections) {
            connection.shutdown();
        }
    }

    public class ConnectionHandler implements Runnable {


        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private String message;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                out.println("Please enter a nickname:");
                nickname = in.readLine();
                while (nickname.isBlank()) {
                    out.println("Please enter a valid nickname!");
                    in.readLine();
                }

                System.out.println(nickname + " is connected!");
                broadcast(nickname + " joined the chat!");

                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick:")) {
                        String[] splited = message.split(" ");
                        System.out.println(this.nickname + " changed his nickname to: " + splited[1]);
                        this.nickname = splited[1];
                        broadcast("Successfully changed nickname to: " + this.nickname);
                    } else if (message.equals("/exit")) {
                        broadcast(this.nickname + " left the chat.");
                        System.out.println(this.nickname + " left the chat.");
                        shutdown();
                    } else if (message.equals("/time")) {
                        System.out.println(String.valueOf(LocalTime.now()));
                        broadcast(String.valueOf(LocalTime.now()));
                    } else {
                        broadcast(this.nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown()  {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            }catch (IOException e){
                //ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
