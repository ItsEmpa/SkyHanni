package at.hannibal2.skyhanni.neu

import com.google.gson.JsonObject
import io.github.moulberry.notenoughupdates.events.ProfileDataLoadedEvent
import io.github.moulberry.notenoughupdates.events.RepositoryReloadEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@Suppress("SkyHanniModuleInspection")
object NEUEvents {

    var profileDataListener: (JsonObject?) -> Unit = {}

    var repositoryReloadListener: () -> Unit = {}

    @SubscribeEvent
    fun onProfileDataLoaded(event: ProfileDataLoadedEvent) = profileDataListener(event.data)

    @SubscribeEvent
    fun onNeuRepoReload(event: RepositoryReloadEvent) = repositoryReloadListener()

}
