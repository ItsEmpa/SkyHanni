package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.api.skill.NewSkillInfo
import at.hannibal2.skyhanni.api.skill.SkillAPI
import at.hannibal2.skyhanni.api.skill.SkillXPInfo
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

enum class SkillType(icon: Item) {
    COMBAT(Items.golden_sword),
    FARMING(Items.golden_hoe),
    FISHING(Items.fishing_rod),
    MINING(Items.golden_pickaxe),
    FORAGING(Items.golden_axe),
    ENCHANTING(Blocks.enchanting_table),
    ALCHEMY(Items.brewing_stand),
    CARPENTRY(Blocks.crafting_table),
    TAMING(Items.spawn_egg),
    ;

    constructor(block: Block) : this(Item.getItemFromBlock(block))

    val item: ItemStack by lazy { ItemUtils.createItemStack(icon, displayName) }
    val info get() = SkillAPI.storage?.getOrPut(this, ::NewSkillInfo)
    val xpInfo get() = SkillAPI.skillXPInfoMap.getOrPut(this, ::SkillXPInfo)

    val displayName: String = name.firstLetterUppercase()

    val lowercaseName: String get() = name.lowercase()
    val uppercaseName: String get() = name

    fun isMaxLevel(): Boolean {
        val level = info?.level ?: return false
        val cap = SkillAPI.maxSkillCap[this] ?: return false
        return level == cap
    }

    override fun toString(): String = "§b$displayName"

    companion object {
        fun getByName(name: String) = getByNameOrNull(name) ?: error("Unknown Skill Type: '$name'")

        fun getByNameOrNull(name: String) = runCatching { valueOf(name.uppercase()) }.getOrNull()
    }
}
