package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.io.Serializable;

/**
 * Implementación los dto de mapas
 */
public class KeyValueDto<K, V> implements Serializable {

    public static final long serialVersionUID = 1L;

    protected K key;

    protected V value;

    /**
     * 
     */
    public KeyValueDto() {}

    /**
     * @param key
     * @param value
     */
    public KeyValueDto(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @return the key
     */
    public K getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(K key) {
        this.key = key;
    }

    /**
     * @return the value
     */
    public V getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(V value) {
        this.value = value;
    }

}
