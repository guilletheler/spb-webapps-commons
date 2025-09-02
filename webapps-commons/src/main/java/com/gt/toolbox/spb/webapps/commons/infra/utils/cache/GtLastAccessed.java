package com.gt.toolbox.spb.webapps.commons.infra.utils.cache;

public interface GtLastAccessed {
    long getLastAccessed();

    void setLastAccessed(long time);
}
