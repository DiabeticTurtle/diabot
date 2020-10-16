package com.dongtronic.diabot.platforms.discord.commands.admin.rewards

import com.dongtronic.diabot.data.mongodb.RewardsDAO
import com.dongtronic.diabot.platforms.discord.commands.DiscordCommand
import com.dongtronic.diabot.util.logger
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import org.apache.commons.lang3.StringUtils

class AdminRewardDeleteCommand(category: Command.Category, parent: Command?) : DiscordCommand(category, parent) {

    private val logger = logger()

    init {
        this.name = "delete"
        this.help = "Delete role reward"
        this.guildOnly = true
        this.aliases = arrayOf("d", "del", "r", "rm", "remove")
        this.userPermissions = this.parent!!.userPermissions
    }

    override fun execute(event: CommandEvent) {
        val args = event.args.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        try {
            if (args.size != 2) {
                throw IllegalArgumentException("Required and reward role IDs are required")
            }

            if (!StringUtils.isNumeric(args[0]) || !StringUtils.isNumeric(args[1])) {
                throw IllegalArgumentException("Role IDs must be numeric")
            }

            val requiredId = args[0]
            val rewardId = args[1]

            val requiredRole = event.jda.getRoleById(requiredId)
                    ?: throw IllegalArgumentException("Role $requiredId does not exist")
            val rewardRole = event.jda.getRoleById(rewardId)
                    ?: throw IllegalArgumentException("Role $rewardId does not exist")

            RewardsDAO.instance.changeRewardRole(event.guild.id, requiredId, rewardId, false).subscribe({
                event.reply("Removed reward **${rewardRole.name}** for **${requiredRole.name}**")
            }, {
                logger.warn("Could not remove reward role $rewardId for role $requiredId under guild ${event.guild.id}")
                event.replyError("Could not remove reward **${rewardRole.name}** for **${requiredRole.name}**")
            })
        } catch (ex: IllegalArgumentException) {
            event.replyError(ex.message)
        }
    }


}
