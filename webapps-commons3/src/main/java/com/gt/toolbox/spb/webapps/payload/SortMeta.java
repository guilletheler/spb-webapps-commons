package com.gt.toolbox.spb.webapps.payload;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortMeta implements Serializable {

    public final static long serialVersionUID = 1L;

    String field;
    SortDirection direction;

    public enum SortDirection {
        @JsonProperty("asc")
        ASC, @JsonProperty("desc")
        DESC, @JsonProperty("none")
        NONE;

        @JsonCreator
        public static SortDirection forName(String name) {
            if (name.equalsIgnoreCase("asc")) {
                return SortDirection.ASC;
            } else if (name.equalsIgnoreCase("desc")) {
                return SortDirection.DESC;
            } else {
                return SortDirection.NONE;
            }
        }
    }
}
