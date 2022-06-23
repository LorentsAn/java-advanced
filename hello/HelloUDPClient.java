package info.kgeorgiy.ja.lorents.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {

    private ExecutorService executor;

    private final static int TIMEOUT = 100;

    /**
     * The main function that takes command line arguments and runs the client.
     * @param args - command line arguments.
     */
    public static void main(String[] args) {

        if (args == null || args.length != 5 ) {
            System.err.println("Incorrect arguments");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments should not be null");
            return;
        }
        try {
            new HelloUDPClient().run(args[0], Integer.parseInt(args[1]), args[2],
                    Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (NumberFormatException e) {
            System.err.println("Incorrect argument, it should be a number " + e.getMessage());
        }

    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        executor = Executors.newFixedThreadPool(threads);
        SocketAddress address = new InetSocketAddress(host, port);
        IntStream.range(0, threads).forEach(threadNumber -> executor.submit(() -> process(prefix, threadNumber, address, requests)));
        close();
    }

    private void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                System.out.println("Timeout exceeded");
            }
        } catch (InterruptedException e) {
            System.out.println("Can't close" + e.getMessage());
        }
    }

    private void process(String prefix, int threadNumber, SocketAddress address, int requests) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            int buffSize = socket.getReceiveBufferSize();
            DatagramPacket request = new DatagramPacket(new byte[0], 0, address);
            for (int requestsNumber = 0; requestsNumber < requests; requestsNumber++) {
                String requestText = prefix + threadNumber + "_" + requestsNumber;
                while (!socket.isClosed() && !Thread.interrupted()) {
                    try {
                        request.setData(requestText.getBytes(StandardCharsets.UTF_8));
                        socket.send(request);
                        request.setData(new byte[buffSize]);
                        socket.receive(request);
                    } catch (IOException e) {
                        System.out.println("An error occurred while sending the request" + e.getMessage());
                    }
                    String responseText = new String(request.getData(), request.getOffset(),
                            request.getLength(), StandardCharsets.UTF_8);
                    if (responseText.contains(requestText)) {
                        System.out.println("Response: " + responseText);
                        break;
                    }

                }
            }
        } catch (SocketException e) {
            System.err.println("An error occurred while creating or opening a socket" + e.getMessage());
        }
    }
}
