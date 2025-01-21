package at.hannibal2.skyhanni.neu

import io.github.moulberry.notenoughupdates.NEUApi
import io.github.moulberry.notenoughupdates.NEUOverlay
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.miscfeatures.PetInfoOverlay
import io.github.moulberry.notenoughupdates.overlays.AuctionSearchOverlay
import io.github.moulberry.notenoughupdates.overlays.BazaarSearchOverlay
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer
import io.github.moulberry.notenoughupdates.util.Calculator
import java.math.BigDecimal

object NEUCompat {

    var isNeuLoaded: Boolean = false
        private set

    fun setNeuLoaded() {
        isNeuLoaded = true
    }

    fun isCustomStorageEnabled(): Boolean = isNeuLoaded && NotEnoughUpdates.INSTANCE.config.storageGUI.enableStorageGUI3

    fun disableInventoryButtons() {
        if (isNeuLoaded) NEUApi.setInventoryButtonsToDisabled()
    }

    fun calculateOrNull(input: String): BigDecimal? {
        if (!isNeuLoaded) return null
        return runCatching { Calculator.calculate(input) }.getOrNull()
    }

    fun neuHasFocus(inStorage: () -> Boolean): Boolean {
        if (!isNeuLoaded) return false
        if (AuctionSearchOverlay.shouldReplace()) return true
        if (BazaarSearchOverlay.shouldReplace()) return true
        // TODO add RecipeSearchOverlay via RecalculatingValue and reflection
        // TODO 2 fix this stupid fucking shit oh my god i hate this
        // https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/overlays/RecipeSearchOverlay.java
        if (inStorage() && isCustomStorageEnabled()) return true
        if (NEUOverlay.searchBarHasFocus) return true

        return false
    }

    fun getLowestBin(internalName: String): Long = NotEnoughUpdates.INSTANCE.manager.auctionManager.getLowestBin(internalName)

    fun isNeuExtendedExpEnabled(): Boolean = isNeuLoaded && NotEnoughUpdates.INSTANCE.config.tooltipTweaks.petExtendExp

    fun isProfileViewer(screen: Any?): Boolean = isNeuLoaded && screen is GuiProfileViewer

    fun getCurrentPetLevel(): String {
        if (!isNeuLoaded) return "?"
        return PetInfoOverlay.getCurrentPet().petLevel?.currentLevel?.toString() ?: "?"
    }

}
