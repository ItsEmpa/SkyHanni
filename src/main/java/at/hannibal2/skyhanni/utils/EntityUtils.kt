package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToIgnoreY
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

@SkyHanniModule
object EntityUtils {

    fun EntityLivingBase.hasNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): Boolean {
        return getNameTagWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity) != null
    }

    fun getPlayerEntities(): MutableList<EntityOtherPlayerMP> = getEntities<EntityOtherPlayerMP>().filter { !it.isNPC() }.toMutableList()

    fun EntityLivingBase.getAllNameTagsInRadiusWith(
        contains: String,
        radius: Double = 3.0,
    ): List<EntityArmorStand> = getArmorStandsInRadius(getLorenzVec().add(y = 3), radius).filter {
        it.name.contains(contains)
    }

    fun EntityLivingBase.getNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): EntityArmorStand? = getAllNameTagsWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity).firstOrNull()

    fun EntityLivingBase.getAllNameTagsWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): List<EntityArmorStand> {
        val center = getLorenzVec().add(y = y)
        return getArmorStandsInRadius(center, inaccuracy).filter {
            val result = it.name.contains(contains)
            if (debugWrongEntity && !result) {
                LorenzUtils.consoleLog("wrong entity in aabb: '" + it.name + "'")
            }
            if (debugRightEntity && result) {
                LorenzUtils.consoleLog("mob: " + center.printWithAccuracy(2))
                LorenzUtils.consoleLog("nametag: " + it.getLorenzVec().printWithAccuracy(2))
                LorenzUtils.consoleLog("accuracy: " + (it.getLorenzVec() - center).printWithAccuracy(3))
            }
            result
        }
    }

    private fun getArmorStandsInRadius(center: LorenzVec, radius: Double): List<EntityArmorStand> {
        val a = center.add(-radius, -radius - 3, -radius).toBlockPos()
        val b = center.add(radius, radius + 3, radius).toBlockPos()
        val alignedBB = AxisAlignedBB(a, b)
        val clazz = EntityArmorStand::class.java
        val worldObj = Minecraft.getMinecraft()?.theWorld ?: return emptyList()
        return worldObj.getEntitiesWithinAABB(clazz, alignedBB)
    }

    fun EntityLivingBase.hasBossHealth(health: Int): Boolean = this.hasMaxHealth(health, true)

    // TODO remove baseMaxHealth
    fun EntityLivingBase.hasMaxHealth(health: Int, boss: Boolean = false, maxHealth: Int = baseMaxHealth): Boolean {
        val derpyMultiplier = if (LorenzUtils.isDerpy) 2 else 1
        if (maxHealth == health * derpyMultiplier) return true

        if (!boss && !DungeonAPI.inDungeon()) {
            // Corrupted
            if (maxHealth == health * 3 * derpyMultiplier) return true
            // Runic
            if (maxHealth == health * 4 * derpyMultiplier) return true
            // Corrupted+Runic
            if (maxHealth == health * 12 * derpyMultiplier) return true
        }

        return false
    }

    fun EntityPlayer.getSkinTexture(): String? {
        val gameProfile = gameProfile ?: return null

        return gameProfile.properties.entries()
            .filter { it.key == "textures" }
            .map { it.value }
            .firstOrNull { it.name == "textures" }
            ?.value
    }

    inline fun <reified T : Entity> getEntitiesNextToPlayer(radius: Double): Sequence<T> =
        getEntitiesNearby<T>(LocationUtils.playerLocation(), radius)

    inline fun <reified T : Entity> getEntitiesNearby(location: LorenzVec, radius: Double): Sequence<T> =
        getEntities<T>().filter { it.distanceTo(location) < radius }

    inline fun <reified T : Entity> getEntitiesNearbyIgnoreY(location: LorenzVec, radius: Double): Sequence<T> =
        getEntities<T>().filter { it.distanceToIgnoreY(location) < radius }

    fun EntityLivingBase.isAtFullHealth() = baseMaxHealth == health.toInt()

    fun EntityArmorStand.hasSkullTexture(skin: String): Boolean {
        if (inventory == null) return false
        return inventory.any { it != null && it.getSkullTexture() == skin }
    }

    fun EntityPlayer.isNPC() = !isRealPlayer()

    fun EntityLivingBase.hasPotionEffect(potion: Potion) = getActivePotionEffect(potion) != null

    fun EntityLivingBase.getArmorInventory(): Array<ItemStack?>? =
        if (this is EntityPlayer) inventory.armorInventory else null

    fun EntityEnderman.getBlockInHand(): IBlockState? = heldBlockState

    inline fun <reified R : Entity> getEntities(): Sequence<R> = getAllEntities().filterIsInstance<R>()

    fun getAllEntities(): Sequence<Entity> = Minecraft.getMinecraft()?.theWorld?.loadedEntityList?.let {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread) it else it.toMutableList()
    }?.asSequence()?.filterNotNull() ?: emptySequence()

    fun Entity.canBeSeen(radius: Double = 150.0) = getLorenzVec().add(y = 0.5).canBeSeen(radius)

    fun getEntityByID(entityId: Int) = Minecraft.getMinecraft()?.thePlayer?.entityWorld?.getEntityByID(entityId)

    @SubscribeEvent
    fun onEntityRenderPre(event: RenderLivingEvent.Pre<*>) {
        val shEvent = SkyHanniRenderEntityEvent.Pre(event.entity, event.renderer, event.x, event.y, event.z)
        if (shEvent.postAndCatch()) {
            event.cancel()
        }
    }

    @SubscribeEvent
    fun onEntityRenderPost(event: RenderLivingEvent.Post<*>) {
        SkyHanniRenderEntityEvent.Post(event.entity, event.renderer, event.x, event.y, event.z).postAndCatch()
    }

    @SubscribeEvent
    fun onEntityRenderSpecialsPre(event: RenderLivingEvent.Specials.Pre<*>) {
        val shEvent = SkyHanniRenderEntityEvent.Specials.Pre(event.entity, event.renderer, event.x, event.y, event.z)
        if (shEvent.postAndCatch()) {
            event.cancel()
        }
    }

    @SubscribeEvent
    fun onEntityRenderSpecialsPost(event: RenderLivingEvent.Specials.Post<*>) {
        SkyHanniRenderEntityEvent.Specials.Post(event.entity, event.renderer, event.x, event.y, event.z).postAndCatch()
    }

    fun EntityLivingBase.isCorrupted() = baseMaxHealth == health.toInt().derpy() * 3 || isRunicAndCorrupt()
    fun EntityLivingBase.isRunic() = baseMaxHealth == health.toInt().derpy() * 4 || isRunicAndCorrupt()
    fun EntityLivingBase.isRunicAndCorrupt() = baseMaxHealth == health.toInt().derpy() * 3 * 4

    fun EntityLivingBase.matchesHealth(checkedHealth: Int, ignoreDerpy: Boolean = false): Boolean {
        var targetHealth = checkedHealth
        if (isRunic()) targetHealth *= 4
        if (isCorrupted()) targetHealth *= 3
        val derpyHealth = if (ignoreDerpy) health.toInt() else health.toInt().derpy()
        return derpyHealth == targetHealth
    }

    fun Entity.cleanName() = name.removeColor()

    /** If no alpha is set or alpha is set to 255 it will set the alpha to 127 */
    fun EntityLivingBase.highlight(color: LorenzColor, alpha: Int = 127, condition: () -> Boolean = { true }) {
        highlight(color.addOpacity(alpha.coerceIn(0..255)), condition)
    }

    /** If no alpha is set or alpha is set to 255 it will set the alpha to 127 */
    fun EntityLivingBase.highlight(color: Color, condition: () -> Boolean = { true }) {
        val highlightColor = color.takeIf { it.alpha == 255 }?.addAlpha(127) ?: color
        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(this, highlightColor.rgb, condition)
    }

    /**
     * @return a fake player with the same skin as the real player
     */
    fun getFakePlayer(): EntityOtherPlayerMP {
        val mc = Minecraft.getMinecraft()
        val player = mc.thePlayer
        return object : EntityOtherPlayerMP(
            mc.theWorld,
            player.gameProfile,
        ) {
            override fun getLocationSkin() =
                player.locationSkin ?: DefaultPlayerSkin.getDefaultSkin(player.uniqueID)

            override fun getTeam() = object : ScorePlayerTeam(null, null) {
                override fun getNameTagVisibility() = EnumVisible.NEVER
            }

            override fun isWearing(part: EnumPlayerModelParts?) =
                player.isWearing(part) && part != EnumPlayerModelParts.CAPE
        }
    }
}

private fun Event.cancel() {
    isCanceled = true
}
