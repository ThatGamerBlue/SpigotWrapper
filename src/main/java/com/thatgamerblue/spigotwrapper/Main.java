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
package com.thatgamerblue.spigotwrapper;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Strings;
import com.thatgamerblue.spigotwrapper.instrumentation.ServerJar;
import com.thatgamerblue.spigotwrapper.plugins.PluginManager;
import com.thatgamerblue.spigotwrapper.util.Globals;
import com.thatgamerblue.spigotwrapper.util.Logger;
import lombok.SneakyThrows;
import org.fusesource.jansi.AnsiConsole;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main
{
	private static final JCommander optionParser = JCommander.newBuilder().addObject(Globals.getOptions()).build();

	@SneakyThrows
	public static void main(final String[] args)
	{
		checkWindowsVersion();
		AnsiConsole.systemInstall();
		try
		{
			optionParser.parse(args);
		}
		catch (ParameterException ex)
		{
			printUsage(-1);
		}
		if (System.getenv("ENABLE_DEBUG_LOG") != null)
		{
			Globals.getOptions().setLoggingLevel(3);
		}
		if (Globals.getOptions().getHelp())
		{
			printUsage(0);
		}
		if (Globals.getOptions().getLoggingLevel() > 3 || Globals.getOptions().getLoggingLevel() < 0)
		{
			Globals.getLogger()
				.warn("Logging level out of range (expected: %d-%d, found: %d)%n       Defaulting to INFO", 0, 3,
					Globals.getOptions().getLoggingLevel());
			Globals.getOptions().setLoggingLevel(2);
		}
		System.out.printf("[INFO com.thatgamerblue.spigotwrapper.Main] Logging level: %s%n", Logger
			.getLogLevelString(Globals.getOptions().getLoggingLevel()));
		final ServerJar jarFile = new ServerJar.Loader().zipFile(new File(Globals.getOptions().getSpigotPath())).load();
		final PluginManager pluginManager = new PluginManager();
		pluginManager.init(true);
		final File cacheDir = new File("cache/");
		cacheDir.mkdirs();
		final File finalJar = new File(cacheDir, "augmented-spigot.jar");
		if (!pluginManager.validatePluginCache(finalJar))
		{
			Globals.getLogger().info("Augmenting classes");
			for (final Map.Entry<String, byte[]> stringEntry : jarFile.getJarContents().entrySet())
			{
				if (stringEntry.getKey().endsWith(".class"))
				{
					String className = stringEntry.getKey().replace("/", ".");
					className = className.substring(0, className.length() - 6);
					stringEntry.setValue(pluginManager.onClassLoaded(stringEntry.getValue(), className));
				}
			}
			jarFile.createZipFile(finalJar);
		}
		else
		{
			Globals.getLogger().info("Using cached augmented jar");
		}
		final long reservedRam = 0x8000000L;
		final long newProcessRam = Runtime.getRuntime().maxMemory() - reservedRam;
		if (Globals.getOptions().getJvmArguments().size() == 0)
		{
			Globals.getOptions().setJvmArguments(ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
				.filter(s -> !s.startsWith("-javaagent:"))
				.collect(Collectors.toList()));
		}
		final List<String> newProcessArgs = new ArrayList<>();
		newProcessArgs.add("java");
		newProcessArgs.add("-Xms" + newProcessRam);
		newProcessArgs.add("-Xmx" + newProcessRam);
		newProcessArgs.addAll(Globals.getOptions().getJvmArguments());
		newProcessArgs.add("-jar");
		newProcessArgs.add(finalJar.getAbsolutePath());
		newProcessArgs.addAll(Globals.getOptions().getSpigotArguments());
		Globals.getLogger().debug("Running command: %s", Strings.join(" ", newProcessArgs));
		System.out.printf("%n-------- SPIGOT START --------%n%n");
		final ProcessBuilder pb = new ProcessBuilder(newProcessArgs).inheritIO();
		final Process proc = pb.start();
		proc.waitFor();
		AnsiConsole.systemUninstall();
		final int exitValue = proc.exitValue();
		if (exitValue != 0)
		{
			finalJar.delete();
		}
		System.exit(proc.exitValue());
	}

	private static void printUsage(final int exitCode)
	{
		final StringBuilder sb = new StringBuilder();
		Main.optionParser.getUsageFormatter().usage(sb);
		System.out.println(sb.toString());
		if (exitCode != 1337)
		{
			System.exit(exitCode);
		}
	}

	private static void checkWindowsVersion()
	{
		final String s = System.getProperty("os.name");
		if (s != null && s.toLowerCase().contains("windows"))
		{
			if (Float.parseFloat(System.getProperty("os.version")) >= 6.0f)
			{
				return;
			}
			Globals.getLogger().fatalUnformatted("Windows XP and below are not supported - update to a later version to use this program.");
			System.exit(-1);
		}
	}
}
