package info.kgeorgiy.ja.lorents.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {

    private final static int TIMEOUT = 60;
    private DatagramSocket serverSocket;
    private ExecutorService threadPool;
    private ExecutorService listener;

    private static boolean validateArgs(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Incorrect arguments");
            return false;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments should not be null");
            return false;
        }
        return true;
    }

    /**
     * The main function that takes command line arguments and runs the server.
     * @param args - command line arguments.
     */
    public static void main(String[] args) {
        if (validateArgs(args)) {
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            int threads = Integer.parseInt(args[1]);
            HelloUDPServer server = new HelloUDPServer();
            server.start(port, threads);
            server.close();
        } catch (NumberFormatException e) {
            System.err.println("Incorrect argument, it should be a number " + e.getMessage());
        }
    }
    @Override
    public void start(int port, int threads) {
        // :NOTE: close после нескольких повторных вызовов start закроет только последние ресурсы - утечка
        if (serverSocket != null) {
            return;
        }

        try {
            serverSocket = new DatagramSocket(port);
        } catch (SocketException e) { // :NOTE: пробросить наверх
            System.err.println("Unable to create socket" + e.getMessage());
        }
        threadPool = Executors.newFixedThreadPool(threads);
        listener = Executors.newSingleThreadExecutor();
        listener.submit(this::receive);
    }

    private void receive() {
        try {
            while (!serverSocket.isClosed()) {
                int buffSize = serverSocket.getReceiveBufferSize();
                DatagramPacket request = new DatagramPacket(new byte[buffSize], buffSize);
                serverSocket.receive(request);
                threadPool.submit(() -> {
                    String requestMessage = new String(request.getData(), request.getOffset(), request.getLength());
                    request.setData(("Hello, " + requestMessage).getBytes(StandardCharsets.UTF_8));
                    try {
                        serverSocket.send(request);
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            System.err.println("An error occurred while sending the request" + e.getMessage());
                        }
                    }
                });
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                System.err.println("Request processing error" + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        serverSocket.close();
        listener.shutdown();
        threadPool.shutdown();
        try {
            if (!listener.awaitTermination(TIMEOUT, TimeUnit.SECONDS) ||
                    !threadPool.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                System.out.println("Timeout exceeded");
            }
        } catch (InterruptedException e) {
            System.err.println("Could not terminate executor pools: " + e.getMessage());
        }
    }
}
