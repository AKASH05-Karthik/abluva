package com.galliumdata.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple MSSQL Proxy Server
 *
 * This is a minimal MSSQL proxy that forwards connections between clients and a real MSSQL server.
 * It demonstrates the core functionality without the complexity of the full Gallium Data engine.
 */
public class MSSQLProxyMain {

    private static final Logger log = LogManager.getLogger(MSSQLProxyMain.class);

    private static final String DEFAULT_PROXY_HOST = "127.0.0.1";

    // *** CHANGED DEFAULT PROXY PORT TO A SAFE PORT ***
    private static final int DEFAULT_PROXY_PORT = 14330; // NEW PORT (no conflict)

    private static final String DEFAULT_TARGET_HOST = "localhost"; // Real MSSQL server
    private static final int DEFAULT_TARGET_PORT = 1433; // MSSQL Server Port
    private static final int DEFAULT_TIMEOUT = 15000;

    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {

        String proxyHost = getArg(args, "--proxy-host", DEFAULT_PROXY_HOST);
        int proxyPort = getIntArg(args, "--proxy-port", DEFAULT_PROXY_PORT);
        String targetHost = getArg(args, "--target-host", DEFAULT_TARGET_HOST);
        int targetPort = getIntArg(args, "--target-port", DEFAULT_TARGET_PORT);
        int timeout = getIntArg(args, "--timeout", DEFAULT_TIMEOUT);

        MSSQLProxyMain proxy = new MSSQLProxyMain();
        proxy.start(proxyHost, proxyPort, targetHost, targetPort, timeout);
    }

    private static String getArg(String[] args, String key, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (key.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    private static int getIntArg(String[] args, String key, int defaultValue) {
        String value = getArg(args, key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid value for " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    public void start(String proxyHost, int proxyPort, String targetHost, int targetPort, int timeout) {
        log.info("Starting MSSQL Proxy Server...");
        log.info("Proxy listening on: " + proxyHost + ":" + proxyPort);
        log.info("Forwarding to: " + targetHost + ":" + targetPort);

        try {
            serverSocket = new ServerSocket(proxyPort, 50, java.net.InetAddress.getByName(proxyHost));
            running = true;

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            log.info("MSSQL Proxy Server is running. Press Ctrl+C to stop.");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("New client connection from: " + clientSocket.getRemoteSocketAddress());

                    // Handle connection in a separate thread
                    executor.submit(() -> handleConnection(clientSocket, targetHost, targetPort, timeout));

                } catch (IOException e) {
                    if (running) {
                        log.error("Error accepting client connection: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            log.error("Failed to start proxy server: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void handleConnection(Socket clientSocket, String targetHost, int targetPort, int timeout) {
        Socket serverSocket = null;

        try {
            serverSocket = new Socket();
            serverSocket.connect(new java.net.InetSocketAddress(targetHost, targetPort), timeout);
            log.info("Connected to target server: " + targetHost + ":" + targetPort);

            final Socket finalServerSocket = serverSocket;

            Thread clientToServer = new Thread(() -> forwardData(clientSocket, finalServerSocket, "client->server"));
            Thread serverToClient = new Thread(() -> forwardData(finalServerSocket, clientSocket, "server->client"));

            clientToServer.start();
            serverToClient.start();

            clientToServer.join();
            serverToClient.join();

        } catch (Exception e) {
            log.error("Error handling connection: " + e.getMessage());
        } finally {
            closeQuietly(clientSocket);
            closeQuietly(serverSocket);
        }
    }

    private void forwardData(Socket from, Socket to, String direction) {
        byte[] buffer = new byte[8192];
        int bytesRead;

        try (InputStream in = from.getInputStream();
             OutputStream out = to.getOutputStream()) {

            log.debug("Starting forward: " + direction);

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }

        } catch (IOException e) {
            log.debug("Connection closed (" + direction + "): " + e.getMessage());
        }

        log.debug("Forwarding stopped: " + direction);
    }

    private void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.debug("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void closeQuietly(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.debug("Error closing server socket: " + e.getMessage());
            }
        }
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        log.info("Stopping MSSQL Proxy Server...");

        closeQuietly(serverSocket);
        executor.shutdown();

        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("MSSQL Proxy Server stopped.");
    }
}
