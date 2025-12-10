/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.util.BinaryDump;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.concurrent.atomic.AtomicLong;

public class PacketLogger {
    private static Path logPath = FileSystems.getDefault().getPath("/tmp/pkts", new String[0]);
    private static final AtomicLong packetNum = new AtomicLong();

    public static void saveIncomingPacket(Socket sock, byte[] buffer, int offset, int length) {
        int localPort = sock.getLocalPort();
        int remotePort = ((InetSocketAddress)sock.getRemoteSocketAddress()).getPort();
        String remoteHost = ((InetSocketAddress)sock.getRemoteSocketAddress()).getHostString();
        PacketLogger.savePacket(remotePort, localPort, remoteHost, buffer, offset, length);
    }

    public static void saveOutgoingPacket(Socket sock, byte[] buffer, int offset, int length) {
        int localPort = sock.getLocalPort();
        int remotePort = ((InetSocketAddress)sock.getRemoteSocketAddress()).getPort();
        String remoteHost = ((InetSocketAddress)sock.getRemoteSocketAddress()).getHostString();
        PacketLogger.savePacket(localPort, remotePort, remoteHost, buffer, offset, length);
    }

    public static void savePacket(int fromPort, int toPort, String remoteHost, byte[] buffer, int offset, int length) {
        ZonedDateTime now = ZonedDateTime.now();
        int hours = now.get(ChronoField.CLOCK_HOUR_OF_DAY);
        int minutes = now.get(ChronoField.MINUTE_OF_HOUR);
        int secs = now.get(ChronoField.SECOND_OF_MINUTE);
        String hoursStr = "" + hours;
        if (hours < 10) {
            hoursStr = "0" + hours;
        }
        String minutesStr = "" + minutes;
        if (minutes < 10) {
            minutesStr = "0" + minutes;
        }
        String secondsStr = "" + secs;
        if (secs < 10) {
            secondsStr = "0" + secs;
        }
        long nanos = System.nanoTime() % 1000000000L;
        String nanosStr = String.format("%09d", nanos);
        Path logSubdir = logPath.resolve(hoursStr).resolve(minutesStr).resolve(secondsStr);
        logSubdir.toFile().mkdirs();
        String filename = nanosStr + "_" + fromPort + "_" + toPort + "_" + remoteHost + ".txt";
        Path filePath = logSubdir.resolve(filename);
        String pktLog = BinaryDump.getBinaryDump(buffer, offset, length);
        byte[] pktLogBytes = pktLog.getBytes(StandardCharsets.UTF_8);
        try {
            FileOutputStream outStr = new FileOutputStream(filePath.toFile());
            outStr.write(pktLogBytes);
            outStr.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
