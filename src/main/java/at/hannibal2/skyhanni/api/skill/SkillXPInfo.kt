package at.hannibal2.skyhanni.api.skill

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import java.util.*

data class SkillXPInfo(
    var lastTotalXp: Float = 0f,
    var xpGainQueue: LinkedList<Float> = LinkedList(),
    var xpGainHour: Float = 0f,
    var xpGainLast: Float = 0f,
    var timer: Int = 3,
    var sessionTimerActive: Boolean = false,
    var isActive: Boolean = false,
    var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast(),
    var timeActive: Long = 0L,
)
