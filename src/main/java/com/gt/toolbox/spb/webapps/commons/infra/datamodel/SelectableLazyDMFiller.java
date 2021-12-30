package com.gt.toolbox.spb.webapps.commons.infra.datamodel;

public interface SelectableLazyDMFiller<E> extends LazyDMFiller<E> {

	E findById(Object id);
}
