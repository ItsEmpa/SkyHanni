package at.hannibal2.skyhanni.compat

import at.hannibal2.skyhanni.compat.NEUCompat.isNeuLoaded
import io.github.moulberry.notenoughupdates.NEUApi
import io.github.moulberry.notenoughupdates.NEUOverlay
import io.github.moulberry.notenoughupdates.overlays.AuctionSearchOverlay
import io.github.moulberry.notenoughupdates.overlays.BazaarSearchOverlay
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer

object NEUScreens {

    fun isCustomStorageEnabled(): Boolean = isNeuLoaded && config.storageGUI.enableStorageGUI3

    fun disableInventoryButtons() {
        if (isNeuLoaded) NEUApi.setInventoryButtonsToDisabled()
    }

    // Haven't found a way to have for this to have the main sourceset as
    // a dependency without creating a cyclical dependency
    fun neuHasFocus(inStorage: () -> Boolean): Boolean {
        if (!isNeuLoaded) return false
        if (AuctionSearchOverlay.shouldReplace()) return true
        if (BazaarSearchOverlay.shouldReplace()) return true
        // TODO: store used NEU version, and use that to determine if we should check for RecipeSearchOverlay
        // https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/overlays/RecipeSearchOverlay.java
        if (inStorage() && isCustomStorageEnabled()) return true
        if (NEUOverlay.searchBarHasFocus) return true

        return false
    }

    fun isProfileViewer(screen: Any?): Boolean = isNeuLoaded && screen is GuiProfileViewer

}
