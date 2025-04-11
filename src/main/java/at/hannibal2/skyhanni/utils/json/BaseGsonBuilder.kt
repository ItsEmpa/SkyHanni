package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.utils.KotlinTypeAdapterFactory
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.notenoughupdates.moulconfig.observer.PropertyTypeAdapterFactory

object BaseGsonBuilder {
    fun gson(): GsonBuilder = GsonBuilder().setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeSpecialFloatingPointValues()
        .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
        .registerTypeAdapterFactory(KotlinTypeAdapterFactory)
        .registerTypeAdapter(SkyHanniTypeAdapters.UUID)
        .registerTypeAdapter(SkyHanniTypeAdapters.VEC_STRING)
        .registerTypeAdapter(SkyHanniTypeAdapters.TROPHY_RARITY)
        .registerTypeAdapter(SkyHanniTypeAdapters.NEU_ITEMSTACK)
        .registerTypeAdapter(SkyHanniTypeAdapters.INTERNAL_NAME)
        .registerTypeAdapter(SkyHanniTypeAdapters.RARITY)
        .registerTypeAdapter(SkyHanniTypeAdapters.ISLAND_TYPE)
        .registerTypeAdapter(SkyHanniTypeAdapters.MOD_VERSION)
        .registerTypeAdapter(SkyHanniTypeAdapters.TRACKER_DISPLAY_MODE)
        .registerTypeAdapter(SkyHanniTypeAdapters.TIME_MARK)
        .registerTypeAdapter(SkyHanniTypeAdapters.DURATION)
        .registerTypeAdapter(SkyHanniTypeAdapters.LOCALE_DATE)
        .enableComplexMapKeySerialization()

    fun lenientGson(): GsonBuilder = gson()
        .registerTypeAdapterFactory(SkippingTypeAdapterFactory)
        .registerTypeAdapterFactory(ListEnumSkippingTypeAdapterFactory)
}
