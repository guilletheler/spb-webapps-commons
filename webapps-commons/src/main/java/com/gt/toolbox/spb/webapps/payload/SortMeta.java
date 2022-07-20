package com.gt.toolbox.spb.webapps.payload;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortMeta implements Serializable {

    public final static long serialVersionUID = 1L;

    String field;
    SortDirection direction;

    public enum SortDirection {
        @JsonProperty("asc")
        ASC,
        @JsonProperty("desc")
        DESC,
        @JsonProperty("none")
        NONE;
    }
}
