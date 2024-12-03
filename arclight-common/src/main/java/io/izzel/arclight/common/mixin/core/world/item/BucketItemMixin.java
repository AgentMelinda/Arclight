package io.izzel.arclight.common.mixin.core.world.item;

import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.item.BucketItemBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.DummyGeneratorAccess;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin implements BucketItemBridge {

    // @formatter:off
    @Shadow public abstract boolean emptyContents(@Nullable Player player, Level worldIn, BlockPos posIn, @javax.annotation.Nullable BlockHitResult rayTrace);
    // @formatter:on

    @Inject(method = "use", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private void arclight$bucketFill(Level worldIn, Player playerIn, InteractionHand handIn, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir, @Local ItemStack stack, @Local BlockHitResult result) {
        if (!DistValidate.isValid(worldIn)) return;
        BlockPos pos = result.getBlockPos();
        BlockState state = worldIn.getBlockState(pos);
        ItemStack dummyFluid = ((BucketPickup) state.getBlock()).pickupBlock(playerIn, DummyGeneratorAccess.INSTANCE, pos, state);
        PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent((ServerLevel) worldIn, playerIn, pos, pos, result.getDirection(), stack, dummyFluid.getItem(), handIn);
        if (event.isCancelled()) {
            ((ServerPlayer) playerIn).connection.send(new ClientboundBlockUpdatePacket(worldIn, pos));
            ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().updateInventory();
            cir.setReturnValue(new InteractionResultHolder<>(InteractionResult.FAIL, stack));
        } else {
            arclight$setCaptureItem(event.getItemStack());
        }
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void arclight$clean(Level worldIn, Player playerIn, InteractionHand handIn, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        arclight$setDirection(null);
        arclight$setClick(null);
        arclight$setHand(null);
    }

    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$useEventItem(ItemStack itemStack) {
        return arclight$getCaptureItem() == null ? itemStack : CraftItemStack.asNMSCopy(arclight$getCaptureItem());
    }

    public boolean emptyContents(Player entity, Level world, BlockPos pos, @Nullable BlockHitResult result, Direction direction, BlockPos clicked, ItemStack itemstack, InteractionHand hand) {
        arclight$setDirection(direction);
        arclight$setClick(clicked);
        arclight$setHand(hand);
        arclight$setStack(itemstack);
        try {
            return this.emptyContents(entity, world, pos, result);
        } finally {
            arclight$setDirection(null);
            arclight$setClick(null);
            arclight$setHand(null);
            arclight$setStack(null);
        }
    }

    @Unique
    @Nullable
    private transient Direction arclight$direction;

    @Unique
    @Nullable
    private transient BlockPos arclight$click;

    @Unique
    @Nullable
    private transient InteractionHand arclight$hand;

    @Unique
    @Nullable
    private transient ItemStack arclight$stack;

    @Unique
    @Nullable
    private transient org.bukkit.inventory.ItemStack arclight$captureItem;

    @Nullable
    @Override
    public Direction arclight$getDirection() {
        return this.arclight$direction;
    }

    @Override
    public void arclight$setDirection(@Nullable Direction value) {
        this.arclight$direction = value;
    }

    @Nullable
    @Override
    public BlockPos arclight$getClick() {
        return this.arclight$click;
    }

    @Override
    public void arclight$setClick(@Nullable BlockPos value) {
        this.arclight$click = value;
    }

    @Nullable
    @Override
    public InteractionHand arclight$getHand() {
        return this.arclight$hand;
    }

    @Override
    public void arclight$setHand(@Nullable InteractionHand value) {
        this.arclight$hand = value;
    }

    @Nullable
    @Override
    public ItemStack arclight$getStack() {
        return this.arclight$stack;
    }

    @Override
    public void arclight$setStack(@Nullable ItemStack value) {
        this.arclight$stack = value;
    }

    @Nullable
    @Override
    public org.bukkit.inventory.ItemStack arclight$getCaptureItem() {
        return this.arclight$captureItem;
    }

    @Override
    public void arclight$setCaptureItem(@Nullable org.bukkit.inventory.ItemStack value) {
        this.arclight$captureItem = value;
    }
}
