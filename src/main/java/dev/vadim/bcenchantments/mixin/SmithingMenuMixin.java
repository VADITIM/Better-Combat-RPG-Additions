package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.progression.Refinement;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Weapon refinement on the smithing table: a weapon in the base slot takes gems, an echo shard with a
 * trim template, or a trim template alone, and a shield takes a trim template with an echo shard, as
 * described in {@link Refinement}. A weapon's powers cost experience levels, shown by
 * {@code SmithingScreenMixin}. A weapon or shield taken out upgraded goes straight back in the middle slot.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenu {
	private static final String WITH_SLOT = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withSlot(IIILjava/util/function/Predicate;)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;";

	@Shadow
	@Final
	private Level level;

	private SmithingMenuMixin() {
		super(null, 0, null, null, null);
	}

	@ModifyArg(method = "createInputSlotDefinitions", at = @At(value = "INVOKE", target = WITH_SLOT, ordinal = 1), index = 3)
	private static Predicate<ItemStack> bcenchantments$acceptWeapons(Predicate<ItemStack> vanilla) {
		return vanilla.or(Refinement::isBase);
	}

	@ModifyArg(method = "createInputSlotDefinitions", at = @At(value = "INVOKE", target = WITH_SLOT, ordinal = 2), index = 3)
	private static Predicate<ItemStack> bcenchantments$acceptGems(Predicate<ItemStack> vanilla) {
		return vanilla.or(Refinement::isAddition);
	}

	@Inject(method = "canMoveIntoInputSlots", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$shiftClick(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (Refinement.isBase(stack) && !this.getSlot(1).hasItem() || Refinement.isAddition(stack) && !this.getSlot(2).hasItem()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$refine(CallbackInfo ci) {
		if (!(this.level instanceof ServerLevel)) {
			return;
		}
		Refinement.Plan plan = this.bcenchantments$plan();
		if (plan != null) {
			this.resultSlots.setRecipeUsed(null);
			this.resultSlots.setItem(0, plan.result());
			ci.cancel();
		}
	}

	/** A new power on a weapon that has one already costs experience levels, which the player must have. */
	@Override
	protected boolean mayPickup(Player player, boolean hasStack) {
		Refinement.Plan plan = this.bcenchantments$plan();
		if (plan != null && plan.levelCost() > 0 && !player.hasInfiniteMaterials() && player.experienceLevel < plan.levelCost()) {
			return false;
		}
		return super.mayPickup(player, hasStack);
	}

	@Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$take(Player player, ItemStack carried, CallbackInfo ci) {
		// Planned again from the inputs, which both sides have, so the client takes the same items the server does.
		Refinement.Plan plan = this.bcenchantments$plan();
		if (plan == null) {
			return;
		}
		ci.cancel();
		carried.onCraftedBy(player, carried.getCount());
		if (plan.levelCost() > 0 && !player.hasInfiniteMaterials()) {
			player.giveExperienceLevels(-plan.levelCost());
		}
		if (plan.levelUp() && !player.level().isClientSide()) {
			// A new seed for the next level's rolls, so they cannot be read off by putting the weapon back.
			player.onEnchantmentPerformed(ItemStack.EMPTY, 0);
		}
		this.bcenchantments$shrink(2, plan.additionItems());
		if (plan.consumesTemplate()) {
			this.bcenchantments$shrink(0, 1);
		}
		this.bcenchantments$shrink(1, 1);
		this.access.execute((level, pos) -> {
			level.levelEvent(1044, pos, 0);
			if (plan.levelUp()) {
				level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.3F);
			}
		});
		this.bcenchantments$returnTool(carried);
	}

	@Inject(method = "onTake", at = @At("TAIL"))
	private void bcenchantments$takeVanilla(Player player, ItemStack carried, CallbackInfo ci) {
		// A vanilla upgrade (netherite) of a weapon goes back on the table too.
		this.bcenchantments$returnTool(carried);
	}

	/** Shift-clicking an upgraded weapon out of the result puts it back in the middle slot, like a plain click. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index == this.getResultSlot()) {
			Slot slot = this.slots.get(index);
			if (slot.hasItem() && Refinement.isBase(slot.getItem()) && slot.mayPickup(player)) {
				ItemStack taken = slot.remove(slot.getItem().getCount());
				slot.onTake(player, taken);
				if (!taken.isEmpty() && !player.getInventory().add(taken)) {
					player.drop(taken, false);
				}
				return ItemStack.EMPTY;
			}
		}
		return super.quickMoveStack(player, index);
	}

	/**
	 * Puts an upgraded weapon or shield straight back in the emptied middle slot, so the next step can follow;
	 * {@code taken} is then off the cursor, or emptied when it was shift-clicked.
	 */
	private void bcenchantments$returnTool(ItemStack taken) {
		if (taken.isEmpty() || !Refinement.isBase(taken) || !this.inputSlots.getItem(1).isEmpty()) {
			return;
		}
		ItemStack tool = taken.copy();
		if (this.getCarried() == taken) {
			this.setCarried(ItemStack.EMPTY);
		}
		taken.setCount(0);
		this.inputSlots.setItem(1, tool);
	}

	private Refinement.Plan bcenchantments$plan() {
		return Refinement.plan(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2), this.player.getEnchantmentSeed());
	}

	private void bcenchantments$shrink(int slot, int count) {
		ItemStack stack = this.inputSlots.getItem(slot);
		if (count > 0 && !stack.isEmpty()) {
			stack.shrink(count);
			this.inputSlots.setItem(slot, stack);
		}
	}
}
