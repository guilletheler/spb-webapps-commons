package com.gt.toolbox.spb.webapps.commons.infra.model;

import java.util.Calendar;

import lombok.Getter;

public enum DiaSemana {
	LUNES(1, Calendar.MONDAY),
	MARTES(2, Calendar.TUESDAY),
	MIERCOLES(3, Calendar.WEDNESDAY),
	JUEVES(4, Calendar.THURSDAY),
	VIERNES(5, Calendar.FRIDAY),
	SABADO(6, Calendar.SATURDAY),
	DOMINGO(7, Calendar.SUNDAY);

	@Getter
	int codigo;
	
	@Getter
	int codigoCalendar;

	private DiaSemana(int codigo, int codigoCalendar) {
		this.codigo = codigo;
		this.codigoCalendar = codigoCalendar;
	}

	public static final DiaSemana[] LUNES_DOMINGO = new DiaSemana[] { DiaSemana.LUNES, DiaSemana.MARTES,
			DiaSemana.MIERCOLES, DiaSemana.JUEVES, DiaSemana.VIERNES, DiaSemana.SABADO, DiaSemana.DOMINGO };
	
	public static final DiaSemana[] LUNES_SABADO = new DiaSemana[] { DiaSemana.LUNES, DiaSemana.MARTES,
			DiaSemana.MIERCOLES, DiaSemana.JUEVES, DiaSemana.VIERNES, DiaSemana.SABADO };
	
	public static final DiaSemana[] LUNES_VIERNES = new DiaSemana[] { DiaSemana.LUNES, DiaSemana.MARTES,
			DiaSemana.MIERCOLES, DiaSemana.JUEVES, DiaSemana.VIERNES };
}
