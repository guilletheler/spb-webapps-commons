package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Implementación los dto de mapas
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeyValueDto<K, V> implements Serializable {
    
    public static final long serialVersionUID = 1L;

    protected K key;

    protected V value;
}
