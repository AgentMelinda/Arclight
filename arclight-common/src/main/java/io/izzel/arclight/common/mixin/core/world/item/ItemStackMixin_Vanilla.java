package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
@OnlyInPlatform(value = {ArclightPlatform.VANILLA, ArclightPlatform.FABRIC})
public abstract class ItemStackMixin_Vanilla implements ItemStackBridge {

    // @formatter:off
    @Shadow private int count;
    // @formatter:on

    @Decorate(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int arclight$itemDamage(ServerLevel serverLevel, ItemStack itemStack, int i, @Local(ordinal = 0) ServerPlayer damager) throws Throwable {
        int result = (int) DecorationOps.callsite().invoke(serverLevel, itemStack, i);
        if (damager != null) {
            PlayerItemDamageEvent event = new PlayerItemDamageEvent(((ServerPlayerEntityBridge) damager).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror((ItemStack) (Object) this), result);
            event.getPlayer().getServer().getPluginManager().callEvent(event);

            if (result != event.getDamage() || event.isCancelled()) {
                event.getPlayer().updateInventory();
            }
            if (event.isCancelled()) {
                return (int) DecorationOps.cancel().invoke();
            }
            result = event.getDamage();
        }
        return result;
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void arclight$itemBreak(int amount, ServerLevel level, @Nullable ServerPlayer serverPlayer, Consumer<Item> onBroken, CallbackInfo ci) {
        if (this.count == 1 && serverPlayer != null) {
            CraftEventFactory.callPlayerItemBreakEvent(serverPlayer, (ItemStack) (Object) this);
        }
    }
}
