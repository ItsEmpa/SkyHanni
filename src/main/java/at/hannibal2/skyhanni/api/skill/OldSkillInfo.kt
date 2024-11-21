package at.hannibal2.skyhanni.api.skill

import com.google.gson.annotations.Expose

data class NewSkillInfo(
    @Expose var totalXp: Long = 0, // total xp of that skill

    // real info
    @Expose var level: Int = 0, // current skill level
    @Expose var currentLevelXp: Long = 0, // xp over the current level
    @Expose var currentLevelXpMax: Long = 0, // total xp needed to reach the next level, 0 when level = currentLevelCap

    // overflow info
    @Expose var overflowLevel: Int = 0, // overflow level
    @Expose var overflowCurrentXp: Long = 0, // xp over the overflow level
    @Expose var overflowCurrentXpMax: Long = 0, // total xp needed to reach the next overflow level
    @Expose var overflowTotalXp: Long = 0, // overflow xp (0 when no overflow)

    // other info
    @Expose var currentLevelCap: Int = 50, // level cap for the skill (used for farming/taming where the cap can change)
    @Expose var lastGain: String = "", // last gain of xp // TODO: specify what it is better
    @Expose var customGoalLevel: Int = 0, // custom goal level
) {
    var hasConfirmedLevel: Boolean = false

    var lastActionBarPercent: Double? = null // last percent of the skill showed in actionbar
    var lastTablistPercent: Double? = null // last percent of the skill showed in tablist

    fun edit(info: NewSkillInfo.() -> Unit) = info()

    fun updateNoOverflow() {
        this.overflowLevel = level
        this.overflowCurrentXp = currentLevelXp
        this.overflowCurrentXpMax = currentLevelXpMax
    }

    fun updateOverflow() = SkillUtil.updateOverflowInfo(this)
}

@Deprecated("")
data class OldSkillInfo(
    @Expose var level: Int = 0,
    @Expose var totalXp: Long = 0,
    @Expose var currentXp: Long = 0,
    @Expose var currentXpMax: Long = 0,
    @Expose var overflowLevel: Int = 0,
    @Expose var overflowCurrentXp: Long = 0,
    @Expose var overflowTotalXp: Long = 0,
    @Expose var overflowCurrentXpMax: Long = 0,
    @Expose var lastGain: String = "",
    @Expose var customGoalLevel: Int = 0,
)
