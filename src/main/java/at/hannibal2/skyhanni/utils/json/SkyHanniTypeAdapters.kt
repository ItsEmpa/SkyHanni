package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.item.ItemStack
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object SkyHanniTypeAdapters {

    val NEU_ITEMSTACK: TypeAdapter<ItemStack> = SimpleStringTypeAdapter(NeuItems::saveNBTData, NeuItems::loadNBTData)

    val UUID: TypeAdapter<UUID> = SimpleStringTypeAdapter(
        java.util.UUID::toString,
        java.util.UUID::fromString,
    )

    val INTERNAL_NAME: TypeAdapter<NeuInternalName> = SimpleStringTypeAdapter(
        NeuInternalName::asString,
        { toInternalName() },
    )

    val VEC_STRING: TypeAdapter<LorenzVec> = SimpleStringTypeAdapter(
        LorenzVec::asStoredString,
        LorenzVec::decodeFromString,
    )

    val TROPHY_RARITY = SimpleStringTypeAdapter.forEnum<TrophyRarity>()

    val TIME_MARK: TypeAdapter<SimpleTimeMark> = SimpleTypeAdapter(
        { value(it.toMillis()) },
        { nextString().toLong().asTimeMark() },
    )

    val DURATION: TypeAdapter<Duration> = SimpleTypeAdapter(
        { value(it.inWholeMilliseconds) },
        { nextString().toLong().milliseconds },
    )

    val CROP_TYPE = SimpleStringTypeAdapter.forEnum<CropType>()

    val PEST_TYPE = SimpleStringTypeAdapter.forEnum<PestType>()

    val SKYBLOCK_STAT = SimpleStringTypeAdapter.forEnum<SkyblockStat>(SkyblockStat.UNKNOWN)

    val MOD_VERSION: TypeAdapter<ModVersion> = SimpleStringTypeAdapter(ModVersion::asString, ModVersion::fromString)

    val TRACKER_DISPLAY_MODE = SimpleStringTypeAdapter.forEnum<SkyHanniTracker.DefaultDisplayMode>()
    val ISLAND_TYPE = SimpleStringTypeAdapter.forEnum<IslandType>(IslandType.UNKNOWN)
    val RARITY = SimpleStringTypeAdapter.forEnum<LorenzRarity>()

    val LOCALE_DATE = SimpleStringTypeAdapter(
        LocalDate::toString,
        LocalDate::parse,
    )

    inline fun <reified T> GsonBuilder.registerTypeAdapter(typeAdapter: TypeAdapter<T>, nullSafe: Boolean = true): GsonBuilder {
        return registerTypeAdapter(T::class.java, if (nullSafe) typeAdapter.nullSafe() else typeAdapter)
    }

    inline fun <reified T> GsonBuilder.registerTypeAdapter(
        crossinline write: (JsonWriter, T) -> Unit,
        crossinline read: (JsonReader) -> T,
    ): GsonBuilder = registerTypeAdapter<T>(
        SimpleTypeAdapter(
            { value -> write(this, value) },
            { read(this) },
        )
    )
}
