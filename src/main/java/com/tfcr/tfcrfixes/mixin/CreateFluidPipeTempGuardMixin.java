package com.tfcr.tfcrfixes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cold Sweat 2.4.2's CreateFluidPipeTemp blindly casts the pipe block entity to
 * Create's FluidPipeBlockEntity. Create 6.0.10 uses StraightPipeBlockEntity for
 * straight pipe segments, so the cast throws a ClassCastException during
 * WorldHelper.getTemperatureAt (called every player tick by tfc_coldsweat's
 * thirst-temperature hook), which kills the player tick and kicks the player
 * from the server with "Internal server error".
 *
 * Guard: only let the original method run when the block entity really is a
 * FluidPipeBlockEntity; straight pipes simply radiate no heat.
 */
@Mixin(targets = "com.momosoftworks.coldsweat.api.temperature.block_temp.compat.CreateFluidPipeTemp", remap = false)
public abstract class CreateFluidPipeTempGuardMixin {
    private static final String FLUID_PIPE_BE = "com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity";

    @Inject(method = "getTemperature", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfcr_fixes$guardNonFluidPipe(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance, CallbackInfoReturnable<Double> cir) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !FLUID_PIPE_BE.equals(be.getClass().getName())) {
            cir.setReturnValue(0.0D);
        }
    }
}
