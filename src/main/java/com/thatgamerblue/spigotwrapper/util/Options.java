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

import com.beust.jcommander.Parameter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class Options
{

	@Getter(AccessLevel.NONE)
	@Parameter(names = { "-?", "-h", "--help" }, description = "Print help message and exit (default = false)", help = true)
	private boolean help = false;
	@Parameter(names = { "-s", "--jarfile" }, description = "Spigot .jar file relative to execution directory (default = \"server.jar\"")
	private String spigotPath = "server.jar";
	@Parameter(names = { "-l", "--log" }, description = "Logging level (0 = fatal, 1 = warn, 2 = info, 3 = debug) (default = 2")
	private int loggingLevel = 2;
	@Parameter(names = { "-j", "--jvmarg" }, description = "Arguments to pass to the JVM. Usage -j arg1 -j arg2 -j arg3 (default = whatever you pass to this jar on the command line, minus javaagents)")
	private List<String> jvmArguments = new ArrayList<>();
	@Parameter(names = { "-a", "--arg" }, description = "Arguments to pass to spigot. Usage: -a arg1 -a arg2 -a arg3 (default = nothing)")
	private List<String> spigotArguments = new ArrayList<>();

	public boolean getHelp() {
		return this.help;
	}

}
