package com.gt.toolbox.spb.webapps.commons.infra.datamodel;

import java.util.Objects;

/**
 * Created by rmpestano on 10/31/14.
 */
public enum SortOrder {

    ASCENDING, DESCENDING, UNSORTED;

    public boolean isAscending() {
        return Objects.equals(this, ASCENDING);
    }
}
