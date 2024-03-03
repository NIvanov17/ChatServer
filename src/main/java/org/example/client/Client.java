package org.example.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isDone;


    @Override
    public void run() {
        try {
            client = new Socket("127.0.0.1", 9999);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);
            InputHandler inputHandler = new InputHandler();
            Thread thread = new Thread(inputHandler);
            thread.start();
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }

        } catch (IOException e) {
            shutdown();
        }
    }

    public void shutdown() {
        isDone = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }

        } catch (IOException e) {
            shutdown();
        }
    }

    public class InputHandler implements Runnable {



        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                while (!isDone) {
                    String message = reader.readLine();
                    if(message.equals("/exit")){
                        out.println(message);
                        reader.close();
                        shutdown();
                    }else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
