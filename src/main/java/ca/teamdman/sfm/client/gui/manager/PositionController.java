package ca.teamdman.sfm.client.gui.manager;


import ca.teamdman.sfm.client.gui.ManagerScreen;

import static ca.teamdman.sfm.client.gui.ManagerScreen.*;
import static ca.teamdman.sfm.client.gui.manager.PositionController.DragMode.*;

public class PositionController {
	private final ManagerScreen GUI;
	private       int           dragOffsetX = 0;
	private       int           dragOffsetY = 0;
	private       Component     dragging    = null;
	private       DragMode      mode        = NONE;

	public PositionController(ManagerScreen gui) {
		this.GUI = gui;
	}

	// Return false to pass through
	public boolean onMouseDown(int x, int y, int button, Component comp) {
		if (button != LEFT)
			return false;
		if (comp == null)
			return false;
		if (hasAltDown()) {
			mode = MOVE;
			dragging = comp;
			dragOffsetX = comp.getPosition().getX() - x;
			dragOffsetY = comp.getPosition().getY() - y;
			return true;
		} else if (hasControlDown()) {
			comp.copy(GUI).ifPresent(c -> {
				dragging = c;
				mode = COPY;
			});
			dragOffsetX = comp.getPosition().getX() - x;
			dragOffsetY = comp.getPosition().getY() - y;
			return true;
		} else {
			mode = NONE;
			return false;
		}
	}

	public boolean onDrag(int x, int y, int button) {
		if (dragging == null)
			return false;

		if (hasShiftDown()) {
			x -= (x + dragOffsetX) % 10;
			y -= (y + dragOffsetY) % 10;
		}

		dragging.getPosition().setXY(x + dragOffsetX, y + dragOffsetY);
		GUI.RELATIONSHIP_CONTROLLER.postComponentReposition(dragging);
		return true;
	}

	public boolean onMouseUp(int x, int y, int button) {
		if (dragging != null) {
			dragging = null;
			return true;
		} else {
			return false;
		}
	}

	protected enum DragMode {
		NONE,
		MOVE,
		COPY
	}

}
