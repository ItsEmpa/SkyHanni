package at.hannibal2.skyhanni.api.skill

import at.hannibal2.skyhanni.api.skill.SkillUtil.SPACE_SPLITTER
import at.hannibal2.skyhanni.api.skill.SkillUtil.XP_NEEDED_FOR_50
import at.hannibal2.skyhanni.api.skill.SkillUtil.XP_NEEDED_FOR_60
import at.hannibal2.skyhanni.api.skill.SkillUtil.calculateSkillLevel
import at.hannibal2.skyhanni.api.skill.SkillUtil.getLevelFromXp
import at.hannibal2.skyhanni.api.skill.SkillUtil.updateOverflowInfo
import at.hannibal2.skyhanni.api.skill.SkillUtil.xpRequiredForLevel
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuSkillLevelJson
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.SkillExpGainEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillProgress
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.formatLongOrUserError
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.command.CommandBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SkillAPI {
    private val patternGroup = RepoPattern.group("api.skilldisplay")

    // TODO add regex tests
    private val skillPercentPattern by patternGroup.pattern(
        "skill.percent",
        "\\+(?<gained>[\\d.,]+) (?<skillName>.+) \\((?<progress>[\\d.]+)%\\)",
    )
    private val skillPattern by patternGroup.pattern(
        "skill",
        "\\+(?<gained>[\\d.,]+) (?<skillName>\\w+) \\((?<current>[\\d.,]+)/(?<needed>[\\d.,]+)\\)",
    )
    private val skillMultiplierPattern by patternGroup.pattern(
        "skill.multiplier",
        "\\+(?<gained>[\\d.,]+) (?<skillName>.+) \\((?<current>[\\d.,]+)/(?<needed>[\\d,.]+[kmb])\\)",
    )
    private val skillTabPercentPattern by patternGroup.pattern(
        "skill.tab",
        " (?<type>\\w+)(?: (?<level>\\d+))?: §r§a(?<progress>[0-9.]+)%",
    )
    private val maxSkillTabPattern by patternGroup.pattern(
        "skill.tab.max",
        " (?<type>\\w+) (?<level>\\d+): §r§c§lMAX",
    )
    private val skillTabPattern by patternGroup.pattern(
        "skill.tab.nopercent",
        " §r§a(?<type>\\w+)(?: (?<level>\\d+))?: §r§e(?<current>[0-9,.]+)§r§6/§r§e(?<needed>[0-9kmb]+)",
    )

    private val actionbarPatternsList = listOf(skillPattern, skillPercentPattern, skillMultiplierPattern)
    private val widgetPatternsList = listOf(skillTabPercentPattern, maxSkillTabPattern, skillTabPattern)

    val skillXPInfoMap = enumMapOf<SkillType, SkillXPInfo>()

    @Deprecated("")
    var oldOldSkillInfoMap = mutableMapOf<SkillType?, OldSkillInfo?>()
        private set
    var oldSkillInfoMap = enumMapOf<SkillType, NewSkillInfo>()
        private set

    @Deprecated("")
    val oldStorage: MutableMap<SkillType, OldSkillInfo>? get() = mutableMapOf()
    val storage: MutableMap<SkillType, NewSkillInfo>? get() = ProfileStorageData.profileSpecific?.skillData

    @Deprecated("")
    var oldExactLevelingMap = mapOf<Int, Int>()
    var exactLevelingMap = mapOf<Long, Int>()
        private set
    var levelingMap = mutableMapOf<Int, Long>()
    var levelArray = listOf<Int>()
    var activeSkill: SkillType? = null
    var defaultSkillCap = mapOf<String, Int>()
    var maxSkillCap = enumMapOf<SkillType, Int>()
        private set

    var showDisplay = false
    var lastUpdate = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val activeSkill = activeSkill ?: return
        val info = activeSkill.xpInfo ?: return
        if (!info.sessionTimerActive) return

        val time = when (activeSkill) {
            SkillType.FARMING -> SkillProgress.etaConfig.farmingPauseTime
            SkillType.MINING -> SkillProgress.etaConfig.miningPauseTime
            SkillType.COMBAT -> SkillProgress.etaConfig.combatPauseTime
            SkillType.FORAGING -> SkillProgress.etaConfig.foragingPauseTime
            SkillType.FISHING -> SkillProgress.etaConfig.fishingPauseTime
            else -> 0
        }.seconds
        if (info.lastUpdate.passedSince() > time) {
            info.sessionTimerActive = false
        }
        if (info.sessionTimerActive) {
            info.timeActive++
        }
    }

    @SubscribeEvent
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val actionBar = event.actionBar.removeColor()
        val components = SPACE_SPLITTER.splitToList(actionBar)
        for (component in components) {
            val matcher = actionbarPatternsList.firstNotNullOfOrNull { pattern ->
                pattern.matcher(component).takeIf { it.matches() }
            } ?: continue
            val skillName = matcher.group("skillName")
            val skillType = SkillType.getByNameOrNull(skillName) ?: return
            val skillInfo = skillType.info ?: return
            val skillXp = skillType.xpInfo ?: return
            activeSkill = skillType
            when (matcher.pattern()) {
                skillPattern, skillMultiplierPattern -> handleActionBarSkill(matcher, skillType, skillInfo)
                skillPercentPattern -> handleActionBarSkillPercent(matcher, skillType, skillInfo)
            }

            SkillExpGainEvent(skillType, matcher.group("gained").formatDouble()).postAndCatch()

            showDisplay = true
            lastUpdate = SimpleTimeMark.now()
            skillXp.lastUpdate = SimpleTimeMark.now()
            skillXp.sessionTimerActive = true
            SkillProgress.updateDisplay()
            SkillProgress.hideInActionBar = listOf(component)
            return
        }
    }

    @SubscribeEvent
    fun onTabWidget(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.SKILLS)) return
        for (line in event.widget.lines) {
            val matcher = widgetPatternsList.firstNotNullOfOrNull { pattern ->
                pattern.matcher(line).takeIf { it.matches() }
            } ?: continue
            val skill = SkillType.getByNameOrNull(matcher.group("type")) ?: continue
            val info = skill.info ?: continue
            val level = matcher.groupOrNull("level")?.romanToDecimalIfNecessary() ?: 0
            when (matcher.pattern()) {
                skillTabPattern -> handleWidgetSkill(matcher, skill, info, level)
            }
        }
    }

    private fun handleWidgetSkill(matcher: Matcher, skill: SkillType, info: NewSkillInfo, level: Int) {
        val currentXp = matcher.group("current").formatLong()
        val neededXp = matcher.group("needed").formatLong()
        val totalXp = currentXp + xpRequiredForLevel(level)
        val gained = matcher.group("gained")
        info.edit {
            this.totalXp = totalXp
            this.level = level
            this.currentLevelXp = currentXp
            this.currentLevelXpMax = neededXp
            this.lastGain = gained
            this.lastTablistPercent = null
            this.hasConfirmedLevel = true
        }
        info.updateNoOverflow()
    }

    private fun handleActionBarSkill(matcher: Matcher, skill: SkillType, info: NewSkillInfo) {
        val currentLevelXp = matcher.group("current").formatLong()
        val currentLevelMaxXp = matcher.group("needed").formatLong()
        val level = getLevelFromXp(currentLevelMaxXp) ?: info.level
        var levelCap = info.currentLevelCap
        if (level > levelCap) levelCap = level
        val totalXp = xpRequiredForLevel(level) + currentLevelXp
        val gained = matcher.group("gained")
        info.edit {
            this.totalXp = totalXp

            this.level = level
            this.currentLevelXp = currentLevelXp
            this.currentLevelXpMax = currentLevelMaxXp

            this.currentLevelCap = levelCap
            this.lastGain = gained
            this.lastActionBarPercent = null
        }
        if (skill.isMaxLevel()) info.updateOverflow()
        else info.updateNoOverflow()
    }

    private fun handleActionBarSkillPercent(matcher: Matcher, skill: SkillType, info: NewSkillInfo) {
        val progress = matcher.group("progress").formatDouble()
        if (info.lastActionBarPercent == progress) return
        val gained = matcher.group("gained")
        val xpForNext = info.currentLevelXpMax
        val currentLevelXp = (xpForNext * progress / 100).toLong()
        val totalXp = info.totalXp - info.currentLevelXp + currentLevelXp
        info.edit {
            this.totalXp = totalXp
            this.currentLevelXp = currentLevelXp
            this.lastGain = gained

            this.lastActionBarPercent = progress
        }

        if (!skill.isMaxLevel()) {
            info.updateNoOverflow()
            return
        }
        updateOverflowInfo(info)
    }

    @SubscribeEvent
    fun onNEURepoReload(event: NeuRepositoryReloadEvent) {
        val data = event.readConstant<NeuSkillLevelJson>("leveling")

        levelArray = data.levelingXp
        levelingMap = levelArray.withIndex().associate { (index, xp) -> (index + 1) to xp.toLong() }.toMutableMap()
        exactLevelingMap = levelingMap.entries.associate { (index, xp) -> xp to index }
        defaultSkillCap = data.levelingCaps
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.inventoryName != "Your Skills") return
        for (stack in event.inventoryItems.values) {
            val lore = stack.getLore()
            if (lore.none { it.contains("Click to view!") || it.contains("Not unlocked!") }) continue
            val cleanName = stack.cleanName()
            val split = cleanName.split(" ")
            val skillName = split.first()
            val skill = SkillType.getByNameOrNull(skillName) ?: continue
            val skillLevel = if (split.size > 1) split.last().romanToDecimalIfNecessary() else 0
            val skillInfo = skill.info ?: continue

            val emptyLineIndex = lore.indexOfFirst { it.removeColor().isBlank() }
            if (emptyLineIndex == -1) continue
            val nextLine = lore.getOrNull(emptyLineIndex + 1) ?: continue
            val progressLine = lore.getOrNull(emptyLineIndex + 2)?.removeColor() ?: continue
            val progress = progressLine.substring(progressLine.lastIndexOf(' ') + 1)
            if (nextLine == "§7§8Max Skill level reached!") {
                onUpdateMax(progress, skillInfo, skillLevel)
            } else {
                onUpdateNotMax(progress, skillInfo, skillLevel)
            }
        }
    }

    private fun onUpdateMax(progress: String, info: NewSkillInfo, skillLevel: Int) {
        val totalXp = progress.formatLong()
        val xpForMax = xpRequiredForLevel(skillLevel)
        val overflowXp = totalXp - xpForMax
        info.edit {
            this.totalXp = totalXp
            this.level = skillLevel
            this.currentLevelXp = overflowXp
            this.currentLevelXpMax = 0
            this.hasConfirmedLevel = true
        }
        info.updateOverflow()
    }

    private fun onUpdateNotMax(progress: String, info: NewSkillInfo, skillLevel: Int) {
        val split = progress.split("/")
        val currentXp = split.first().formatLong()
        val neededXp = split.last().formatLong()
        val levelXp = xpRequiredForLevel(skillLevel)
        val totalXp = levelXp + currentXp

        info.edit {
            this.totalXp = totalXp
            this.level = skillLevel
            this.currentLevelXp = currentXp
            this.currentLevelXpMax = neededXp
            this.hasConfirmedLevel = true
        }
        info.updateNoOverflow()
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Skills")
        val storage = storage ?: run {
            event.addIrrelevant("SkillMap is empty")
            return
        }

        event.addIrrelevant {
            val activeSkill = activeSkill
            if (activeSkill == null) {
                add("activeSkill is null")
            } else {
                add("active skill:")
                storage[activeSkill]?.let { info ->
                    addDebug(activeSkill, info)
                }
                add("")
                add("")
            }

            for ((skill, info) in storage) {
                if (skill != activeSkill) addDebug(skill, info)
            }
        }
    }

    private fun MutableList<String>.addDebug(skillType: SkillType, skillInfo: NewSkillInfo) {
        add("Name: $skillType")
        add("-  Level: ${skillInfo.level}")
        add("-  CurrentXp: ${skillInfo.currentLevelXp}")
        add("-  CurrentXpMax: ${skillInfo.currentLevelXpMax}")
        add("-  TotalXp: ${skillInfo.totalXp}")
        add("-  OverflowLevel: ${skillInfo.overflowLevel}")
        add("-  OverflowCurrentXp: ${skillInfo.overflowCurrentXp}")
        add("-  OverflowCurrentXpMax: ${skillInfo.overflowCurrentXpMax}")
        add("-  OverflowTotalXp: ${skillInfo.overflowTotalXp}")
        add("-  CustomGoalLevel: ${skillInfo.customGoalLevel}\n")
    }

    @Deprecated("")
    private fun updateSkillInfo(existingLevel: OldSkillInfo, level: Int, currentXp: Long, maxXp: Long, totalXp: Long, gained: String) {
        val cap = defaultSkillCap[activeSkill?.lowercaseName] ?: 60
        val add = if (level >= 50) {
            when (cap) {
                50 -> XP_NEEDED_FOR_50
                60 -> XP_NEEDED_FOR_60
                else -> 0
            }
        } else {
            0
        }

        val (levelOverflow, currentOverflow, currentMaxOverflow, totalOverflow) =
            calculateSkillLevel(totalXp + add, defaultSkillCap[activeSkill?.lowercaseName] ?: 60)

        existingLevel.apply {
            this.totalXp = totalXp
            this.currentXp = currentXp
            this.currentXpMax = maxXp
            this.level = level

            this.overflowTotalXp = totalOverflow
            this.overflowCurrentXp = currentOverflow
            this.overflowCurrentXpMax = currentMaxOverflow
            this.overflowLevel = levelOverflow

            this.lastGain = gained
        }
    }

    fun onCommand(it: Array<String>) {
        if (it.isEmpty()) {
            return commandHelp()
        }

        val first = it.first()
        if (it.size == 1) {
            when (first) {
                "goal" -> {
                    ChatUtils.chat("§bSkill Custom Goal Level")
                    val map = storage?.filter { it.value.customGoalLevel != 0 } ?: return
                    if (map.isEmpty()) {
                        ChatUtils.userError("You haven't set any custom goals yet!")
                    }
                    map.forEach { (skill, info) ->
                        ChatUtils.chat("§e${skill.displayName}: §b${info.customGoalLevel}")
                    }
                    return
                }
            }
        }

        if (it.size == 2) {
            val second = it[1]
            when (first) {
                "levelwithxp" -> {
                    val xp = second.formatLongOrUserError() ?: return
                    val (overflowLevel, current, needed, _) = calculateSkillLevel(xp, 60)
                    ChatUtils.chat(
                        "With §b${xp.addSeparators()} §eXP you would be level §b$overflowLevel " +
                            "§ewith progress (§b${current.addSeparators()}§e/§b${needed.addSeparators()}§e) XP",
                    )
                    return
                }

                "xpforlevel" -> {
                    val level = second.toIntOrNull()
                    if (level == null) {
                        ChatUtils.userError("Not a valid number: '$second'")
                        return
                    }
                    val neededXP = xpRequiredForLevel(level)
                    ChatUtils.chat("You need §b${neededXP.addSeparators()} §eXP to reach level §b${level.toDouble()}")
                    return
                }

                "goal" -> {
                    val rawSkill = it[1].lowercase()
                    val skillType = SkillType.getByNameOrNull(rawSkill)
                    if (skillType == null) {
                        ChatUtils.userError("Unknown Skill type: $rawSkill")
                        return
                    }
                    val skill = oldStorage?.get(skillType) ?: return
                    skill.customGoalLevel = 0
                    ChatUtils.chat("Custom goal level for §b${skillType.displayName} §ereset")
                }
            }
        }
        if (it.size == 3) {
            when (first) {
                "goal" -> {
                    val rawSkill = it[1].lowercase()
                    val skillType = SkillType.getByNameOrNull(rawSkill)
                    if (skillType == null) {
                        ChatUtils.userError("Unknown Skill type: $rawSkill")
                        return
                    }
                    val rawLevel = it[2]
                    val targetLevel = rawLevel.toIntOrNull()
                    if (targetLevel == null) {
                        ChatUtils.userError("$rawLevel is not a valid number.")
                        return
                    }
                    val skill = oldStorage?.get(skillType) ?: return

                    if (targetLevel <= skill.overflowLevel) {
                        ChatUtils.userError(
                            "Custom goal level ($targetLevel) must be greater than your current level (${skill.overflowLevel}).",
                        )
                        return
                    }

                    skill.customGoalLevel = targetLevel
                    ChatUtils.chat("Custom goal level for §b${skillType.displayName} §eset to §b$targetLevel")
                    return
                }
            }
        }
        commandHelp()
    }

    fun onComplete(strings: Array<String>): List<String> {
        return when (strings.size) {
            1 -> listOf("levelwithxp", "xpforlevel", "goal")
            2 -> if (strings[0].equals("goal", true)) CommandBase.getListOfStringsMatchingLastWord(
                strings,
                SkillType.entries.map(SkillType::displayName)
            ) else listOf()

            else -> listOf()
        }
    }

    private fun commandHelp() {
        ChatUtils.chat(
            listOf(
                "§6/shskills levelwithxp <xp> - §bGet a level with the given current XP.",
                "§6/shskills xpforlevel <desiredLevel> - §bGet how much XP you need for a desired level.",
                "§6/shskills goal - §bView your current goal",
                "§6/shskills goal <skill> <level> - §bDefine your goal for <skill>",
                "",
            ).joinToString("\n"),
            prefix = false,
        )
    }
}

