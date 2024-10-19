package at.hannibal2.skyhanni.api.hypixel

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelAPIServerChangeEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAll
import at.hannibal2.skyhanni.utils.LorenzLogger
import net.hypixel.data.type.GameType
import net.hypixel.data.type.ServerType
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@Suppress("MemberVisibilityCanBePrivate", "UNUSED_VARIABLE", "unused")
@SkyHanniModule
object HypixelLocationAPI {

    var inHypixel: Boolean = false
        private set

    var inSkyblock: Boolean = false
        private set

    var island: IslandType = IslandType.NONE
        private set

    var serverId: String? = null
        private set

    var serverType: ServerType? = null
        private set

    var map: String? = null
        private set

    fun inLimbo(): Boolean = serverId == "limbo"

    val config = SkyHanniMod.feature.dev.hypixelModApi

    private val logger = LorenzLogger("debug/hypixel_api")

    @HandleEvent(priority = Int.MIN_VALUE)
    fun onServerChange(event: HypixelAPIServerChangeEvent) {
        logger.log(event.toString())
        checkHypixel()
        inSkyblock = event.serverType == GameType.SKYBLOCK
        serverType = event.serverType
        map = event.map
        serverId = event.serverName
        if (!inSkyblock) return changeIsland(IslandType.NONE)

        val newIsland = IslandType.getByIdOrUnknown(event.mode)
        if (newIsland == IslandType.UNKNOWN) {
            ChatUtils.debug("Unknown island detected: '$newIsland'")
            logger.log("Unknown Island: '$newIsland'")
        } else {
            logger.log("Island: '$newIsland'")
        }
        island = newIsland
        changeIsland(island)
    }

    private fun changeIsland(newIsland: IslandType) {
        if (newIsland == island) return
        val oldIsland = island
        island = newIsland
        // TODO: post island change event
        return
    }

    private fun checkHypixel() {
        if (inHypixel) return
        inHypixel = true
        // TODO: post hypixel join event
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Hypixel Mod API")
        event.addIrrelevant {
            addAll(
                "inHypixel: $inHypixel",
                "inSkyblock: $inSkyblock",
                "island: $island",
                "serverId: $serverId",
                "serverType: $serverType",
                "map: $map",
            )
        }
    }

    private fun reset() {
        logger.log("Disconnected")
        inHypixel = false
        inSkyblock = false
        island = IslandType.NONE
        serverId = null
        serverType = null
        map = null
    }

    fun checkHypixel(hypixel: Boolean) {
        if (hypixel == inHypixel) return
        sendError(
            "Hypixel check comparison with HypixelModAPI failed. Please report in discord.",
            "inHypixel comparison failed",
        )
    }

    fun checkSkyblock(skyblock: Boolean) {
        if (skyblock == inSkyblock) return
        sendError(
            "SkyBlock check comparison with HypixelModAPI failed. Please report in discord.",
            "inSkyBlock comparison failed",
        )
    }

    fun checkIsland(otherIsland: IslandType) {
        if (otherIsland == island) return
        if (otherIsland == IslandType.NONE) return
        if (otherIsland.toggleGuest() == island) return
        sendError(
            "Island check comparison with HypixelModAPI failed. Please report in discord.",
            "island comparison failed",
        )
    }

    private fun sendError(userMessage: String, internalMessage: String) {
        ErrorManager.logErrorStateWithData(
            userMessage,
            internalMessage,
            "HypixelData.skyBlock" to HypixelData.skyBlock,
            "inSkyblock" to inSkyblock,
            "HypixelData.hypixelLive" to HypixelData.hypixelLive,
            "inHypixel" to inHypixel,
            "HypixelData.skyBlockIsland" to HypixelData.skyBlockIsland,
            "island" to island,
            "HypixelData.serverId" to HypixelData.serverId,
            "serverId" to serverId,
            "serverType" to serverType,
            "map" to map,
            betaOnly = true,
            noStackTrace = true,
        )
    }

    @HandleEvent
    fun onDisconnect(event: ClientDisconnectEvent) = reset()

}
