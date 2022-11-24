package com.gt.toolbox.spb.webapps.commons;

import java.io.File;
import java.security.CodeSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnvironmentUtils {
    public static void setAppHome(Class<?> mainClass) {
		CodeSource codeSource = mainClass.getProtectionDomain().getCodeSource();

		String jarDir = "";

		try {
			File jarFile;
			String path = codeSource.getLocation().getPath();
			File jarFolder = null;
			if (path.startsWith("file:/") && path.contains("!")) {
				jarFile = new File(path.substring(6, path.indexOf("!") - 1));
			} else {
				jarFile = new File(codeSource.getLocation().toURI().getPath());
			}

			jarFolder = jarFile.getParentFile();

			if (jarFolder.getName().equals("target")) {
				// Supongo entorno de desarrollo
				jarFolder = jarFolder.getParentFile();
			}

			jarDir = jarFolder.getPath();
		} catch (Exception ex) {
			Logger.getLogger(mainClass.getName()).log(Level.SEVERE,
					"Error buscando jar path " + codeSource.getLocation().toString(), ex);
		}

		if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
			if (!jarDir.startsWith("/")) {
				jarDir = "/" + jarDir;
			}
		}

		System.setProperty("app.home", jarDir);

	}
}
