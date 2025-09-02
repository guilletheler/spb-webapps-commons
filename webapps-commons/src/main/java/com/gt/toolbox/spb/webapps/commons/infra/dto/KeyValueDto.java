package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Implementación los dto de mapas
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class KeyValueDto<K, V> implements Serializable {

    public static final long serialVersionUID = 1L;

    protected K key;

    protected V value;

}
