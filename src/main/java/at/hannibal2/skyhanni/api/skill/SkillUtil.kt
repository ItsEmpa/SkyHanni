package at.hannibal2.skyhanni.api.skill

import at.hannibal2.skyhanni.api.skill.SkillAPI.activeSkill
import at.hannibal2.skyhanni.api.skill.SkillAPI.defaultSkillCap
import at.hannibal2.skyhanni.api.skill.SkillAPI.exactLevelingMap
import at.hannibal2.skyhanni.api.skill.SkillAPI.levelingMap
import at.hannibal2.skyhanni.api.skill.SkillAPI.oldExactLevelingMap
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.utils.Quad
import com.google.common.base.Splitter

object SkillUtil {

    val SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults()
    const val XP_NEEDED_FOR_60 = 111_672_425L
    const val XP_NEEDED_FOR_50 = 55_172_425L

    @Deprecated("")
    fun getSkillInfo(skill: SkillType): OldSkillInfo? {
        return SkillAPI.oldStorage?.get(skill)
    }

    // TODO: use a map
    fun xpRequiredForLevel(desiredLevel: Int): Long {
        var totalXp = 0L
        val maxLevel = 60

        if (desiredLevel <= maxLevel) {
            for (level in 1..desiredLevel) {
                totalXp += levelingMap[level]?.toLong() ?: 0L
            }
        } else {
            val xpNeeded = XP_NEEDED_FOR_60

            totalXp += xpNeeded

            var level = 60
            var xpForNext = 7000000L + 600000L
            var slope = 600000L

            while (level < desiredLevel) {
                totalXp += xpForNext
                level++
                xpForNext += slope

                if (level % 10 == 0) slope *= 2
            }
        }

        return totalXp
    }

    private fun getOrCalculateXpInLevel(level: Int): Long = levelingMap.getOrPut(level) { getXpForSpecificOverflowLevel(level) }

    // TODO: finish this
    fun updateOverflowInfo(info: NewSkillInfo) {
        val xpForMax = xpRequiredForLevel(info.level)
        var overflowXp = info.totalXp - xpForMax
        var xpForNext = 0L
        var overflowLevel = info.level

        while (overflowXp != 0L) {
            xpForNext = getOrCalculateXpInLevel(overflowLevel + 1)
            if (xpForNext < overflowXp) {
                overflowXp -= xpForNext
                overflowLevel++
            } else {
                if (xpForNext == overflowXp) {
                    overflowXp = 0
                    overflowLevel++
                    xpForNext = getOrCalculateXpInLevel(overflowLevel + 1)
                }
                break
            }
        }

        info.edit {
            this.overflowLevel = overflowLevel
            this.overflowCurrentXp = overflowXp
            this.overflowCurrentXpMax = xpForNext
        }
    }

    @Deprecated("")
    fun getLevelExact(neededXp: Long): Int {
        return oldExactLevelingMap.getOrDefault(neededXp.toInt(), defaultSkillCap[activeSkill?.lowercaseName] ?: 60)
    }

    fun getLevelFromXp(neededXp: Long): Int? {
        if (neededXp == 0L) return null
        return exactLevelingMap[neededXp]
    }

    private fun getXpForSpecificOverflowLevel(overflowLevel: Int): Long {
        require(overflowLevel > 60) { "Overflow level must be above 60." }

        var level = 60
        var slope = 600000L
        var xpForCurr = 7000000L + slope

        while (level < overflowLevel) {
            level++
            xpForCurr += slope
            if (level % 10 == 0) slope *= 2
        }

        return xpForCurr
    }

    @Deprecated("")
    fun calculateLevelXp(level: Int): Double {
        return SkillAPI.levelArray.asSequence().take(level + 1).sumOf { it.toDouble() }
    }

    @Deprecated("")
    fun calculateSkillLevel(currentXp: Long, maxSkillCap: Int): Quad<Int, Long, Long, Long> {
        var xpCurrent = currentXp
        var level = 0
        val maxLevel = maxSkillCap.coerceAtMost(60)

        while (level < maxLevel && xpCurrent >= (levelingMap[level + 1]?.toLong() ?: Long.MAX_VALUE)) {
            val xpForNextLevel = levelingMap[level + 1]?.toLong() ?: Long.MAX_VALUE
            xpCurrent -= xpForNextLevel
            level++
        }

        var xpForNext = levelingMap[level + 1]?.toLong() ?: 0L
        var overflowXp = 0L

        if (level >= maxLevel) {
            val xpNeeded = if (maxSkillCap == 50) XP_NEEDED_FOR_50 else XP_NEEDED_FOR_60

            if (currentXp >= xpNeeded) {
                overflowXp = currentXp - xpNeeded

                xpCurrent = overflowXp
                var slope = 300000L
                var xpForCurr = 4000000L + slope

                while (xpCurrent >= xpForCurr && level < 60) {
                    level++
                    xpCurrent -= xpForCurr
                    xpForCurr += slope
                    if (level % 10 == 0) slope *= 2
                }

                if (level >= 60) {
                    slope = 600000L
                    xpForCurr = 7000000L + slope
                    while (xpCurrent >= xpForCurr) {
                        level++
                        xpCurrent -= xpForCurr
                        xpForCurr += slope
                        if (level % 10 == 0) slope *= 2
                    }
                }

                xpForNext = xpForCurr
            }
        }

        return Quad(level, xpCurrent, xpForNext, overflowXp)
    }

}
