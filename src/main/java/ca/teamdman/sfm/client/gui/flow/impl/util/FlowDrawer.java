/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ca.teamdman.sfm.client.gui.flow.impl.util;

import ca.teamdman.sfm.client.gui.flow.core.BaseScreen;
import ca.teamdman.sfm.client.gui.flow.core.Colour3f.CONST;
import ca.teamdman.sfm.client.gui.flow.core.FlowComponent;
import ca.teamdman.sfm.common.flow.core.Position;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.math.MathHelper;

public class FlowDrawer extends FlowContainer {

	private static final int PADDING_X = 4;
	private static final int PADDING_Y = 4;
	private static final int ITEM_MARGIN_X = 4;
	private static final int ITEM_MARGIN_Y = 4;
	private int maxItemsPerRow;
	private int maxItemsPerColumn;
	private int maxItemWidth;
	private int maxItemHeight;
	private boolean shrinkToFit = true;
	private int scroll = 0;
	public FlowDrawer(Position pos, int maxItemsPerRow, int maxItemsPerColumn) {
		super(pos);
		this.maxItemsPerRow = maxItemsPerRow;
		this.maxItemsPerColumn = maxItemsPerColumn;
	}

	public int getMaxWidth() {
		return (maxItemWidth + ITEM_MARGIN_X) * getMaxItemsPerRow() + PADDING_X;
	}

	public int getMaxItemsPerRow() {
		return maxItemsPerRow;
	}

	public void setMaxItemsPerRow(int maxItemsPerRow) {
		this.maxItemsPerRow = maxItemsPerRow;
	}

	public void setShrinkToFit(boolean value) {
		this.shrinkToFit = value;
	}

	public void update() {
		maxItemWidth = -getChildren().stream() // negate result
			.mapToInt(c -> -c.getSize().getWidth()) // negative so that the 'largest' is first
			.sorted()
			.findFirst()
			.orElse(-32);
		maxItemHeight = -getChildren().stream()
			.mapToInt(c -> -c.getSize().getHeight())
			.sorted()
			.findFirst()
			.orElse(-32);

		fixScroll();
		getSize().setSize(
			shrinkToFit
				? (maxItemWidth + ITEM_MARGIN_X) * getItemsPerRow() + PADDING_X
				: getMaxWidth(),
			shrinkToFit
				? (maxItemHeight + ITEM_MARGIN_Y) * getItemsPerColumn() + PADDING_Y
				: getMaxHeight()
		);
		AtomicInteger i = new AtomicInteger();
		getChildren().forEach(c -> c.getPosition().setXY(
			getWrappedX(i.get()),
			getWrappedY(i.getAndIncrement()) - scroll
		));
	}

	public void fixScroll() {
		this.scroll = MathHelper.clamp(
			this.scroll,
			0,
			Math.max(
				0,
				(
					(int) Math.ceil(getChildren().size() / (float) getItemsPerRow())
						- getItemsPerColumn()
				) * (maxItemHeight + ITEM_MARGIN_Y)
			)
		);
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

		screen.drawRect(
			matrixStack,
			getPosition().getX(),
			getPosition().getY(),
			getSize().getWidth(),
			getSize().getHeight(),
			CONST.PANEL_BACKGROUND_NORMAL
		);

		// Enable clipping for drawer contents
		// +2 -4 for border padding
		screen.beginScissor(
			matrixStack,
			getPosition().getX() + 2,
			getPosition().getY() + 2,
			getSize().getWidth() - 4,
			getSize().getHeight() - 4
		);

		matrixStack.push();
		matrixStack.translate(getPosition().getX(), getPosition().getY(), 0);
		for (FlowComponent comp : getChildren()) {
			if (comp.isVisible() && isChildVisible(comp)) {
				comp.draw(
					screen,
					matrixStack,
					mx - getPosition().getX(),
					my - getPosition().getY(),
					deltaTime
				);
			}
		}
		matrixStack.pop();

		screen.endScissor();

		screen.drawBorder(
			matrixStack,
			getPosition().getX(),
			getPosition().getY(),
			getSize().getWidth(),
			getSize().getHeight(),
			2,
			CONST.PANEL_BORDER
		);

		drawTooltip(screen, matrixStack, mx, my, deltaTime);
	}

	@Override
	public boolean mouseScrolled(int mx, int my, double scroll) {
		if (isInBounds(mx, my)) {
			if (scroll > 0) {
				scrollUp();
			} else {
				scrollDown();
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean isChildVisible(FlowComponent comp) {
		return comp.getPosition().getY() > -comp.getSize().getHeight() // not above bounds
			&& comp.getPosition().getY() < getMaxHeight(); // not below bounds
	}

	public int getMaxHeight() {
		return (maxItemHeight + ITEM_MARGIN_Y) * getMaxItemsPerColumn() + PADDING_Y;
	}

	public int getMaxItemsPerColumn() {
		return maxItemsPerColumn;
	}

	public void setMaxItemsPerColumn(int maxItemsPerColumn) {
		this.maxItemsPerColumn = maxItemsPerColumn;
	}

	public void scrollDown() {
		scroll += 7;
		update();
	}

	public void scrollUp() {
		scroll -= 7;
		update();
	}

	public int getItemsPerColumn() {
		return MathHelper.clamp(
			getItemRow(getChildren().size() - 1) + 1,
			1,
			getMaxItemsPerColumn()
		);
	}

	public int getItemsPerRow() {
		return MathHelper.clamp(getChildren().size(), 1, getMaxItemsPerRow());
	}

	public int getWrappedX(int index) {
		return getItemColumn(index)
			* (maxItemWidth + ITEM_MARGIN_X)
			+ ITEM_MARGIN_X / 2
			+ PADDING_X / 2;
	}

	public int getItemColumn(int index) {
		return index % getItemsPerRow();
	}

	public int getItemRow(int index) {
		return (int) Math.floor(index / (float) getItemsPerRow());
	}

	public int getWrappedY(int index) {
		return getItemRow(index)
			* (maxItemHeight + ITEM_MARGIN_Y)
			+ ITEM_MARGIN_Y / 2
			+ PADDING_Y / 2;
	}
}
