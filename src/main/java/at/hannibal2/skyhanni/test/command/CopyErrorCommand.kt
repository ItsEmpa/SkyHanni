package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.common.cache.CacheBuilder
import net.minecraft.client.Minecraft
import java.util.*
import java.util.concurrent.TimeUnit

object CopyErrorCommand {
    // random id -> error message
    private val errorMessages = mutableMapOf<String, String>()
    private val fullErrorMessages = mutableMapOf<String, String>()
    private var cache =
        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build<Pair<String, Int>, Unit>()

    fun command(array: Array<String>) {
        if (array.size != 1) {
            LorenzUtils.chat("§cUse /shcopyerror <error id>")

            return
        }

        val id = array[0]
        val fullErrorMessage = LorenzUtils.isControlKeyDown()
        val errorMessage = if (fullErrorMessage) {
            fullErrorMessages[id]
        } else {
            errorMessages[id]
        }
        val name = if (fullErrorMessage) "Ful error" else "Error"
        LorenzUtils.chat(errorMessage?.let {
            OSUtils.copyToClipboard(it)
            "§e[SkyHanni] $name copied into the clipboard, please report it on the SkyHanni discord!"
        } ?: "§c[SkyHanni] Error id not found!")
    }

    fun logError(error: Throwable, message: String) {
        Minecraft.getMinecraft().thePlayer ?: throw Error(message, error)

        val pair = error.stackTrace[0].let { it.fileName to it.lineNumber }
//        if (cache.getIfPresent(pair) != null) return
        cache.put(pair, Unit)

        val fullStackTrace = error.getExactStackTrace(true).joinToString("\n")
        val stackTrace = error.getExactStackTrace(false).joinToString("\n").removeSpam()
        val randomId = UUID.randomUUID().toString()

        errorMessages[randomId] = "```\nSkyHanni ${SkyHanniMod.version}: $message\n \n$stackTrace\n```"
        fullErrorMessages[randomId] =
            "```\nSkyHanni ${SkyHanniMod.version}: $message\n(full stack trace)\n \n$fullStackTrace\n```"

        LorenzUtils.clickableChat(
            "§c[SkyHanni ${SkyHanniMod.version}]: $message. Click here to copy the error into the clipboard.",
            "shcopyerror $randomId"
        )
    }
}

private fun Throwable.getExactStackTrace(full: Boolean, parent: List<String> = emptyList()): List<String> = buildList {
    add("Caused by " + javaClass.name + ": $message")

    val breakAfter = listOf(
        "at net.minecraftforge.client.ClientCommandHandler.executeCommand(",
    )
    val replace = mapOf(
        "io.mouberry,notenoughupdates" to "NEU",
        "at.hannibal2.skyhanni" to "SH",
    )

    for (traceElement in stackTrace) {
        var text = "\tat $traceElement"
        if (!full) {
            if (text in parent) {
                println("broke at: $text")
                break
            }
        }
        if (!full) {
            for ((from, to) in replace) {
                text = text.replace(from, to)
            }
        }
        add(text)
        if (!full) {
            if (breakAfter.any { text.contains(it) }) {
                println("breakAfter: $text")
                break
            }
        }
    }

    cause?.let {
        addAll(it.getExactStackTrace(full, this))
    }
}

private fun String.removeSpam(): String {
    val ignored = listOf(
        "at io.netty.",
        "at net.minecraft.network.",
        "at net.minecraftforge.fml.common.network.handshake.",
        "at java.lang.Thread.run",
        "at com.google.gson.internal.",
        "at net.minecraftforge.fml.common.eventhandler.",
        "at java.util.concurrent.",
        "at sun.reflect.",
        "at net.minecraft.client.Minecraft.addScheduledTask(",
        "at java.lang.reflect.",
        "at at.hannibal2.skyhanni.config.commands.Commands\$",
    )
    return split("\n").filter { line -> !ignored.any { line.contains(it) } }.joinToString("\n")
}
