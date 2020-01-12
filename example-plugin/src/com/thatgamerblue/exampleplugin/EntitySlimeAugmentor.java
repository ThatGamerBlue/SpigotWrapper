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

import com.thatgamerblue.spigotwrapper.util.Globals;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Iterator;

public class EntitySlimeAugmentor implements Opcodes
{
	private final byte[] bytes;
	private final String fullClassName;
	private ClassReader cr;
	private ClassWriter cw;

	public EntitySlimeAugmentor(byte[] bytes, String fullClassName) {
		this.bytes = bytes;
		this.fullClassName = fullClassName;
	}

	protected byte[] run() {
		Globals.getLogger().info("Instrumenting %s", this.fullClassName);
		this.cr = new ClassReader(this.bytes);
		this.cw = new ClassWriter(this.cr, 3);

		ClassNode cn = new ClassNode(ASM7);
		this.cr.accept(cn, 8);
		MethodNode targetMethod = null;
		int foundMethods = 0;
		Iterator var4 = cn.methods.iterator();

		while(var4.hasNext()) {
			MethodNode methodNode = (MethodNode)var4.next();
			if ((methodNode.access & 8) == 8 && methodNode.desc.endsWith("Z")) {
				targetMethod = methodNode;
				++foundMethods;
			}
		}

		if (foundMethods > 1) {
			Globals.getLogger().fatal("Got more than 1 candidate for checkSlimeSpawnRules", new Object[0]);
			return this.bytes;
		} else if (targetMethod == null) {
			Globals.getLogger().fatal("Got no candidates for checkSlimeSpawnRules", new Object[0]);
			return this.bytes;
		} else {
			for(int i = 0; i < targetMethod.instructions.size(); ++i) {
				AbstractInsnNode abstractInsnNode = targetMethod.instructions.get(i);
				if (abstractInsnNode instanceof VarInsnNode && targetMethod.instructions.get(i + 1) instanceof IntInsnNode && targetMethod.instructions.get(i + 2) instanceof MethodInsnNode && targetMethod.instructions.get(i + 3) instanceof JumpInsnNode) {
					VarInsnNode varInsnNode = (VarInsnNode)abstractInsnNode;
					IntInsnNode intInsnNode = (IntInsnNode)targetMethod.instructions.get(i + 1);
					MethodInsnNode methodInsnNode = (MethodInsnNode)targetMethod.instructions.get(i + 2);
					if (varInsnNode.getOpcode() == 25 && intInsnNode.operand == 8 && methodInsnNode.name.equals("nextInt") && methodInsnNode.owner.contains("Random")) {
						targetMethod.instructions.remove(varInsnNode);
						targetMethod.instructions.remove(methodInsnNode);
						break;
					}
				}
			}

			cn.accept(this.cw);
			return this.cw.toByteArray();
		}
	}
}
