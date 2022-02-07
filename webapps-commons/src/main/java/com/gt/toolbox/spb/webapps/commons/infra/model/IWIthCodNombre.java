/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gt.toolbox.spb.webapps.commons.infra.model;

/**
 *
 * @author John Doe
 */
public interface IWIthCodNombre<T> {
	T getCodigo();
    void setCodigo(T codigo);
    
    String getNombre();
    void setNombre(String nombre);
}
