/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ca.teamdman.sfm.client.gui.flow.impl.util;

import ca.teamdman.sfm.client.gui.flow.core.BaseScreen;
import ca.teamdman.sfm.client.gui.flow.core.FlowComponent;
import ca.teamdman.sfm.client.gui.flow.core.Size;
import ca.teamdman.sfm.common.flow.core.Position;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.glfw.GLFW;

/**
 * @see net.minecraft.client.gui.widget.TextFieldWidget
 * @see net.minecraft.client.gui.fonts.TextInputUtil
 * @see net.minecraft.util.text.TextFormatting
 */
public class TextAreaFlowComponent extends FlowComponent {

	protected final TextFieldWidget delegate;
	private boolean debounce = false;
	private String content;

	public TextAreaFlowComponent(
		BaseScreen screen,
		String content,
		String placeholder,
		Position pos,
		Size size
	) {
		super(new WidgetPositionDelegate(pos), new WidgetSizeDelegate(size));
		this.content = content;
		this.delegate = new PatchedTextFieldWidget(
			screen.getFontRenderer(),
			pos.getX(),
			pos.getY(),
			size.getWidth(),
			size.getHeight(),
			content,
			placeholder
		);
		((WidgetPositionDelegate) getPosition()).setWidget(delegate);
		((WidgetSizeDelegate) getSize()).setWidget(delegate);
	}

	public void setContent(String content) {
		debounce = true;
		delegate.setText(content);
		debounce = false;
	}

	public String getContent() {
		return delegate.getText();
	}

	public void setValidator(Predicate<String> validator) {
		delegate.setValidator(validator);
	}

	public void setResponder(Consumer<String> responder) {
		delegate.setResponder(s -> {
			if (!debounce) {
				responder.accept(s);
			}
		});
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers, int mx, int my) {
		delegate.keyPressed(keyCode, scanCode, modifiers);
		return delegate.canWrite();
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers, int mx, int my) {
		delegate.keyReleased(keyCode, scanCode, modifiers);
		return delegate.canWrite();
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers, int mx, int my) {
		delegate.charTyped(codePoint, modifiers);
		return delegate.canWrite();
	}

	@Override
	public void tick() {
		delegate.tick();
	}

	@Override
	public boolean mousePressed(int mx, int my, int button) {
		if (isInBounds(mx, my) && button == GLFW.GLFW_MOUSE_BUTTON_2) {
			clear();
			return true;
		} else {
			return delegate.mouseClicked(mx, my, button);
		}
	}

	public void clear() {
		delegate.setText("");
	}

	@Override
	public boolean mouseDragged(int mx, int my, int button, int dmx, int dmy) {
		return false; // Ignore default widget drag
//		return delegate.mouseDragged(mx, my, button, dmx, dmy);
	}

	@Override
	public boolean mouseReleased(int mx, int my, int button) {
		return false; // Text widget doesn't do anything on release, but it tries to consume :/
//		return delegate.mouseReleased(mx, my, button);
	}

	@Override
	public void draw(
		BaseScreen screen, MatrixStack matrixStack, int mx, int my, float deltaTime
	) {
		screen.clearRect(
			matrixStack,
			getPosition().getX(),
			getPosition().getY(),
			getSize().getWidth(),
			getSize().getHeight()
		);
		delegate.render(matrixStack, mx, my, deltaTime);
	}

	private static class PatchedTextFieldWidget extends TextFieldWidget {

		protected final FontRenderer FONT;
		private final String PLACEHOLDER_TEXT;
		private MatrixStack matrixStack;

		public PatchedTextFieldWidget(
			FontRenderer font,
			int x,
			int y,
			int width,
			int height,
			String content,
			String placeholderContent
		) {
			super(font, x, y, width, height, null);
			setText(content);
			this.FONT = font;
			this.PLACEHOLDER_TEXT = placeholderContent;
		}

		@Override
		protected void drawSelectionBox(int startX, int startY, int endX, int endY) {
			RenderSystem.pushMatrix();
			RenderSystem.multMatrix(matrixStack.getLast().getMatrix());
			super.drawSelectionBox(startX, startY, endX, endY);
			RenderSystem.popMatrix();
		}

		@Override
		public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
			this.matrixStack = matrixStack;
			super.render(matrixStack, mouseX, mouseY, partialTicks);
			if (!isFocused() && getText().length() == 0) {
				FONT.drawStringWithShadow(
					matrixStack,
					PLACEHOLDER_TEXT,
					this.x + 4,
					this.y + (this.height - 8) / 2f,
					7368816
				);
			}
		}
	}
}
