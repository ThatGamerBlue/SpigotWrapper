/*
 * This file is part of the SpigotWrapper distribution (https://github.com/ThatGamerBlue/SpigotWrapper).
 * Copyright (c) 2020 ThatGamerBlue.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.thatgamerblue.spigotwrapper.util;

import com.thatgamerblue.spigotwrapper.plugins.PluginManager;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils
{

	private static final Comparator<String> directoriesFirst = ((a, b) -> {
		if (a.endsWith("/") && !b.endsWith("/")) {
			return -1;
		}
		else if (!a.endsWith("/") && b.endsWith("/")) {
			return 1;
		}
		else {
			return a.compareTo(b);
		}
	});

	@Getter
	private static MessageDigest sha256Digest;

	public static TreeMap<String, byte[]> readAllEntries(final File zipFile) {
		final TreeMap<String, byte[]> jarEntries = new TreeMap<>(Utils.directoriesFirst);
		try {
			final ZipFile zf = new ZipFile(zipFile);
			final Enumeration<? extends ZipEntry> entries = zf.entries();
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final byte[] tmp = new byte[1024];
			while (entries.hasMoreElements()) {
				final ZipEntry meta = entries.nextElement();
				baos.reset();
				final InputStream stream = zf.getInputStream(meta);
				int readBytes;
				while ((readBytes = stream.read(tmp)) > 0) {
					baos.write(tmp, 0, readBytes);
				}
				jarEntries.put(meta.getName(), baos.toByteArray());
			}
		}
		catch (IOException ex) {
			Globals.getLogger().fatal("%s", Logger.exceptionToString(ex));
			System.exit(-1);
		}
		return jarEntries;
	}

	public static String hashPluginsAndServer() throws IOException {
		final String calculatedHash = sha256(PluginManager.PLUGIN_DIRECTORY);
		return sha256((calculatedHash + sha256(new File(Globals.getOptions().getSpigotPath()))).getBytes());
	}

	public static String sha256(final File fileIn) throws IOException {
		String hash = "";
		if (fileIn.isDirectory()) {
			final File[] files = fileIn.listFiles();
			Arrays.sort(files);
			for (final File f2 : files) {
				hash = sha256((hash + sha256(Files.readAllBytes(f2.toPath()))).getBytes());
			}
		}
		else {
			hash = sha256(Files.readAllBytes(fileIn.toPath()));
		}
		return hash;
	}

	public static String sha256(final byte[] bytes) {
		return bytesToHex(Utils.sha256Digest.digest(bytes));
	}

	private static String bytesToHex(final byte[] hash) {
		final StringBuilder hexString = new StringBuilder();
		for (int i = 0; i < hash.length; ++i) {
			final String hex = Integer.toHexString(0xFF & hash[i]);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString().toLowerCase();
	}

	static {
		try {
			Utils.sha256Digest = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e) {
			Globals.getLogger().fatalUnformatted("Failed to create sha-256 instance, disabling caching");
			Utils.sha256Digest = null;
		}
	}

}
