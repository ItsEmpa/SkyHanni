package at.hannibal2.skyhanni.compat

import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.miscfeatures.PetInfoOverlay
import io.github.moulberry.notenoughupdates.options.NEUConfig
import io.github.moulberry.notenoughupdates.util.Calculator
import java.math.BigDecimal

internal inline val NEU: NotEnoughUpdates get() = NotEnoughUpdates.INSTANCE

internal inline val config: NEUConfig get() = NEU.config

object NEUCompat {

    var isNeuLoaded: Boolean = false
        private set

    fun setNeuLoaded() {
        isNeuLoaded = true
    }

    fun calculateOrNull(input: String): BigDecimal? {
        if (!isNeuLoaded) return null
        return runCatching { Calculator.calculate(input) }.getOrNull()
    }

    fun getLowestBin(internalName: String): Long = NEU.manager.auctionManager.getLowestBin(internalName)

    fun isNeuExtendedExpEnabled(): Boolean = isNeuLoaded && config.tooltipTweaks.petExtendExp

    fun getCurrentPetLevel(): Int? {
        if (!isNeuLoaded) return null
        return PetInfoOverlay.getCurrentPet().petLevel?.currentLevel
    }

}
