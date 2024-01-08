package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;

@Log
public class JsonObjectConverterTest {

    @Test
    public void test() throws JsonProcessingException {
        var loc = buildLocalidad();

        var mapper = new ObjectMapper();

        mapper.setAnnotationIntrospector(
                new JsonPropertyFilter(LocalidadDto.class, Arrays.asList("provincia")));

        var dtoAsString = mapper.writer().writeValueAsString(loc);

        log.info("Localidad serializada");
        log.info(dtoAsString);
    }

    public static List<LocalidadDto> buildLocalidad() {
        PaisDto pais = new PaisDto();
        pais.setCodigo(54);
        pais.setNombre("Argentina");

        ProvinciaDto prov = new ProvinciaDto();
        prov.setCodigo(12);
        prov.setNombre("Santa Fe");
        prov.setPais(pais);


        List<LocalidadDto> locs = new ArrayList<>();
        LocalidadDto loc = new LocalidadDto();
        loc.setId(1);
        loc.setCodigo(1);
        loc.setCodigoPostal("3080");
        loc.setNombre("Esperanza");
        loc.setPrefijoTelefonico("03496");
        loc.setProvincia(prov);

        locs.add(loc);
        loc = new LocalidadDto();
        loc.setId(2);
        loc.setCodigo(1);
        loc.setCodigoPostal("3000");
        loc.setNombre("Santa Fe");
        loc.setPrefijoTelefonico("0342");
        loc.setProvincia(prov);
        locs.add(loc);

        return locs;
    }
}
