package ca.teamdman.sfm.client.gui.manager;

import ca.teamdman.sfm.client.gui.core.BaseContainerScreen;
import ca.teamdman.sfm.common.container.ManagerContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class ManagerScreen extends BaseContainerScreen<ManagerContainer> {
	public ManagerScreen(ManagerContainer container, PlayerInventory inv, ITextComponent name) {
		super(container, 512, 256, inv, name);
	}
}
