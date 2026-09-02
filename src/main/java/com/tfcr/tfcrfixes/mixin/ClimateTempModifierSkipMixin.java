package com.tfcr.tfcrfixes.mixin;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * tfc_coldsweat's ClimateTempModifier adds TFC's instant climate temperature on
 * top of the ColdSweat world temperature for EVERY dimension. Cosmonautics
 * dimensions (rocketnautics:moon, rocketnautics:deep_space) are not TFC climate
 * zones, so Climate.getInstantTemperature returns garbage there (players observed
 * -1000 C on the moon).
 *
 * Additionally, Cold Sweat's own per-dimension override
 * (coldsweat/world.toml "Dimension Temperatures") resolves dimension IDs against
 * datapack registries that do not exist yet when the config is parsed, so the
 * configured moon / deep-space temperatures never actually apply and players see
 * a constant 0 C (the raw biome base temperature).
 *
 * This mixin therefore forces a constant world temperature for the cosmonautics
 * dimensions, bypassing both the broken config lookup and the TFC climate:
 *   rocketnautics:moon       -> -50 C
 *   rocketnautics:deep_space -> -273 C
 * Any other rocketnautics dimension keeps the previous no-op (identity) behavior.
 */
@Mixin(targets = "net.atobaazul.tfc_coldsweat.temperature.modifier.ClimateTempModifier", remap = false)
public abstract class ClimateTempModifierSkipMixin {
    // Cold Sweat MC units = Celsius / 25 (see Temperature.convert C <-> MC)
    private static final double MOON_TEMP_MC = Temperature.convert(-50.0, Temperature.Units.C, Temperature.Units.MC, true);
    private static final double DEEP_SPACE_TEMP_MC = Temperature.convert(-273.0, Temperature.Units.C, Temperature.Units.MC, true);

    @Inject(method = "calculate", at = @At("HEAD"), cancellable = true)
    private void tfcr_fixes$forceCosmonauticsTemperature(LivingEntity entity,
                                                         Temperature.Trait trait,
                                                         CallbackInfoReturnable<Function<Double, Double>> cir) {
        if (!"rocketnautics".equals(entity.level().dimension().location().getNamespace())) {
            return;
        }
        double constant = switch (entity.level().dimension().location().getPath()) {
            case "moon" -> MOON_TEMP_MC;
            case "deep_space" -> DEEP_SPACE_TEMP_MC;
            default -> Double.NaN;
        };
        if (Double.isNaN(constant)) {
            cir.setReturnValue(Function.identity());
        } else {
            cir.setReturnValue(d -> constant);
        }
    }
}
