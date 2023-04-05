package com.gt.toolbox.spb.pf.commons.infra.utils;

import java.util.List;

import javax.faces.application.FacesMessage;

import org.omnifaces.util.Messages;


public class Utils extends com.gt.toolbox.spb.webapps.commons.infra.utils.Utils {
    

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
}
