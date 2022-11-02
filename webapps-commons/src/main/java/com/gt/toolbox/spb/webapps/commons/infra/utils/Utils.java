package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.faces.application.FacesMessage;

import org.omnifaces.util.Messages;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Created by rmpestano on 07/02/17.
 */
public class Utils implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final SimpleDateFormat SDF_ISO_HMS = new SimpleDateFormat("HHmmss");

	public static final SimpleDateFormat SDF_ISO_HM = new SimpleDateFormat("HHmm");

	public static final SimpleDateFormat SDF_HMS = new SimpleDateFormat("HH:mm:ss");

	public static final SimpleDateFormat SDF_SLASH_DM = new SimpleDateFormat("dd/MM");

	public static final SimpleDateFormat SDF_SLASH_DMY = new SimpleDateFormat("dd/MM/yy");

	public static final SimpleDateFormat SDF_SLASH_DMYY = new SimpleDateFormat("dd/MM/yyyy");

	public static final SimpleDateFormat SDF_SLASH_DMYHM = new SimpleDateFormat("dd/MM/yy HH:mm");;

	public static final SimpleDateFormat SDF_SLASH_DMYHMS = new SimpleDateFormat("dd/MM/yy HH:mm:ss");;

	public static final SimpleDateFormat SDF_SLASH_DMYYHM = new SimpleDateFormat("dd/MM/yyyy HH:mm");;

	public static final SimpleDateFormat SDF_SLASH_DMYYHMS = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public static final SimpleDateFormat SDF_ISO_YYMD = new SimpleDateFormat("yyyyMMdd");

	public static final SimpleDateFormat SDF_DMYY = new SimpleDateFormat("ddMMyyyy");

	public static final SimpleDateFormat SDF_DMY = new SimpleDateFormat("ddMMyy");

	public static final SimpleDateFormat SDF_MD = new SimpleDateFormat("MMdd");

	public static final SimpleDateFormat SDF_SLASH_ISO_YYMDHM = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	public static final SimpleDateFormat SDF_BAR_ISO_YYMDHMS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static final SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[] { SDF_SLASH_DMY, SDF_SLASH_DMYY,
			SDF_SLASH_DMYHM, SDF_SLASH_DMYYHM, SDF_SLASH_DMYHMS, SDF_SLASH_DMYYHMS };

	public static final DecimalFormat DF_2E = new DecimalFormat("00");

	public static final DecimalFormat DF_4E = new DecimalFormat("0000");

	public static final DecimalFormat DF_5E = new DecimalFormat("00000");

	public static final DecimalFormat DF_8E = new DecimalFormat("00000000");

	public static final DecimalFormat DF_2D = new DecimalFormat("0.00");

	public static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

	public static final Date DEFAULT_DATE = new Date(0L);

	public static <T> void addIfNotContains(List<T> list, T item) {
		if (!list.contains(item)) {
			list.add(0, item);
		}
	}

	public static void addDetailMessage(String message) {
		addDetailMessage(message, null);
	}

	public static void addDetailMessage(String message, FacesMessage.Severity severity) {

		FacesMessage facesMessage = Messages.create("").detail(message).get();
		if (severity != null && severity != FacesMessage.SEVERITY_INFO) {
			facesMessage.setSeverity(severity);
		}
		Messages.addFlash(null, facesMessage);
	}

	public static boolean isUserInRole(String role) {
		// get security context from thread local
		SecurityContext context = SecurityContextHolder.getContext();
		if (context == null)
			return false;

		Authentication authentication = context.getAuthentication();
		if (authentication == null)
			return false;

		for (GrantedAuthority auth : authentication.getAuthorities()) {
			if (Objects.equals(role, auth.getAuthority()))
				return true;
		}

		return false;
	}

	public static Calendar getFirstDayOfMonth(Integer month) {
		Calendar desde = Calendar.getInstance();

		desde.add(Calendar.MONTH, month);

		desde.set(Calendar.DAY_OF_MONTH, 1);

		desde.set(Calendar.MILLISECOND, 0);
		desde.set(Calendar.SECOND, 0);
		desde.set(Calendar.MINUTE, 0);
		desde.set(Calendar.HOUR, 0);

		return desde;
	}

	public static Calendar getLastDayOfMonth(int month) {
		return getLastDayOfMonth(getFirstDayOfMonth(month));
	}

	public static Date getLastDayOfMonth(Date desde) {
		Calendar cdesde = Calendar.getInstance();
		cdesde.setTime(desde);
		return getLastDayOfMonth(cdesde).getTime();
	}

	public static Calendar getLastDayOfMonth(Calendar desde) {
		Calendar hasta = Calendar.getInstance();
		hasta.setTimeInMillis(desde.getTimeInMillis());
		hasta.add(Calendar.MONTH, 1);
		hasta.add(Calendar.DAY_OF_MONTH, -1);

		return hasta;
	}

	public static Integer dateToIntYYMD(Date fecha) {
		if (fecha == null) {
			return null;
		}
		return Integer.valueOf(SDF_ISO_YYMD.format(fecha));
	}

	public static String[] separar(String descripcion, int largo, int delta) {
		if (descripcion == null || descripcion.isEmpty()) {
			return new String[] { "" };
		}

		List<String> ret = new ArrayList<>();

		String string = descripcion;

		while (string.length() > 0) {
			String tmp = getLine(string, largo, delta);
			ret.add(tmp.trim());
			string = string.substring(tmp.length());
		}

		return ret.toArray(new String[] {});
	}

	private static String getLine(String string, int largo, int delta) {

		String ret = string;
		if (string.length() > largo) {
			for (int pos = largo; pos >= 0; pos--) {
				char curChar = string.charAt(pos);
				if (!((curChar >= 'A' && curChar <= 'Z') ||
						(curChar >= 'a' && curChar <= '<') ||
						(curChar >= '0' && curChar <= '9'))) {
					ret = string.substring(0, pos);
					break;
				}
				if (largo - (largo - pos) > delta) {
					ret = string.substring(0, pos);
					break;
				}
			}
		}

		return ret;
	}

	public static String getExceptionNotificationText(Exception ex) {
		return getExceptionNotificationText(ex, null);
	}

	public static String getExceptionNotificationText(Exception ex, String packageName) {
		StringBuilder sb = new StringBuilder();

		sb.append("Excepción ")
				.append(ex.getClass().getSimpleName())
				.append("\n")
				.append(ex.getMessage());
		if (!ex.getMessage().equals(ex.getLocalizedMessage())) {
			sb.append("\n")
					.append(ex.getLocalizedMessage());
		}
		var ste = Arrays.asList(ex.getStackTrace())
				.stream()
				.filter(st -> packageName == null || st.getClassName().startsWith(packageName))
				.findFirst()
				.orElse(null);

		if (ste != null) {
			sb.append("\n")
					.append(ste.getClassName() + "." + ste.getMethodName() + ":" + ste.getLineNumber());

		}
		return sb.toString();
	}
}
