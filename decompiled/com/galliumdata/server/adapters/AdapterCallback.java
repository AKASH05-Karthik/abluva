/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.cache.CacheBuilder
 *  com.google.common.cache.CacheLoader
 *  com.google.common.cache.LoadingCache
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.apache.logging.log4j.util.Supplier
 *  org.graalvm.polyglot.Source
 *  org.graalvm.polyglot.proxy.ProxyObject
 */
package com.galliumdata.server.adapters;

import com.galliumdata.server.adapters.AdapterCallbackResponse;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.handler.mssql.MSSQLForwarder;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.logic.ConnectionFilter;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.logic.RequestFilter;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.metarepo.FilterImplementation;
import com.galliumdata.server.metarepo.MetaRepository;
import com.galliumdata.server.metarepo.MetaRepositoryManager;
import com.galliumdata.server.repository.FilterStage;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.util.Utils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.proxy.ProxyObject;

public class AdapterCallback {
    private Project project;
    private final Variables adapterContext = new Variables();
    private final Map<String, Boolean> requestFilterAvailability = new HashMap<String, Boolean>(20);
    private final Map<String, Boolean> responseFilterAvailability = new HashMap<String, Boolean>(20);
    public static final String DO_NOT_CALL = "_doNotCallFilters";
    private static final Logger log = LogManager.getLogger((String)"galliumdata.uselog");
    private static CacheLoader<String, Class<? extends ResponseFilter>> respFiltCacheLoader = new CacheLoader<String, Class<? extends ResponseFilter>>(){

        public Class<? extends ResponseFilter> load(String className) throws Exception {
            try {
                return Class.forName(className);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    };
    private static final LoadingCache<String, Class<? extends ResponseFilter>> responseFilterClassCache = CacheBuilder.newBuilder().maximumSize(100L).build(respFiltCacheLoader);

    public AdapterCallback(Project project) {
        this.project = project;
    }

    public AdapterCallbackResponse connectionRequested(Socket socket, Variables context) {
        AdapterCallbackResponse response = new AdapterCallbackResponse();
        response.reject = false;
        response.errorMessage = null;
        MetaRepository metarepo = MetaRepositoryManager.getMainRepository();
        for (FilterUse conFilter : this.project.getFilters(FilterStage.CONNECTION).values()) {
            if (!conFilter.isActive() || conFilter.getFilterType().equals("ConnectionCloseFilter")) continue;
            FilterImplementation filterImpl = metarepo.getFilterType(conFilter.getFilterType());
            if (filterImpl == null) {
                throw new RepositoryException("repo.UnknownFilter", conFilter.getName(), conFilter.getFilterType());
            }
            switch (filterImpl.getCodeType()) {
                case JAVA: {
                    ConnectionFilter filter = filterImpl.getConnectionFilter(conFilter);
                    context.put("filterContext", conFilter.getFilterContext());
                    context.put("projectContext", this.project.getProjectContext());
                    context.put("adapterContext", this.adapterContext);
                    context.put("utils", new Utils());
                    context.put("socket", socket);
                    context.put("log", log);
                    FilterResult filterResult = filter.acceptConnection(socket, context);
                    if (filterResult.isSuccess()) break;
                    response.reject = true;
                    response.errorMessage = filterResult.getErrorMessage();
                    if (!log.isTraceEnabled()) break;
                    log.trace(Markers.USER_LOGIC, "Filter {} has rejected connection with error message: {}", (Object)conFilter.getName(), (Object)filterResult.getErrorMessage());
                    break;
                }
                case JAVASCRIPT: {
                    FilterResult result = new FilterResult();
                    result.setSuccess(true);
                    result.setFilterName(conFilter.getName());
                    Source src = ScriptManager.getInstance().getSource(filterImpl.getPath().toString());
                    context.put("socket", socket);
                    FilterResult filterResult = new FilterResult();
                    context.put("log", log);
                    context.put("result", filterResult);
                    context.put("utils", new Utils());
                    context.put("parameters", ProxyObject.fromMap(conFilter.getParameters()));
                    context.put("adapterContext", this.adapterContext);
                    context.put("filterContext", conFilter.getFilterContext());
                    context.put("projectContext", this.project.getProjectContext());
                    context.put("threadContext", MSSQLForwarder.getThreadContext());
                    if (filterImpl.canHaveCode() && Files.exists(conFilter.getPath(), new LinkOption[0])) {
                        Source userSrc = ScriptManager.getInstance().getSource(conFilter.getPath().toString());
                        ScriptExecutor.executeFilterScript(userSrc, filterResult, context);
                    }
                    ScriptExecutor.executeFilterScript(src, filterResult, context);
                    if (filterResult.isSuccess()) break;
                    response.reject = true;
                    response.errorMessage = filterResult.getErrorMessage();
                    if (!log.isTraceEnabled()) break;
                    log.trace(Markers.USER_LOGIC, "Filter implementation {} for filter {} has rejected connection with error message: {}", (Object)filterImpl.getName(), (Object)conFilter.getName(), (Object)filterResult.getErrorMessage());
                    break;
                }
                default: {
                    throw new RepositoryException("repo.InvalidPropertyValue", new Object[]{"filterType", filterImpl.getCodeType(), filterImpl.getName()});
                }
            }
            if (!response.reject) continue;
            return response;
        }
        return response;
    }

    public void connectionClosing(Variables context) {
        MetaRepository metarepo = MetaRepositoryManager.getMainRepository();
        block3: for (FilterUse conFilter : this.project.getFilters(FilterStage.CONNECTION).values()) {
            if (!conFilter.isActive() || !conFilter.getFilterType().equals("ConnectionCloseFilter")) continue;
            FilterImplementation filterImpl = metarepo.getFilterType(conFilter.getFilterType());
            if (filterImpl == null) {
                throw new RepositoryException("repo.UnknownFilter", conFilter.getName(), conFilter.getFilterType());
            }
            switch (filterImpl.getCodeType()) {
                case JAVA: {
                    Variables ctxt = new Variables();
                    ctxt.put("connectionContext", context);
                    ConnectionFilter filter = filterImpl.getConnectionFilter(conFilter);
                    ctxt.put("filterContext", conFilter.getFilterContext());
                    ctxt.put("projectContext", this.project.getProjectContext());
                    ctxt.put("utils", new Utils());
                    ctxt.put("log", log);
                    filter.acceptConnection(null, ctxt);
                    continue block3;
                }
            }
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[]{"filterType", filterImpl.getCodeType(), filterImpl.getName()});
        }
    }

    public boolean hasFiltersForPacketType(FilterStage filterStage, String packetType) {
        Boolean avail;
        if (filterStage == FilterStage.REQUEST ? (avail = this.requestFilterAvailability.get(packetType)) != null : filterStage == FilterStage.RESPONSE && (avail = this.responseFilterAvailability.get(packetType)) != null) {
            return avail;
        }
        LinkedList<String> packetTypes = new LinkedList<String>();
        packetTypes.add(packetType);
        boolean avail2 = this.hasFiltersForPacketTypes(filterStage, packetTypes);
        if (filterStage == FilterStage.REQUEST) {
            this.requestFilterAvailability.put(packetType, avail2);
        }
        if (filterStage == FilterStage.RESPONSE) {
            this.responseFilterAvailability.put(packetType, avail2);
        }
        return avail2;
    }

    public void resetCache() {
        this.requestFilterAvailability.clear();
        this.responseFilterAvailability.clear();
    }

    public boolean hasFiltersForPacketTypes(FilterStage filterStage, Collection<String> packetTypes) {
        Map<String, FilterUse> filters = this.project.getFilters(filterStage);
        if (filters.size() == 0) {
            return false;
        }
        if (filterStage == FilterStage.CONNECTION) {
            for (FilterUse use : filters.values()) {
                if (!use.isActive()) continue;
                return true;
            }
        }
        for (FilterUse use : filters.values()) {
            if (!use.isActive()) continue;
            FilterImplementation filterImpl = MetaRepositoryManager.getMainRepository().getFilterType(use.getFilterType());
            String[] pktTypes = null;
            if (filterStage == FilterStage.REQUEST || filterStage == FilterStage.DUPLEX) {
                pktTypes = filterImpl.getRequestFilter(use).getPacketTypes();
            }
            if (filterStage == FilterStage.RESPONSE || filterStage == FilterStage.DUPLEX) {
                pktTypes = filterImpl.getResponseFilter(use).getPacketTypes();
            }
            if (pktTypes != null && pktTypes.length != 0) {
                if (!Arrays.stream(pktTypes).anyMatch(packetTypes::contains)) continue;
            }
            return true;
        }
        return false;
    }

    public AdapterCallbackResponse invokeRequestFilters(String packetType, Variables context) {
        AdapterCallbackResponse response = new AdapterCallbackResponse();
        context.put("projectContext", this.project.getProjectContext());
        context.put("utils", new Utils());
        context.put("log", log);
        block4: for (FilterUse reqFilter : this.project.getFilters(FilterStage.REQUEST).values()) {
            FilterResult result;
            if (!reqFilter.isActive()) continue;
            FilterImplementation filterImpl = MetaRepositoryManager.getMainRepository().getFilterType(reqFilter.getFilterType());
            if (filterImpl == null) {
                throw new RepositoryException("repo.UnknownFilter", reqFilter.getName(), reqFilter.getFilterType());
            }
            if (filterImpl.getEditions() != null && !filterImpl.getEditions().contains("Community")) {
                log.debug(Markers.DB2, "Filter \"" + reqFilter.getName() + "\" will not be executed because it is not allowed in this edition of Gallium Data.");
                continue;
            }
            String[] pktTypes = filterImpl.getRequestFilter(reqFilter).getPacketTypes();
            if (pktTypes != null && pktTypes.length > 0) {
                if (Arrays.stream(pktTypes).noneMatch(packetType::equals)) continue;
            }
            switch (filterImpl.getCodeType()) {
                case JAVA: {
                    context.put("filterContext", reqFilter.getFilterContext());
                    context.put("packetType", packetType);
                    RequestFilter filter = filterImpl.getRequestFilter(reqFilter);
                    pktTypes = filterImpl.getRequestFilter(reqFilter).getPacketTypes();
                    if (pktTypes != null && pktTypes.length > 0) {
                        if (Arrays.stream(pktTypes).noneMatch(packetType::equals)) continue block4;
                    }
                    result = filter.filterRequest(context);
                    break;
                }
                case JAVASCRIPT: {
                    Supplier[] supplierArray = new Supplier[1];
                    supplierArray[0] = reqFilter::getName;
                    log.trace(Markers.USER_LOGIC, "JavaScript request filter called: {}", supplierArray);
                    result = new FilterResult();
                    result.setSuccess(true);
                    result.setFilterName(reqFilter.getName());
                    Source src = ScriptManager.getInstance().getSource(reqFilter.getPath().toString());
                    result = new FilterResult();
                    context.put("result", result);
                    context.put("parameters", ProxyObject.fromMap(reqFilter.getParameters()));
                    context.put("filterContext", reqFilter.getFilterContext());
                    ScriptExecutor.executeFilterScript(src, result, context);
                    break;
                }
                default: {
                    throw new RepositoryException("repo.InvalidPropertyValue", new Object[]{"filterType", filterImpl.getCodeType(), filterImpl.getName()});
                }
            }
            if (response.response == null) {
                response.response = result.getResponse();
            } else if (result.getResponse() != null) {
                log.debug(Markers.USER_LOGIC, "Response has been set by more than one filter, the first one will be used");
            }
            if (result.isSkip()) {
                response.skip = true;
            }
            if (!result.isSuccess() || result.getErrorMessage() != null || result.getErrorCode() != 0) {
                response.reject = true;
                response.errorMessage = result.getErrorMessage();
                response.errorCode = result.getErrorCode();
                response.errorParameters = result.getErrorParameters();
                response.sqlStatus = result.getSqlStatus();
                response.errorResponse = result.getErrorResponse();
                response.closeConnection = result.isCloseConnection();
                response.logicName = reqFilter.getName();
            }
            if (!response.reject) continue;
            return response;
        }
        return response;
    }

    public AdapterCallbackResponse invokeResponseFilters(String packetType, Variables context) {
        AdapterCallbackResponse response = new AdapterCallbackResponse();
        response.reject = false;
        response.errorMessage = null;
        MetaRepository metarepo = MetaRepositoryManager.getMainRepository();
        context.put("projectContext", this.project.getProjectContext());
        context.put("utils", new Utils());
        context.put("log", log);
        context.put("packetType", packetType);
        Variables connCtxt = (Variables)context.get("connectionContext");
        Set doNotCallRules = (Set)connCtxt.get(DO_NOT_CALL);
        block6: for (FilterUse respFilter : this.project.getFilters(FilterStage.RESPONSE).values()) {
            if (!respFilter.isActive() || doNotCallRules != null && doNotCallRules.contains(respFilter.hashCode())) continue;
            FilterImplementation filterImpl = metarepo.getFilterType(respFilter.getFilterType());
            if (filterImpl == null) {
                throw new RepositoryException("repo.UnknownFilter", respFilter.getName(), respFilter.getFilterType());
            }
            switch (filterImpl.getCodeType()) {
                case JAVA: {
                    FilterResult result;
                    ResponseFilter filter = filterImpl.getResponseFilter(respFilter);
                    String[] pktTypes = filterImpl.getResponseFilter(respFilter).getPacketTypes();
                    if (pktTypes != null && pktTypes.length > 0) {
                        int i;
                        for (i = 0; i < pktTypes.length && !pktTypes[i].equals(packetType); ++i) {
                        }
                        if (i == pktTypes.length) continue block6;
                    }
                    context.put("filterContext", respFilter.getFilterContext());
                    try {
                        result = filter.filterResponse(context);
                        if (!result.isSuccess()) {
                            response.reject = true;
                            response.errorMessage = result.getErrorMessage();
                            response.errorCode = result.getErrorCode();
                            response.closeConnection = result.isCloseConnection();
                        }
                        if (result.isCloseConnection()) {
                            response.closeConnection = true;
                        }
                        if (!result.isDoNotCall()) break;
                        response.doNotCall = true;
                    }
                    catch (Exception ex) {
                        if (!log.isDebugEnabled()) break;
                        log.debug(Markers.USER_LOGIC, "Exception while executing filter " + filter.getName() + ": " + ex.getMessage());
                    }
                    break;
                }
                case JAVASCRIPT: {
                    Supplier[] supplierArray = new Supplier[1];
                    supplierArray[0] = respFilter::getName;
                    log.trace(Markers.USER_LOGIC, "JavaScript request filter called: {}", supplierArray);
                    FilterResult result = new FilterResult();
                    result.setSuccess(true);
                    result.setFilterName(respFilter.getName());
                    Source src = ScriptManager.getInstance().getSource(respFilter.getPath().toString());
                    context.putAll(context);
                    context.put("result", result);
                    context.put("parameters", ProxyObject.fromMap(respFilter.getParameters()));
                    context.put("filterContext", respFilter.getFilterContext());
                    ScriptExecutor.executeFilterScript(src, result, context);
                    if (result.isSuccess()) break;
                    response.reject = true;
                    response.errorMessage = result.getErrorMessage();
                    break;
                }
                default: {
                    throw new RepositoryException("repo.InvalidPropertyValue", new Object[]{"filterType", filterImpl.getCodeType(), filterImpl.getName()});
                }
            }
            if (!response.doNotCall || doNotCallRules == null) continue;
            doNotCallRules.add(respFilter.hashCode());
        }
        return response;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public static void flushCaches() {
        responseFilterClassCache.invalidateAll();
    }

    private static Class<? extends ResponseFilter> getResponseFilterClass(String name) {
        return (Class)responseFilterClassCache.getUnchecked((Object)name);
    }
}
