package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClaveValorDto<K, V> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	K clave;
	V valor;
}
