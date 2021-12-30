package com.gt.toolbox.spb.webapps.commons.infra.model;

public interface SelectableLazyDMFiller<E> extends LazyDMFiller<E> {

	E findById(Object id);
}
