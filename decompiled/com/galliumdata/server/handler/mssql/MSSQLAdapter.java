/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.adapters.AdapterCallback;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import com.galliumdata.server.adapters.AdapterInterface;
import com.galliumdata.server.adapters.AdapterStatus;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.handler.mssql.MSSQLConnectionHandler;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.repository.Project;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MSSQLAdapter
implements AdapterInterface {
    private Project project;
    private Connection connection;
    private Map<String, Object> parameters;
    private AdapterCallback callback;
    private final Set<MSSQLConnectionHandler> liveHandlers = new HashSet<MSSQLConnectionHandler>();
    private final AdapterStatus status = new AdapterStatus();
    private ServerSocket serverSocket;
    private boolean stopRequested = false;
    private final Variables adapterContext = new Variables();
    protected int batchSizeRows = -1;
    protected int batchSizeBytes = -1;
    private static final Logger log = LogManager.getLogger((String)"galliumdata.core");
    private static final int MAX_LIVE_HANDLERS = 1000;
    protected static final String PARAM_LOCAL_ADDRESS = "Local address";
    protected static final String PARAM_LOCAL_PORT = "Local port";
    protected static final String PARAM_SERVER_HOST = "Server host";
    protected static final String PARAM_SERVER_PORT = "Server port";
    protected static final String PARAM_TIMEOUT_TO_SERVER = "Timeout to server";
    protected static final String PARAM_TRUST_SERVER_CERT = "Trust server certificate";
    protected static final String PARAM_BATCH_SIZE_ROWS = "Result set batch size (rows)";
    protected static final String PARAM_BATCH_SIZE_BYTES = "Result set batch size (bytes)";
    protected SSLContext clientSSLContext;
    protected SSLContext serverSSLContext;
    public static final String CTXT_CONNECTION_CONTEXT = "connectionContext";

    @Override
    public void initialize() {
    }

    @Override
    public boolean configure(Project project, Connection conn, AdapterCallback callback) {
        log.trace(Markers.MSSQL, "MSSQL connection " + conn.getName() + " is being configured");
        this.project = project;
        this.connection = conn;
        this.parameters = conn.getParameters();
        this.callback = callback;
        return true;
    }

    @Override
    public void stopProcessing() {
        this.stopRequested = true;
        try {
            this.serverSocket.close();
        }
        catch (Exception ex) {
            log.trace(Markers.MSSQL, "Exception when closing serverSocket: " + ex.getMessage());
        }
    }

    @Override
    public void switchProject(Project project, Connection conn) {
        this.project = project;
        this.parameters = conn.getParameters();
        this.callback.resetCache();
        this.callback.setProject(project);
    }

    @Override
    public void shutdown() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            }
            catch (Exception ex) {
                log.warn(Markers.MSSQL, "Exception while shutting down MSSQL adapter: " + ex.getMessage());
            }
        }
    }

    @Override
    public AdapterStatus getStatus() {
        return this.status;
    }

    @Override
    public String getName() {
        return "MSSQL adapter - " + this.connection.getName();
    }

    @Override
    public void run() {
        int localPort;
        Object obj;
        InetAddress localAddress;
        this.status.startTime = ZonedDateTime.now();
        String localAddressStr = (String)this.parameters.get(PARAM_LOCAL_ADDRESS);
        if (null == localAddressStr || localAddressStr.trim().length() == 0) {
            localAddressStr = "0.0.0.0";
        }
        try {
            localAddress = InetAddress.getByName(localAddressStr);
        }
        catch (Exception ex) {
            throw new ServerException("repo.BadHost", ex.getMessage());
        }
        try {
            obj = this.parameters.get(PARAM_LOCAL_PORT);
            if (obj == null) {
                throw new ServerException("repo.BadProperty", PARAM_LOCAL_PORT, "Local port must be specified");
            }
            if (!(obj instanceof Integer)) {
                throw new ServerException("repo.BadProperty", PARAM_LOCAL_PORT, "Local port must be an integer");
            }
            localPort = (Integer)obj;
        }
        catch (Exception ex) {
            throw new ServerException("repo.BadProperty", PARAM_LOCAL_PORT, ex.getMessage());
        }
        try {
            obj = this.parameters.get(PARAM_BATCH_SIZE_ROWS);
            if (obj == null || obj.toString().isBlank()) {
                this.batchSizeRows = -1;
            } else {
                if (!(obj instanceof Integer)) {
                    throw new ServerException("repo.BadProperty", PARAM_BATCH_SIZE_ROWS, "Result set batch size (rows) must be an integer");
                }
                this.batchSizeRows = (Integer)obj;
            }
        }
        catch (Exception ex) {
            throw new ServerException("repo.BadProperty", PARAM_BATCH_SIZE_ROWS, ex.getMessage());
        }
        try {
            obj = this.parameters.get(PARAM_BATCH_SIZE_BYTES);
            if (obj == null || obj.toString().isBlank()) {
                this.batchSizeBytes = -1;
            } else {
                if (!(obj instanceof Integer)) {
                    throw new ServerException("repo.BadProperty", PARAM_BATCH_SIZE_BYTES, "Result set batch size (bytes) must be an integer");
                }
                this.batchSizeBytes = (Integer)obj;
            }
        }
        catch (Exception ex) {
            throw new ServerException("repo.BadProperty", PARAM_BATCH_SIZE_BYTES, ex.getMessage());
        }
        try {
            this.serverSocket = new ServerSocket(localPort, 1000, localAddress);
        }
        catch (Exception ex) {
            throw new ServerException("core.PortAlreadyTaken", localPort, this.project.getName());
        }
        log.trace(Markers.MSSQL, "MSSQL adapter is now starting on local port " + localPort);
        while (!this.stopRequested) {
            Socket socket = null;
            try {
                socket = this.serverSocket.accept();
            }
            catch (Exception ex) {
                if (!"Socket closed".equals(ex.getMessage())) {
                    log.error("MSSQL local socket got exception: " + ex.getMessage());
                }
                try {
                    if (socket == null) break;
                    socket.close();
                }
                catch (Exception ex2) {
                    ex.printStackTrace();
                }
                break;
            }
            Variables context = new Variables();
            context.put("connectionParameters", this.parameters);
            Variables connectionContext = new Variables();
            InetSocketAddress remoteAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
            connectionContext.put("clientIP", remoteAddress.getHostString());
            connectionContext.put("clientPort", remoteAddress.getPort());
            context.put(CTXT_CONNECTION_CONTEXT, connectionContext);
            AdapterCallbackResponse callbackResponse = this.callback.connectionRequested(socket, context);
            if (callbackResponse.reject) {
                try {
                    socket.close();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                continue;
            }
            context = new Variables();
            context.put(CTXT_CONNECTION_CONTEXT, connectionContext);
            MSSQLConnectionHandler handler = new MSSQLConnectionHandler(socket, this, context, "MSSQLConnectionHandler");
            if (this.liveHandlers.size() > 1000) {
                throw new ServerException("db.mssql.server.TooManyRequests", this.liveHandlers.size(), 1000);
            }
            this.liveHandlers.add(handler);
            handler.start();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public String testConnection(Project project, Connection conn) {
        MSSQLConnectionHandler handler = new MSSQLConnectionHandler(this);
        try {
            handler.connectToServer();
            String string = null;
            return string;
        }
        catch (Exception ex) {
            String string = ex.getMessage();
            return string;
        }
        finally {
            handler.cleanupTestHandler();
        }
    }

    public Object getParameter(String name) {
        return this.parameters.get(name);
    }

    public AdapterCallback getCallbackAdapter() {
        return this.callback;
    }

    public Project getProject() {
        return this.project;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void handlerIsDone(MSSQLConnectionHandler handler) {
        if (!this.liveHandlers.contains(handler)) {
            log.warn(Markers.MSSQL, "Unknown handler terminating");
            return;
        }
        this.getCallbackAdapter().connectionClosing(handler.connectionContext);
        this.liveHandlers.remove(handler);
    }

    protected synchronized void initializeServerSSLContext(int serverMajorVersion) {
        if (this.serverSSLContext != null) {
            return;
        }
        try {
            if (serverMajorVersion == 11) {
                System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.0,TLSv1.1,TLSv1.2");
                this.serverSSLContext = SSLContext.getInstance("TLSv1.1");
            } else {
                this.serverSSLContext = SSLContext.getInstance("TLSv1.2");
            }
            Object trustParam = this.getParameter(PARAM_TRUST_SERVER_CERT);
            TrustManager[] trustManagers = trustParam == null || trustParam.equals(Boolean.FALSE) ? this.getProject().getTrustManagers() : Project.getCredulousTrustManagers();
            this.serverSSLContext.init(this.getProject().getKeyManagers(), trustManagers, new SecureRandom());
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected synchronized void intializeClientSSLContext() {
        if (this.clientSSLContext != null) {
            return;
        }
        try {
            this.clientSSLContext = SSLContext.getInstance("TLSv1.2");
            this.clientSSLContext.init(this.getProject().getKeyManagers(), this.getProject().getTrustManagers(), new SecureRandom());
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
