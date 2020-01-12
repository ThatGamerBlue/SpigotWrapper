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

import lombok.SneakyThrows;
import org.fusesource.jansi.Ansi;
import sun.reflect.Reflection;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger
{
	public static final int LEVEL_DEBUG = 3;
	public static final int LEVEL_INFO = 2;
	public static final int LEVEL_WARN = 1;
	public static final int LEVEL_FATAL = 0;

	public static String getLogLevelString(final int level)
	{
		switch (level)
		{
			case LEVEL_FATAL:
				return "FATAL";
			case LEVEL_WARN:
				return "WARN";
			case LEVEL_INFO:
				return "INFO";
			case LEVEL_DEBUG:
				return "DEBUG";
			default:
				return null;
		}
	}

	public void printLicense()
	{
		System.out.println("\tSpigotWrapper  Copyright (C) 2020  ThatGamerBlue");
		System.out.println("\tThis program comes with ABSOLUTELY NO WARRANTY,");
		System.out.println("\tThis is free software, and you are welcome to redistribute it");
		System.out.println("\tunder certain conditions.");
		System.out.println("\tSee https://www.gnu.org/licenses/ for details");
		System.out.println("\tAlternatively, see LICENSE in the root of this jar file.\n");
	}

	public void debug(final String fmt, final Object... objs)
	{
		if (Globals.getOptions().getLoggingLevel() >= LEVEL_DEBUG)
		{
			System.out.println("[DEBUG " + this.getCallingClass() + "] " + String.format(fmt, objs));
		}
	}

	public void info(final String fmt, final Object... objs)
	{
		if (Globals.getOptions().getLoggingLevel() >= LEVEL_INFO)
		{
			System.out.println("[INFO " + this.getCallingClass() + "] " + String.format(fmt, objs));
		}
	}

	public void warn(final String fmt, final Object... objs)
	{
		if (Globals.getOptions().getLoggingLevel() >= LEVEL_WARN)
		{
			System.out.println(
				Ansi.ansi().fgBrightYellow().a("[WARN " + this.getCallingClass() + "] " + String.format(fmt, objs))
					.reset());
		}
	}

	public void fatal(final String fmt, final Object... objs)
	{
		if (Globals.getOptions().getLoggingLevel() >= LEVEL_FATAL)
		{
			System.out.println(
				Ansi.ansi().fgBrightRed().a("[FATAL " + this.getCallingClass() + "] " + String.format(fmt, objs))
					.reset());
		}
	}

	public void fatalUnformatted(final String fmt, final Object... objs)
	{
		if (Globals.getOptions().getLoggingLevel() >= LEVEL_FATAL)
		{
			System.out.println("[FATAL " + this.getCallingClass() + "] " + String.format(fmt, objs));
		}
	}

	private String getCallingClass()
	{
		int i = 0;
		try
		{
			while (Reflection.getCallerClass(++i) == Logger.class)
			{
				if (i == 10)
				{
					return "FAILED TO GET CALLING CLASS";
				}
				if (i == 11)
				{
					throw new ClassNotFoundException("this is here to trick the compiler");
				}
				if (i == 12)
				{
					throw new NoSuchMethodException("this is here to trick the compiler");
				}
			}
			return Reflection.getCallerClass(i).getName();
		}
		catch (ClassNotFoundException | NoSuchMethodException ex3)
		{
			return "FAILED TO GET CALLING CLASS";
		}
	}

	@SneakyThrows
	public static String exceptionToString(final Exception ex)
	{
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		final String sStackTrace = sw.toString();
		sw.close();
		pw.close();
		return sStackTrace;
	}

}
