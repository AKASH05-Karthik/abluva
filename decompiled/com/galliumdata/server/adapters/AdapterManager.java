/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.adapters;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.adapters.AdapterInterface;
import com.galliumdata.server.metarepo.Adapter;
import com.galliumdata.server.metarepo.MetaRepositoryManager;
import com.galliumdata.server.repository.RepositoryException;

public class AdapterManager {
    private static final AdapterManager instance = new AdapterManager();

    public static AdapterManager getInstance() {
        return instance;
    }

    public AdapterInterface instantiateAdapter(String name) {
        AdapterInterface newAdapter;
        Adapter repoAdapter = MetaRepositoryManager.getMainRepository().getAdapters().get(name);
        if (repoAdapter == null) {
            throw new ServerException("repo.UnknownAdapter", name);
        }
        String adapterImplStr = repoAdapter.getImplementation();
        if (adapterImplStr.startsWith("java:")) {
            String clsName = adapterImplStr.substring("java:".length());
            try {
                Class<?> cls = Class.forName(clsName);
                newAdapter = (AdapterInterface)cls.getConstructor(new Class[0]).newInstance(new Object[0]);
            }
            catch (ClassNotFoundException cnfe) {
                throw new ServerException("repo.UnknownAdapterClass", name, clsName);
            }
            catch (Exception ex) {
                throw new ServerException("repo.CannotInstantiateAdapter", clsName, ex.getMessage());
            }
        } else {
            throw new RepositoryException("repo.InvalidAdapter", repoAdapter.getName(), adapterImplStr);
        }
        return newAdapter;
    }
}
