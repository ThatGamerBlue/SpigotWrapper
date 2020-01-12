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
package com.thatgamerblue.spigotwrapper.plugins;

import com.thatgamerblue.spigotwrapper.util.Globals;
import com.thatgamerblue.spigotwrapper.util.Logger;
import com.thatgamerblue.spigotwrapper.util.Utils;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PluginManager
{
	public static final File PLUGIN_DIRECTORY = new File("wrapper-plugins/");
	private final List<Plugin> loadedPlugins = new ArrayList<>();
	private boolean hasLoaded = false;
	private URLClassLoader pluginClassLoader = null;

	public void init()
	{
		this.init(false);
	}

	public void init(final boolean loadPlugins)
	{
		if (!PluginManager.PLUGIN_DIRECTORY.exists())
		{
			Globals.getLogger().info("Plugins folder not detected");
			PluginManager.PLUGIN_DIRECTORY.mkdirs();
			Globals.getLogger().info("Done!");
		}
		else if (!PluginManager.PLUGIN_DIRECTORY.isDirectory())
		{
			Globals.getLogger()
				.fatal("wrapper-plugins is not a directory! Please remove the file before continuing.");
			System.exit(-1);
		}
		if (!PluginManager.PLUGIN_DIRECTORY.canRead() || !PluginManager.PLUGIN_DIRECTORY.canExecute())
		{
			Globals.getLogger().fatal(
				"wrapper-plugins needs to be readable and executable by the current user. Please resolve this and re-run.");
			System.exit(-1);
		}
		final Map<File, PluginManifest> validPlugins = new HashMap<>();
		for (final File file : PluginManager.PLUGIN_DIRECTORY.listFiles())
		{
			if (!file.getName().endsWith(".jar"))
			{
				Globals.getLogger().warn("Found non-jar file in wrapper-plugins folder, %s", file.getName());
			}
			else
			{
				final TreeMap<String, byte[]> contents = Utils.readAllEntries(file);
				if (!contents.containsKey("manifest.json"))
				{
					Globals.getLogger().fatal("File %s is not a plugin file, missing manifest.json", file.getName());
				}
				else
				{
					final byte[] bManifestContents = contents.get("manifest.json");
					final String jsonString = new String(bManifestContents, StandardCharsets.UTF_8);
					try
					{
						final JSONObject jsonRoot = (JSONObject) Globals.getJsonParser().parse(jsonString);
						final String author = (String) jsonRoot.get("author");
						final String name = (String) jsonRoot.get("name");
						final String version = (String) jsonRoot.get("version");
						final String mainClass = (String) jsonRoot.get("main_class");
						final PluginManifest manifest = new PluginManifest(name, version, author, mainClass);
						validPlugins.put(file, manifest);
					}
					catch (ParseException ex)
					{
						Globals.getLogger().fatal("Failed to parse manifest.json in %s, skipping%n%s", file.getName(),
							Logger.exceptionToString(ex));
					}
				}
			}
		}
		if (loadPlugins)
		{
			this.loadPlugins(validPlugins);
		}
	}

	@SneakyThrows
	private URL fileToURL(final File f)
	{
		return f.toURI().toURL();
	}

	@SneakyThrows
	public void loadPlugins(final Map<File, PluginManifest> validPlugins)
	{
		if (this.hasLoaded)
		{
			Globals.getLogger().fatal("You can only load plugins once! Ignoring.");
			return;
		}
		this.hasLoaded = true;
		if (validPlugins.size() == 0)
		{
			Globals.getLogger().warn("No plugins found to load. Skipping.");
			return;
		}
		Globals.getLogger().info("Creating class loader...");
		final Object[] fileAryObj =
			validPlugins.keySet().stream().map(this::fileToURL).toArray();
		this.pluginClassLoader = new URLClassLoader(
			Arrays.copyOf(fileAryObj, fileAryObj.length, URL[].class),
			this.getClass().getClassLoader());
		Globals.getLogger().info("Loading plugins...");
		int loadedPluginCount = 0;
		for (final Map.Entry<File, PluginManifest> keyValueSet : validPlugins.entrySet())
		{
			final File pluginFile = keyValueSet.getKey();
			final PluginManifest manifest = keyValueSet.getValue();
			Globals.getLogger().info("Loading plugin %s v%s by %s", manifest.getName(), manifest.getVersion(),
				manifest.getAuthor());
			Class clazz;
			try
			{
				clazz = this.pluginClassLoader.loadClass(manifest.getMainClass().replace("/", "."));
			}
			catch (ClassNotFoundException e2)
			{
				Globals.getLogger()
					.fatal("Couldn't find main class %s for plugin %s, report this to the author!",
						manifest.getMainClass(), manifest.getName());
				return;
			}
			if (clazz.getSuperclass() != Plugin.class)
			{
				Globals.getLogger()
					.fatal("Plugin class %s from %s doesn't extend Plugin! Skipping.", manifest.getMainClass(),
						manifest.getName());
				return;
			}
			Plugin instance;
			try
			{
				instance = (Plugin) clazz.getConstructor().newInstance();
			}
			catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException ex2)
			{
				Globals.getLogger()
					.fatal("Couldn't instantiate class %s from %s! Skipping.", manifest.getMainClass(),
						manifest.getName());
				return;
			}
			final Field manifestField = Plugin.class.getDeclaredField("manifest");
			final boolean manifestFieldFlag = manifestField.isAccessible();
			manifestField.setAccessible(true);
			manifestField.set(instance, manifest);
			manifestField.setAccessible(manifestFieldFlag);
			instance.init();
			this.loadedPlugins.add(instance);
			++loadedPluginCount;
		}
		Globals.getLogger().info("Loaded %d plugins.", loadedPluginCount);
	}

	public byte[] onClassLoaded(final byte[] originalBytes, final String className)
	{
		byte[] transformedBytes = originalBytes;
		for (final Plugin plugin : this.loadedPlugins)
		{
			transformedBytes = plugin.onClassLoaded(transformedBytes, className);
		}
		return transformedBytes;
	}

	public boolean validatePluginCache(final File finalJar)
	{
		if (Utils.getSha256Digest() == null)
		{
			Globals.getLogger().warn("SHA256 instance is null");
			return false;
		}
		if (!finalJar.exists() || !finalJar.canExecute())
		{
			Globals.getLogger().debug("Final JAR doesn't exist");
			return false;
		}
		try
		{
			final File hashFile = new File("cache/plugins.sha256");
			if (!hashFile.exists() || hashFile.length() != 64L)
			{
				Globals.getLogger().warn("Stored hash is invalid");
				return false;
			}
			final String storedHash = new String(Files.readAllBytes(hashFile.toPath()), StandardCharsets.UTF_8);
			final String calculatedHash = Utils.hashPluginsAndServer();
			if (calculatedHash.equalsIgnoreCase(storedHash))
			{
				Globals.getLogger().info("Using cached jar - hashes match");
				return true;
			}
		}
		catch (IOException e)
		{
			Globals.getLogger().warn("Failed to check hash of cached plugins");
			return false;
		}
		Globals.getLogger().info("Hashes didn't match");
		return false;
	}

}
