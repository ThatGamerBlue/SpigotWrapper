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
package com.thatgamerblue.spigotwrapper.instrumentation;

import com.thatgamerblue.spigotwrapper.util.Globals;
import com.thatgamerblue.spigotwrapper.util.Logger;
import com.thatgamerblue.spigotwrapper.util.Utils;
import lombok.Getter;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

@Getter
public class ServerJar
{
	private String version;
	private final TreeMap<String, byte[]> jarContents;
	private final Manifest manifest;

	@SneakyThrows
	public ServerJar(final TreeMap<String, byte[]> jarContents, final Manifest manifest, final String jarName)
	{
		this.jarContents = jarContents;
		this.manifest = manifest;
		if (!jarContents.containsKey("version.json"))
		{
			Globals.getLogger().fatal("%s", Logger.exceptionToString(new IllegalArgumentException(
				String.format("JAR file %s is likely not a spigot jar, missing version.json", jarName))));
			for (final Map.Entry<String, byte[]> entry : jarContents.entrySet())
			{
				final File f = new File("dump/" + entry.getKey());
				f.mkdirs();
				if (!entry.getKey().endsWith("/"))
				{
					f.delete();
					Files.write(f.toPath(), entry.getValue(), StandardOpenOption.CREATE_NEW);
				}
			}
			System.exit(-1);
		}
		final byte[] bVersionContents = jarContents.get("version.json");
		final String jsonString = new String(bVersionContents, StandardCharsets.UTF_8);
		Globals.getLogger().debug("%s", jsonString);
		try
		{
			final JSONObject rootObject = (JSONObject) Globals.getJsonParser().parse(jsonString);
			this.version = (String) rootObject.get("id");
			Globals.getLogger().info("Detected version: %s", this.getVersion());
		}
		catch (ParseException ex)
		{
			Globals.getLogger()
				.fatal("%s%n%s%s%n%s", "Failed to parse version.json", "File contents (base64 encoded): ", Base64
					.getEncoder().encodeToString(bVersionContents), Logger.exceptionToString(ex));
			System.exit(-1);
		}
	}

	public void createZipFile(final File destination) throws IOException
	{
		destination.getParentFile().mkdirs();
		if (destination.exists())
		{
			destination.delete();
		}
		Globals.getLogger().info("Writing jar file...");
		try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(destination), this.manifest))
		{
			for (final Map.Entry<String, byte[]> entry : this.jarContents.entrySet())
			{
				final String name = entry.getKey().replace("\\", "/");
				if (name.equals("META-INF/MANIFEST.MF"))
				{
					continue;
				}
				final JarEntry jarEntry = new JarEntry(name);
				outputStream.putNextEntry(jarEntry);
				if (!name.endsWith("/"))
				{
					outputStream.write(entry.getValue(), 0, entry.getValue().length);
				}
				outputStream.closeEntry();
			}
		}
		final File hashFile = new File("cache/plugins.sha256");
		final String calculatedHash = Utils.hashPluginsAndServer();
		hashFile.delete();
		Files.write(hashFile.toPath(), calculatedHash.getBytes(), StandardOpenOption.CREATE_NEW);
		Globals.getLogger().info("Done!");
	}

	public static class Loader
	{
		private File zipFile;

		public ServerJar load()
		{
			if (this.zipFile == null)
			{
				throw new IllegalStateException("You must call zipFile() with a non-null value before calling load()");
			}
			Manifest manifest = null;
			try
			{
				final FileInputStream fis = new FileInputStream(this.zipFile);
				try
				{
					final JarInputStream jis = new JarInputStream(fis);
					try
					{
						manifest = jis.getManifest();
					}
					finally
					{
						if (Collections.singletonList(jis).get(0) != null)
						{
							jis.close();
						}
					}
				}
				finally
				{
					if (Collections.singletonList(fis).get(0) != null)
					{
						fis.close();
					}
				}
			}
			catch (IOException ex)
			{
				Globals.getLogger().fatal("Failed to get manifest from jar file%n%s", Logger.exceptionToString(ex));
				System.exit(-1);
			}
			return new ServerJar(Utils.readAllEntries(this.zipFile), manifest, this.zipFile.getName());
		}

		public Loader zipFile(final File zipFile)
		{
			this.zipFile = zipFile;
			return this;
		}
	}

}
