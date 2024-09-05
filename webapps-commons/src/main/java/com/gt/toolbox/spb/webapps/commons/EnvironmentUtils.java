package com.gt.toolbox.spb.webapps.commons;

import java.io.File;
import java.security.CodeSource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentUtils {

	private static final Logger LOG =
			LoggerFactory.getLogger(EnvironmentUtils.class);

	public static void setAppHome(Class<?> mainClass) {

		String jarDir = "";

		File jarFile = getJarFile(mainClass);

		if (jarFile == null || !jarFile.exists()) {
			return;
		}

		File jarFolder = null;
		jarFolder = jarFile.getParentFile();

		if (jarFolder.getName().equals("target")) {
			// Supongo entorno de desarrollo
			jarFolder = jarFolder.getParentFile();
		}

		jarDir = jarFolder.getPath();

		if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
			if (!jarDir.startsWith("/")) {
				jarDir = "/" + jarDir;
			}
		}

		LOG.info("Seteando app.home en '{}'", jarDir);
		System.setProperty("app.home", jarDir);

	}

	public static LocalDateTime getCompiledDate(Class<?> mainClass) {
		var file = getJarFile(mainClass);
		return Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault())
				.toLocalDateTime();
	}

	public static File getJarFile(Class<?> mainClass) {
		CodeSource codeSource = mainClass.getProtectionDomain().getCodeSource();

		File jarFile = null;
		try {
			String path = codeSource.getLocation().getPath();
			var fileName = "";
			if (path.startsWith("file:/") && path.contains("!")) {
				fileName = path.substring(5, path.indexOf("!") - 1);
			} else if (path.startsWith("nested:/") && path.contains("!")) {
				fileName = path.substring(7, path.indexOf("!") - 1);
			} else {
				fileName = codeSource.getLocation().toURI().getPath();
			}

			if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
				if (!fileName.startsWith("/")) {
					fileName = "/" + fileName;
				}
			}

			jarFile = new File(fileName);


			if (jarFile == null || !jarFile.exists()) {
				throw new RuntimeException(
						"El archivo " + fileName + " no existe, codeSource.location: '" + codeSource
								.getLocation().getPath() + "'");
			}
		} catch (Exception ex) {
			LOG.error("Error buscando jar",
					ex);
		}


		return jarFile;
	}
}
