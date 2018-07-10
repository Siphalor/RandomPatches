package com.therandomlabs.randompatches.asm.transformer;

import com.therandomlabs.randompatches.asm.Transformer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MinecraftTransformer extends Transformer {
	//Not used in org.lwjgl.input.Keyboard
	public static final int KEY_UNUSED = 0x54;

	public static KeyBinding toggleNarrator;

	@Override
	public void transform(ClassNode node) {
		final MethodNode method = findMethod(node, "dispatchKeypresses", "func_152348_aa");

		IntInsnNode isB = null;

		for(int i = 0; i < method.instructions.size(); i++) {
			final AbstractInsnNode instruction = method.instructions.get(i);

			if(instruction.getOpcode() == Opcodes.BIPUSH) {
				isB = (IntInsnNode) instruction;

				if(isB.operand == Keyboard.KEY_B) {
					break;
				}

				isB = null;
			}
		}

		final MethodInsnNode callHandleKeypress = new MethodInsnNode(
				Opcodes.INVOKESTATIC,
				"com/therandomlabs/randompatches/asm/transformer/MinecraftTransformer",
				"handleKeypress",
				"()V",
				false
		);

		method.instructions.insertBefore(isB.getPrevious(), callHandleKeypress);
		isB.operand = KEY_UNUSED;
	}

	public static void handleKeypress() {
		final int eventKey = Keyboard.getEventKey();
		final int key = eventKey == 0 ? Keyboard.getEventCharacter() + 256 : eventKey;

		if(!toggleNarrator.isActiveAndMatches(key)) {
			return;
		}

		final Minecraft mc = Minecraft.getMinecraft();

		mc.gameSettings.setOptionValue(GameSettings.Options.NARRATOR, 1);

		if(mc.currentScreen instanceof ScreenChatOptions) {
			((ScreenChatOptions) mc.currentScreen).updateNarratorButton();
		}
	}

	public static void registerKeybind() {
		toggleNarrator = new KeyBinding("key.narrator", new IKeyConflictContext() {
			@Override
			public boolean isActive() {
				return !(Minecraft.getMinecraft().currentScreen instanceof GuiControls);
			}

			@Override
			public boolean conflicts(IKeyConflictContext other) {
				return true;
			}
		}, KeyModifier.CONTROL, Keyboard.KEY_B, "key.categories.misc");

		ClientRegistry.registerKeyBinding(toggleNarrator);
	}
}
