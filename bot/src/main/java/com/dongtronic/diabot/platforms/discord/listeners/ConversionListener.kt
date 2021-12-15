package com.dongtronic.diabot.platforms.discord.listeners

import com.dongtronic.diabot.data.ConversionDTO
import com.dongtronic.diabot.exceptions.UnknownUnitException
import com.dongtronic.diabot.logic.diabetes.BloodGlucoseConverter
import com.dongtronic.diabot.logic.diabetes.GlucoseUnit
import com.dongtronic.diabot.util.Patterns
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class ConversionListener : ListenerAdapter() {
    private val logger = logger()

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return

        val message = event.message
        val messageText = message.contentRaw

        val separateMatcher = Patterns.separateBgPattern.matcher(messageText)

        if (separateMatcher.matches()) {
            sendMessage(getResult(separateMatcher.group(1), "", event), event)
        } else {
            sendMessage(recursiveReading(event, messageText), event)
        }
    }

    private fun recursiveReading(event: GuildMessageReceivedEvent, previousMessageText: String): String {
        if (event.author.isBot) return ""

        val inlineMatches = Patterns.inlineBgPattern.findAll(previousMessageText)
        val unitMatches = Patterns.unitBgPattern.findAll(previousMessageText)

        val sortedMatches = unitMatches
                .plus(inlineMatches)
                .filter { it.groups["value"] != null }
                .sortedBy { it.range.first }

        val multipleMatches = sortedMatches.count() > 1

        return sortedMatches.joinToString("\n") {
            val number = it.groups["value"]!!.value
            val unit = if (it.groups.size == 3) {
                it.groups["unit"]?.value ?: ""
            } else {
                ""
            }

            getResult(number, unit, event, multipleMatches)
        }
    }

    private fun getResult(originalNumString: String,
                          originalUnitString: String,
                          event: GuildMessageReceivedEvent,
                          multipleMatches: Boolean = false): String {
        val separator = if (multipleMatches) "─ " else ""
        val numberString = originalNumString.replace(',', '.')

        try {
            val result: ConversionDTO = if (originalUnitString.length > 1) {
                BloodGlucoseConverter.convert(numberString, originalUnitString)
            } else {
                BloodGlucoseConverter.convert(numberString, null)
            }

            BloodGlucoseConverter.getReactions(result).forEach {
                event.message.addReaction(it).queue()
            }

            return when {
                result.inputUnit === GlucoseUnit.MMOL -> String.format("$separator%s mmol/L is %s mg/dL", result.mmol, result.mgdl)
                result.inputUnit === GlucoseUnit.MGDL -> String.format("$separator%s mg/dL is %s mmol/L", result.mgdl, result.mmol)
                else -> {
                    val reply = arrayOf(
                            "$separator*I'm not sure if you gave me mmol/L or mg/dL, so I'll give you both.*",
                            "┌%s mg/dL is **%s mmol/L**",
                            "└%s mmol/L is **%s mg/dL**").joinToString(
                            "%n")

                    String.format(reply, numberString, result.mmol, numberString, result.mgdl)
                }
            }
        } catch (ex: IllegalArgumentException) {
            // Ignored on purpose
            logger.warn("IllegalArgumentException occurred but was ignored in BG conversion")
        } catch (ex: UnknownUnitException) {
            // Ignored on purpose
        }

        return ""
    }

    private fun sendMessage(message: String, event: GuildMessageReceivedEvent) {
        val channel = event.channel
        if (message.isNotEmpty()) {
            channel.sendMessage(message).queue()
        }

    }
}
