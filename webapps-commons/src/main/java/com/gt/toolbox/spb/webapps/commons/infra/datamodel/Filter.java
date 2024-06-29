package com.gt.toolbox.spb.webapps.commons.infra.datamodel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Created by rmpestano on 9/7/14.
 * class which holds database pagination metadata
 */
public class Filter<T extends Serializable> {

    @Getter
    private T entity;
    @Getter
    private int first;
    @Getter
    private int pageSize;
    @Getter
    private String sortField;
    @Getter
    private SortOrder sortOrder;
    @Getter
    private Map<String, Object> params = new HashMap<String, Object>();

    public Filter() {
    }

    public Filter(T entity) {
        this.entity = entity;
    }

    public Filter<T> setFirst(int first) {
        this.first = first;
        return this;
    }

    public Filter<T> setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public Filter<T> setSortField(String sortField) {
        this.sortField = sortField;
        return this;
    }

    public Filter<T> setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public Filter<T> setParams(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public Filter<T> setEntity(T entity) {
        this.entity = entity;
        return this;
    }

    public Filter<T> addParam(String key, Object value) {
        getParams().put(key, value);
        return this;
    }

    public boolean hasParam(String key) {
        return getParams().containsKey(key) && getParam(key) != null;
    }

    public Object getParam(String key) {
        return getParams().get(key);
    }
}
