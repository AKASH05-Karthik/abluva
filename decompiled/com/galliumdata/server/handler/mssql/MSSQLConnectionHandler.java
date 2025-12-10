/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.handler.mssql.ClientForwarder;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.MSSQLAdapter;
import com.galliumdata.server.handler.mssql.ServerForwarder;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.repository.RepositoryException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MSSQLConnectionHandler
extends Thread {
    private final Socket clientSocket;
    protected Socket mssqlSocket;
    protected MSSQLAdapter adapter;
    protected Variables connectionContext = new Variables();
    protected ConnectionState connectionState = new ConnectionState();
    private static final Logger log = LogManager.getLogger((String)"galliumdata.dbproto");

    public MSSQLConnectionHandler(Socket clientSocket, MSSQLAdapter adapter, Variables ctxt, String name) {
        super(name);
        this.clientSocket = clientSocket;
        this.adapter = adapter;
        this.connectionState.connectionContext = this.connectionContext = (Variables)ctxt.get("connectionContext");
    }

    MSSQLConnectionHandler(MSSQLAdapter adapter) {
        super("MSSQL connection test for " + adapter.getConnection().getName());
        this.adapter = adapter;
        this.clientSocket = null;
    }

    @Override
    public void run() {
        ServerForwarder forwardFromServer;
        ClientForwarder forwardFromClient;
        log.trace(Markers.MSSQL, "MSSQLConnectionHandler is running");
        this.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        this.connectionState.setServerName((String)this.adapter.getParameter("Server host"));
        try {
            this.connectToServer();
        }
        catch (Exception ex) {
            log.error(Markers.MSSQL, "Unable to connect to MSSQL server: " + ex.getMessage());
            if (log.isDebugEnabled()) {
                ex.printStackTrace();
            }
            return;
        }
        try {
            forwardFromClient = new ClientForwarder(this.clientSocket, this.mssqlSocket, this.connectionContext, this.connectionState);
            forwardFromServer = new ServerForwarder(this.mssqlSocket, this.clientSocket, this.connectionContext, this.connectionState);
        }
        catch (Exception ex) {
            log.error(Markers.MSSQL, "Unable to start connection handler for {}/{} because of exception while launching forwarders: {}", (Object)this.adapter.getProject().getName(), (Object)this.adapter.getConnection().getName(), (Object)ex.getMessage());
            this.cleanup();
            return;
        }
        forwardFromClient.setOtherForwarder(forwardFromServer);
        forwardFromClient.setAdapter(this.adapter);
        forwardFromServer.setOtherForwarder(forwardFromClient);
        forwardFromServer.setAdapter(this.adapter);
        this.connectionContext.put("userIP", ((InetSocketAddress)this.clientSocket.getRemoteSocketAddress()).getAddress().toString());
        this.connectionContext.put("clientForwarder", forwardFromClient);
        this.connectionContext.put("serverForwarder", forwardFromServer);
        Thread forwardFromClientThread = new Thread(forwardFromClient, "forwardFromClientThread");
        forwardFromClientThread.start();
        forwardFromServer.thread = Thread.currentThread();
        forwardFromServer.run();
        this.cleanup();
        forwardFromClient.requestStop();
        if (log.isTraceEnabled()) {
            log.trace(Markers.MSSQL, "MSSQLConnectionHandler is done with this connection: client " + this.clientSocket.toString() + ", server " + this.mssqlSocket.toString() + ", connection " + this.adapter.getConnection().getName());
        }
    }

    public void connectToServer() {
        Integer timeout;
        int remotePort;
        this.connectionState.setConnectionName(this.adapter.getName());
        String serverName = (String)this.adapter.getParameter("Server host");
        if (null == serverName || serverName.trim().isEmpty()) {
            throw new RepositoryException("repo.MissingProperty", "Server host");
        }
        try {
            InetAddress remoteAddress = InetAddress.getByName(serverName);
        }
        catch (Exception ex) {
            log.error(Markers.MSSQL, "Bad property 'Server host' in repository: " + ex.getMessage());
            throw new ServerException("repo.BadHost", ex.getMessage());
        }
        try {
            remotePort = (Integer)this.adapter.getParameter("Server port");
        }
        catch (Exception ex) {
            log.error(Markers.MSSQL, "Bad property 'Server port' in repository: " + ex.getMessage());
            throw new ServerException("repo.BadProperty", "Server port", ex.getMessage());
        }
        try {
            timeout = (Integer)this.adapter.getParameter("Timeout to server");
            if (timeout == null) {
                timeout = 15000;
            }
        }
        catch (Exception ex) {
            log.error(Markers.MSSQL, "Bad property 'Timeout to server' in repository, assuming default of 15000: " + ex.getMessage());
            timeout = 15000;
        }
        try {
            this.mssqlSocket = new Socket();
            InetSocketAddress sockAddr = new InetSocketAddress(serverName, remotePort);
            this.mssqlSocket.connect(sockAddr, timeout);
        }
        catch (BindException bex) {
            log.error(Markers.MSSQL, "Unable to bind to port " + remotePort + " : " + bex.getMessage());
            this.cleanup();
            throw new ServerException("db.mssql.server.CannotConnectToServer", serverName, remotePort, bex.getMessage());
        }
        catch (SocketTimeoutException ste) {
            log.error(Markers.MSSQL, "Timeout while trying to connect to MSSQL port " + remotePort + " : " + ste.getMessage());
            this.cleanup();
            throw new ServerException("db.mssql.server.CannotConnectToServer", serverName, remotePort, ste.getMessage());
        }
        catch (Exception ex) {
            if (log.isDebugEnabled()) {
                ex.printStackTrace();
            }
            this.cleanup();
            throw new ServerException("db.mssql.server.CannotConnectToServer", serverName, remotePort, ex.getMessage());
        }
    }

    protected void cleanup() {
        try {
            this.clientSocket.close();
        }
        catch (Exception ex) {
            log.warn(Markers.MSSQL, "Exception closing socket from MSSQL client: " + ex.getMessage());
        }
        try {
            this.mssqlSocket.close();
        }
        catch (Exception ex) {
            log.warn(Markers.MSSQL, "Exception closing socket to MSSQL: " + ex.getMessage());
        }
        this.adapter.handlerIsDone(this);
    }

    protected void cleanupTestHandler() {
        try {
            this.mssqlSocket.close();
        }
        catch (Exception ex) {
            log.trace(Markers.MSSQL, "Exception closing socket to MSSQL for connection test: " + ex.getMessage());
        }
    }
}
