/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.bouncycastle.asn1.ASN1Encodable
 *  org.bouncycastle.asn1.ASN1Sequence
 *  org.bouncycastle.asn1.DERNull
 *  org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
 *  org.bouncycastle.asn1.pkcs.PrivateKeyInfo
 *  org.bouncycastle.asn1.pkcs.RSAPrivateKey
 *  org.bouncycastle.asn1.x509.AlgorithmIdentifier
 *  org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
 */
package com.galliumdata.server.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.handler.ProtocolData;
import com.galliumdata.server.handler.ProtocolDataArray;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.handler.ProtocolDataValue;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.repository.Breakpoint;
import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.repository.FilterStage;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.repository.PEMFileReader;
import com.galliumdata.server.repository.Persisted;
import com.galliumdata.server.repository.Repository;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.repository.RepositoryObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class Project
extends RepositoryObject {
    @Persisted(directoryName="connections", memberClass=Connection.class)
    protected Map<String, Connection> connections;
    @Persisted(directoryName="connection_filters", memberClass=FilterUse.class, fileName="connection_filter.json")
    protected Map<String, FilterUse> connectionFilters;
    @Persisted(directoryName="request_filters", memberClass=FilterUse.class, fileName="request_filter.json")
    protected Map<String, FilterUse> requestFilters;
    @Persisted(directoryName="response_filters", memberClass=FilterUse.class, fileName="response_filter.json")
    protected Map<String, FilterUse> responseFilters;
    @Persisted(directoryName="duplex_filters", memberClass=FilterUse.class, fileName="duplex_filter.json")
    protected Map<String, FilterUse> duplexFilters;
    @Persisted(JSONName="keyStore")
    protected KeyStore keyStore;
    protected char[] keyStorePassword;
    private boolean cryptoRead = false;
    protected String keyAlgorithm = "RSA";
    private KeyManager[] keyManagers;
    private TrustManager[] trustManagers;
    private SSLContext sslContext;
    private int cryptoHash;
    private List<Breakpoint> breakpoints;
    private Variables projectContext = new Variables();
    private static final Logger log = LogManager.getLogger((String)"galliumdata.core");

    public Project(Repository repo) {
        super(repo);
    }

    @Override
    public void forgetEverything() {
        this.connectionFilters = null;
        this.requestFilters = null;
        this.responseFilters = null;
        this.duplexFilters = null;
        this.cryptoRead = false;
        this.keyManagers = null;
        this.trustManagers = null;
        this.breakpoints = null;
    }

    public Map<String, Connection> getConnections() {
        if (this.connections == null) {
            this.connections = new TreeMap<String, Connection>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("connections"));
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return this.connections;
    }

    public Map<String, FilterUse> getFilters(FilterStage filterStage) {
        switch (filterStage) {
            case CONNECTION: {
                return this.getConnectionFilters();
            }
            case REQUEST: {
                return this.getRequestFilters();
            }
            case RESPONSE: {
                return this.getResponseFilters();
            }
        }
        throw new RuntimeException("Invalid filter type: " + String.valueOf((Object)filterStage));
    }

    private Map<String, FilterUse> orderFiltersByPriority(Map<String, FilterUse> filters) {
        Set<Map.Entry<String, FilterUse>> entries = filters.entrySet();
        ArrayList<Map.Entry<String, FilterUse>> entryList = new ArrayList<Map.Entry<String, FilterUse>>(entries);
        entryList.sort((o1, o2) -> {
            int p2;
            int p1 = ((FilterUse)o1.getValue()).getPriority();
            if (p1 == (p2 = ((FilterUse)o2.getValue()).getPriority())) {
                return 0;
            }
            return p1 < p2 ? 1 : -1;
        });
        filters = new LinkedHashMap<String, FilterUse>();
        for (Map.Entry entry : entryList) {
            filters.put((String)entry.getKey(), (FilterUse)entry.getValue());
        }
        return filters;
    }

    private Map<String, FilterUse> getConnectionFilters() {
        if (this.connectionFilters == null) {
            this.connectionFilters = new TreeMap<String, FilterUse>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("connectionFilters"));
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            this.connectionFilters = this.orderFiltersByPriority(this.connectionFilters);
        }
        return this.connectionFilters;
    }

    private Map<String, FilterUse> getRequestFilters() {
        if (this.requestFilters == null) {
            this.requestFilters = new TreeMap<String, FilterUse>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("requestFilters"));
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            this.getDuplexFilters();
            this.requestFilters.putAll(this.duplexFilters);
            this.requestFilters = this.orderFiltersByPriority(this.requestFilters);
        }
        return this.requestFilters;
    }

    private Map<String, FilterUse> getResponseFilters() {
        if (this.responseFilters == null) {
            this.responseFilters = new TreeMap<String, FilterUse>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("responseFilters"));
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            this.getDuplexFilters();
            this.responseFilters.putAll(this.duplexFilters);
            this.responseFilters = this.orderFiltersByPriority(this.responseFilters);
        }
        return this.responseFilters;
    }

    private void getDuplexFilters() {
        if (this.duplexFilters == null) {
            this.duplexFilters = new TreeMap<String, FilterUse>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("duplexFilters"));
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public List<Breakpoint> getBreakpoints() {
        this.readBreakpoints();
        return this.breakpoints;
    }

    public ProtocolData getBreakpointsAsJson() {
        ProtocolDataObject topNode = new ProtocolDataObject();
        ProtocolDataArray bpsNode = new ProtocolDataArray();
        ((ProtocolData)topNode).put("breakpoints", bpsNode);
        for (Breakpoint bp : this.getBreakpoints()) {
            ProtocolDataObject bpNode = new ProtocolDataObject();
            ((ProtocolData)bpNode).put("file", new ProtocolDataValue(bp.filename));
            ((ProtocolData)bpNode).put("line", new ProtocolDataValue(bp.linenum));
            bpsNode.add(bpNode);
        }
        return topNode;
    }

    public void addBreakpoint(Breakpoint bp) {
        this.readBreakpoints();
        if (!this.breakpoints.contains(bp)) {
            this.breakpoints.add(bp);
            this.writeBreakpoints();
        }
    }

    public void removeBreakpoint(Breakpoint bp) {
        this.readBreakpoints();
        this.breakpoints.remove(bp);
        this.writeBreakpoints();
    }

    private void readBreakpoints() {
        JsonNode node;
        if (this.breakpoints != null) {
            return;
        }
        this.breakpoints = new ArrayList<Breakpoint>();
        Path bpPath = this.path.resolveSibling("breakpoints.json");
        if (!(Files.exists(bpPath, new LinkOption[0]) && Files.isRegularFile(bpPath, new LinkOption[0]) && Files.isReadable(bpPath))) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = Files.newBufferedReader(bpPath);){
            node = mapper.readTree((Reader)reader);
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.BadFile", bpPath.toString(), ex.getMessage());
        }
        JsonNode bps = node.get("breakpoints");
        if (bps == null || bps.isNull()) {
            return;
        }
        if (!bps.isArray()) {
            throw new RepositoryException("repo.BadFile", bpPath.toString(), "breakpoints is not an array");
        }
        for (int i = 0; i < bps.size(); ++i) {
            JsonNode bp = bps.get(i);
            if (!bp.isObject()) {
                throw new RepositoryException("repo.BadFile", bpPath.toString(), "breakpoint is not an object at index " + i);
            }
            if (!bp.hasNonNull("file")) {
                throw new RepositoryException("repo.BadFile", bpPath.toString(), "breakpoint has no file attribute at index " + i);
            }
            if (!bp.hasNonNull("line")) {
                throw new RepositoryException("repo.BadFile", bpPath.toString(), "breakpoint has no line attribute at index " + i);
            }
            Breakpoint newBp = new Breakpoint(bp.get("file").asText(), bp.get("line").asInt());
            this.breakpoints.add(newBp);
        }
    }

    public void writeBreakpoints() {
        Path bpPath = this.path.resolveSibling("breakpoints.json");
        try (BufferedWriter writer = Files.newBufferedWriter(bpPath, new OpenOption[0]);){
            String json = this.getBreakpointsAsJson().toPrettyJSON();
            writer.write(json);
            writer.flush();
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.BadFile", this.path.toString(), ex.getMessage());
        }
    }

    public Variables getProjectContext() {
        return this.projectContext;
    }

    public void setProjectContext(Variables v) {
        this.projectContext = v;
    }

    public boolean projectHasKey() {
        this.readCryptoFiles();
        return this.keyManagers != null;
    }

    public KeyManager[] getKeyManagers() {
        this.readCryptoFiles();
        return this.keyManagers;
    }

    public void addKeyManagers(KeyManager[] mgrs) {
        int newSize = 0;
        if (this.keyManagers != null) {
            newSize = this.keyManagers.length;
        }
        KeyManager[] newMgrs = new KeyManager[newSize += mgrs.length];
        System.arraycopy(mgrs, 0, newMgrs, 0, mgrs.length);
        if (this.keyManagers != null) {
            System.arraycopy(this.keyManagers, 0, newMgrs, mgrs.length, this.keyManagers.length);
        }
        this.keyManagers = newMgrs;
    }

    public TrustManager[] getTrustManagers() {
        this.readCryptoFiles();
        return this.trustManagers;
    }

    public void addTrustManagers(TrustManager[] mgrs) {
        int newSize = 0;
        if (this.trustManagers != null) {
            newSize = this.trustManagers.length;
        }
        TrustManager[] newMgrs = new TrustManager[newSize += mgrs.length];
        System.arraycopy(mgrs, 0, newMgrs, 0, mgrs.length);
        if (this.trustManagers != null) {
            System.arraycopy(this.trustManagers, 0, newMgrs, mgrs.length, this.trustManagers.length);
        }
        this.trustManagers = newMgrs;
    }

    public int getCryptoHash() {
        if (this.cryptoHash == 0) {
            this.readCryptoFiles();
        }
        return this.cryptoHash;
    }

    public static TrustManager[] getCredulousTrustManagers() {
        return new TrustManager[]{new X509TrustManager(){

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
    }

    private void readCryptoFiles() {
        if (this.cryptoRead) {
            return;
        }
        this.cryptoRead = true;
        Path cryptoDir = this.path.resolveSibling("crypto");
        if (!Files.exists(cryptoDir, new LinkOption[0])) {
            return;
        }
        log.trace(Markers.REPO, "Reading crypto files for project: {}", (Object)this.path);
        if (!Files.isDirectory(cryptoDir, new LinkOption[0])) {
            throw new RepositoryException("repo.crypto.NotDirectory", cryptoDir.toString());
        }
        if (!Files.isReadable(cryptoDir)) {
            throw new RepositoryException("repo.crypto.NotReadable", cryptoDir.toString());
        }
        Path jsonPath = cryptoDir.resolve("crypto.json");
        if (Files.exists(jsonPath, new LinkOption[0])) {
            this.readCryptoJSON(jsonPath);
        }
        this.cryptoHash = 0;
        try (Stream<Path> paths = Files.walk(cryptoDir, 1, new FileVisitOption[0]);){
            paths.forEach(this::addCryptoFile);
        }
        catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    private void readCryptoJSON(Path path) {
        JsonNode keyAlgo;
        JsonNode topNode;
        log.trace(Markers.REPO, "Found JSON file for JKS: {}", (Object)path);
        ObjectMapper objMapper = new ObjectMapper();
        try {
            topNode = objMapper.readTree(path.toFile());
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.crypto.JSONReadError", path.toString(), ex.getMessage());
        }
        JsonNode pwNode = topNode.get("JKS password");
        if (pwNode != null) {
            this.keyStorePassword = pwNode.asText().toCharArray();
        }
        if ((keyAlgo = topNode.get("Key algorithm")) != null && !keyAlgo.isNull()) {
            this.keyAlgorithm = keyAlgo.asText();
        }
    }

    private void addCryptoFile(Path path) {
        log.trace(Markers.REPO, "Reading crypto file for project: {}", (Object)path);
        String filename = path.getFileName().toString();
        String lowerCaseFilename = filename.toLowerCase();
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            return;
        }
        switch (lowerCaseFilename) {
            case "keystore.jks": {
                this.readJKS(path);
                break;
            }
            case "key.pem": {
                this.readPrivateKey(path);
                break;
            }
            case "trust.pem": {
                this.readTrustFile(path);
            }
        }
    }

    private void readJKS(Path path) {
        log.debug(Markers.REPO, "Found JKS file for project {}: {}", (Object)this.name, (Object)path);
        try {
            this.keyStore = KeyStore.getInstance("JKS");
            this.keyStore.load(new FileInputStream(path.toFile()), this.keyStorePassword);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(this.keyStore, this.keyStorePassword);
            this.addKeyManagers(kmf.getKeyManagers());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(this.keyStore);
            this.addTrustManagers(tmf.getTrustManagers());
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.crypto.JKSReadError", path, ex.getMessage());
        }
    }

    private void readPrivateKey(Path path) {
        KeyStore keystore;
        List<PEMFileReader.PEMEntry> entries = PEMFileReader.readPEMFile(path.toString());
        if (entries.size() < 2) {
            log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " must contain a key and at least one certificate. SSL will not be available to clients.");
            return;
        }
        PrivateKey privateKey = null;
        ArrayList<PEMFileReader.PEMEntry> certEntries = new ArrayList<PEMFileReader.PEMEntry>();
        block18: for (PEMFileReader.PEMEntry entry : entries) {
            this.cryptoHash += entry.hashCode();
            if (this.cryptoHash == 0) {
                this.cryptoHash = 42;
            }
            switch (entry.label) {
                case "CERTIFICATE": {
                    certEntries.add(entry);
                    continue block18;
                }
                case "RSA PRIVATE KEY": {
                    if (privateKey != null) {
                        log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " contains more than one key. Only the first one will be used.");
                        continue block18;
                    }
                    try {
                        ASN1Sequence seq = ASN1Sequence.getInstance((Object)entry.bytes);
                        RSAPrivateKey bcPrivateKey = RSAPrivateKey.getInstance((Object)seq);
                        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                        AlgorithmIdentifier algId = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, (ASN1Encodable)DERNull.INSTANCE);
                        privateKey = converter.getPrivateKey(new PrivateKeyInfo(algId, (ASN1Encodable)bcPrivateKey));
                    }
                    catch (Exception ex) {
                        log.error("Unable to read private key: " + ex.getMessage());
                    }
                    continue block18;
                }
                case "PRIVATE KEY": {
                    if (privateKey != null) {
                        log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " contains more than one key. Only the first one will be used.");
                        continue block18;
                    }
                    try {
                        KeyFactory kf = KeyFactory.getInstance(this.keyAlgorithm);
                        PKCS8EncodedKeySpec skSpec = new PKCS8EncodedKeySpec(entry.bytes, this.keyAlgorithm);
                        privateKey = kf.generatePrivate(skSpec);
                    }
                    catch (Exception ex) {
                        log.error("Unable to read private key: " + ex.getMessage());
                    }
                    continue block18;
                }
            }
            log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " contains a section labelled " + entry.label + " which is not recognized and will be ignored");
        }
        if (privateKey == null) {
            log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " does not contain a private key -- SSL will not be available to clients");
            return;
        }
        if (certEntries.size() == 0) {
            log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " contains a key but does not contain any certificates, which makes the key unusable. SSL will not be available to clients.");
        }
        Certificate[] keyCerts = new Certificate[certEntries.size()];
        try {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null, null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            int certIdx = 0;
            for (PEMFileReader.PEMEntry certEntry : certEntries) {
                Certificate serverCert;
                keyCerts[certIdx] = serverCert = cf.generateCertificate(new ByteArrayInputStream(certEntry.bytes));
                keystore.setCertificateEntry("server-cert-" + ++certIdx, serverCert);
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.crypto.ErrorReadingCertificate", path.toString(), ex.getMessage());
        }
        try {
            KeyStore.PrivateKeyEntry entry = new KeyStore.PrivateKeyEntry(privateKey, keyCerts);
            keystore.setEntry("key.pem", entry, new KeyStore.PasswordProtection("GalliumData".toCharArray()));
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, "GalliumData".toCharArray());
            this.addKeyManagers(kmf.getKeyManagers());
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.crypto.ErrorReadingKey", path.toString(), ex.getMessage());
        }
    }

    private void readTrustFile(Path path) {
        try {
            List<PEMFileReader.PEMEntry> entries = PEMFileReader.readPEMFile(path.toString());
            if (entries.size() == 0) {
                log.warn(Markers.REPO, "Certificate file " + String.valueOf(path) + " must contain at least one certificate. No certificate has been added to the trust store.");
                return;
            }
            KeyStore trustKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustKeyStore.load(null, null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            int certIdx = 0;
            for (PEMFileReader.PEMEntry entry : entries) {
                this.cryptoHash += entry.hashCode();
                if (this.cryptoHash == 0) {
                    this.cryptoHash = 42;
                }
                Certificate caCert = cf.generateCertificate(new ByteArrayInputStream(entry.bytes));
                trustKeyStore.setCertificateEntry("ca-" + ++certIdx, caCert);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustKeyStore);
            this.addTrustManagers(tmf.getTrustManagers());
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.crypto.ErrorReadingPEM", path.toString(), ex.getMessage());
        }
    }
}
