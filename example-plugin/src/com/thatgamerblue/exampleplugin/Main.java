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
package com.thatgamerblue.exampleplugin;

import com.thatgamerblue.spigotwrapper.plugins.Plugin;
import com.thatgamerblue.spigotwrapper.util.Globals;
import org.objectweb.asm.Opcodes;

public class Main extends Plugin implements Opcodes
{
	public void init()
	{
		Globals.getLogger().info("Hello from example!");
	}

	public byte[] onClassLoaded(byte[] bytes, String fullClassName)
	{
		if (fullClassName.startsWith("net.minecraft.server"))
		{if (fullClassName.endsWith("EntitySlime"))
			{
				return (new EntitySlimeAugmentor(bytes, fullClassName)).run();
			}
		}

		return bytes;
	}
}
