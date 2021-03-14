/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package ca.teamdman.sfm.common.flow.data;

import ca.teamdman.sfm.client.gui.flow.core.FlowComponent;
import ca.teamdman.sfm.client.gui.flow.impl.manager.core.ManagerFlowController;
import ca.teamdman.sfm.client.gui.flow.impl.manager.flowdataholder.itemstacktileentityrule.ItemRuleFlowComponent;
import ca.teamdman.sfm.common.cablenetwork.CableNetwork;
import ca.teamdman.sfm.common.flow.core.ItemMatcher;
import ca.teamdman.sfm.common.flow.core.Position;
import ca.teamdman.sfm.common.flow.core.PositionHolder;
import ca.teamdman.sfm.common.flow.core.TileMatcher;
import ca.teamdman.sfm.common.flow.holder.BasicFlowDataContainer;
import ca.teamdman.sfm.common.flow.holder.FlowDataRemovedObserver;
import ca.teamdman.sfm.common.registrar.FlowDataSerializerRegistrar.FlowDataSerializers;
import ca.teamdman.sfm.common.tile.manager.ExecutionState;
import ca.teamdman.sfm.common.util.BlockPosList;
import ca.teamdman.sfm.common.util.EnumSetSerializationHelper;
import ca.teamdman.sfm.common.util.SFMUtil;
import ca.teamdman.sfm.common.util.SlotsRule;
import ca.teamdman.sfm.common.util.UUIDList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class ItemRuleFlowData extends FlowData implements
	Observer, PositionHolder {

	public static final int MAX_NAME_LENGTH = 256;

	private final FlowDataRemovedObserver OBSERVER;
	public FilterMode filterMode;
	public String name;
	public ItemStack icon;
	public Position position;
	public UUIDList itemMatcherIds;
	public BlockPosList tilePositions;
	public UUIDList tileMatcherIds;
	public EnumSet<Direction> faces;
	public SlotsRule slots;
	public boolean open;

	public ItemRuleFlowData() {
		this(
			UUID.randomUUID(),
			I18n.format("gui.sfm.flow.placeholder.default_rule_name"),
			ItemStack.EMPTY,
			new Position(0, 0),
			FilterMode.WHITELIST,
			Collections.emptyList(),
			Collections.emptyList(),
			EnumSet.allOf(Direction.class),
			new SlotsRule(""),
			false,
			Collections.emptyList()
		);
	}

	public ItemRuleFlowData(
		UUID uuid,
		String name,
		ItemStack icon,
		Position position,
		FilterMode filterMode,
		Collection<UUID> itemMatcherIds,
		Collection<BlockPos> tilePositions,
		EnumSet<Direction> faces,
		SlotsRule slots,
		boolean open,
		Collection<UUID> tileMatcherIds
	) {
		super(uuid);
		this.name = name;
		this.icon = icon;
		this.position = position;
		this.filterMode = filterMode;
		this.itemMatcherIds = new UUIDList(itemMatcherIds);
		this.tilePositions = new BlockPosList(tilePositions);
		this.faces = faces;
		this.slots = slots;
		this.open = open;
		this.OBSERVER = new FlowDataRemovedObserver(
			this,
			data -> this.itemMatcherIds.remove(data.getId())
		);
		this.tileMatcherIds = new UUIDList(tileMatcherIds);
	}

	/**
	 * Copy this rule, but with a new ID
	 */
	public ItemRuleFlowData(ItemRuleFlowData other) {
		this(
			UUID.randomUUID(),
			other.name,
			other.icon.copy(),
			other.position.copy(),
			other.filterMode,
			new UUIDList(other.itemMatcherIds),
			new BlockPosList(other.tilePositions),
			EnumSet.copyOf(other.faces),
			other.slots.copy(),
			other.open,
			other.tileMatcherIds
		);
	}

	/**
	 * @return maximum amount allowed through according to this rule
	 */
	public Optional<ItemMatcher> getBestItemMatcher(
		BasicFlowDataContainer container,
		ItemStack stack,
		ExecutionState state
	) {
		return itemMatcherIds.stream()
			.map(id -> container.get(id, ItemMatcher.class))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.filter(m -> m.matches(stack))
			.max(Comparator
				.comparingInt(m -> state.getRemainingQuantity(this, m))); // Most remaining first
	}

	public List<IItemHandler> getItemHandlers(World world, CableNetwork network) {
		return tilePositions.stream()
			.map(network::getInventory)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.flatMap(tile -> faces.stream()
				.map(face -> tile.getCapability(
					CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
					face
				)))
			.filter(LazyOptional::isPresent)
			.map(LazyOptional::resolve)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}

	public ItemStack getIcon() {
		return icon;
	}

	@Override
	public Position getPosition() {
		return position;
	}

	@Override
	public void addToDataContainer(BasicFlowDataContainer container) {
		super.addToDataContainer(container);
		container.addObserver(this);
	}

	@Override
	public ItemRuleFlowData duplicate(
		BasicFlowDataContainer container, Consumer<FlowData> dependencyTracker
	) {
		ItemRuleFlowData newRule = new ItemRuleFlowData(this);
		dependencyTracker.accept(newRule);

		newRule.itemMatcherIds.clear();
		itemMatcherIds.stream()
			.map(container::get)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.filter(ItemMatcher.class::isInstance)
			.map(data -> data.duplicate(container, dependencyTracker)) // copy matcher data
			.peek(dependencyTracker) // add new matcher to dependency tracker
			.map(FlowData::getId)
			.forEach(newRule.itemMatcherIds::add); // add matcher to rule id list

		newRule.tileMatcherIds.clear();
		tileMatcherIds.stream()
			.map(container::get)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.filter(TileMatcher.class::isInstance)
			.map(data -> data.duplicate(container, dependencyTracker))
			.peek(dependencyTracker)
			.map(FlowData::getId)
			.forEach(newRule.tileMatcherIds::add);

		return newRule;
	}

	@Override
	public FlowComponent createController(
		FlowComponent parent
	) {
		if (!(parent instanceof ManagerFlowController)) {
			return null;
		}
		return new ItemRuleFlowComponent((ManagerFlowController) parent, this);
	}

	@Override
	public Set<Class<? extends FlowData>> getDependencies() {
		return ImmutableSet.of(ItemPickerMatcherFlowData.class);
	}

	@Override
	public FlowDataSerializer getSerializer() {
		return FlowDataSerializers.TILE_ENTITY_RULE;
	}

	@Override
	public void update(Observable o, Object arg) {
		OBSERVER.update(o, arg);
	}

	public enum FilterMode {
		WHITELIST,
		BLACKLIST
	}

	public static class Serializer extends
		FlowDataSerializer<ItemRuleFlowData> {

		public Serializer(ResourceLocation registryName) {
			super(registryName);
		}

		@Override
		public ItemRuleFlowData fromNBT(CompoundNBT tag) {
			return new ItemRuleFlowData(
				UUID.fromString(tag.getString("uuid")),
				tag.getString("name"),
				ItemStack.read(tag.getCompound("icon")),
				new Position(tag.getCompound("pos")),
				FilterMode.valueOf(tag.getString("filterMode")),
				new UUIDList(tag, "itemMatchers"),
				new BlockPosList(tag, "tiles"),
				EnumSetSerializationHelper.deserialize(tag, "faces", Direction::valueOf),
				new SlotsRule(tag.getString("slots")),
				tag.getBoolean("open"),
				new UUIDList(tag, "tileMatchers")
			);
		}

		@Override
		public CompoundNBT toNBT(ItemRuleFlowData data) {
			CompoundNBT tag = super.toNBT(data);
			tag.put("pos", data.position.serializeNBT());
			tag.putString("name", data.name);
			tag.put("icon", data.icon.serializeNBT());
			tag.putString("filterMode", data.filterMode.name());
			tag.put("itemMatchers", data.itemMatcherIds.serialize());
			tag.put("tiles", data.tilePositions.serialize());
			tag.put("faces", EnumSetSerializationHelper.serialize(data.faces));
			tag.putString("slots", data.slots.getDefinition());
			tag.putBoolean("open", data.open);
			tag.put("tileMatchers", data.tileMatcherIds.serialize());
			return tag;
		}

		@Override
		public ItemRuleFlowData fromBuffer(PacketBuffer buf) {
			return new ItemRuleFlowData(
				SFMUtil.readUUID(buf),
				buf.readString(MAX_NAME_LENGTH),
				buf.readItemStack(),
				Position.fromLong(buf.readLong()),
				FilterMode.valueOf(buf.readString(16)),
				new UUIDList(buf),
				new BlockPosList(buf),
				EnumSetSerializationHelper.deserialize(buf, Direction::valueOf),
				new SlotsRule(buf.readString(32)),
				buf.readBoolean(),
				new UUIDList(buf)
			);
		}

		@Override
		public void toBuffer(ItemRuleFlowData data, PacketBuffer buf) {
			SFMUtil.writeUUID(data.getId(), buf);
			buf.writeString(data.name, MAX_NAME_LENGTH);
			buf.writeItemStack(data.icon);
			buf.writeLong(data.position.toLong());
			buf.writeString(data.filterMode.name(), 16);
			data.itemMatcherIds.serialize(buf);
			data.tilePositions.serialize(buf);
			EnumSetSerializationHelper.serialize(data.faces, buf);
			buf.writeString(data.slots.getDefinition(), 32);
			buf.writeBoolean(data.open);
			data.tileMatcherIds.serialize(buf);
		}
	}
}
