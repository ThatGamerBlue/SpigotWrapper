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

import lombok.Getter;

public abstract class Plugin
{

	@Getter
	private PluginManifest manifest;

	public abstract void init();

	public abstract byte[] onClassLoaded(final byte[] p0, final String p1);

}
