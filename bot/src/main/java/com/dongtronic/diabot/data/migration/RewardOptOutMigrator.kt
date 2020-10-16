package com.dongtronic.diabot.data.migration

import com.dongtronic.diabot.data.mongodb.RewardOptOutsDTO
import com.dongtronic.diabot.data.mongodb.RewardsDAO
import com.dongtronic.diabot.util.logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import redis.clients.jedis.Jedis

class RewardOptOutMigrator : Migrator {
    private val redis = com.dongtronic.diabot.data.redis.RewardDAO.getInstance()
    private val mongo = RewardsDAO.instance
    private val jedis: Jedis = Jedis(System.getenv("REDIS_URL"))
    private val logger = logger()

    override fun needsMigration(): Mono<Boolean> {
        return mongo.optOuts.countDocuments().toMono().map {
            return@map it == 0L || getAllRewardOptOuts().size.toLong() > it
        }
    }

    override fun migrate(): Flux<Long> {
        return getAllRewardOptOuts().toFlux()
                .flatMap { mongo.importOptOuts(it) }
                .map { it.wasAcknowledged() }
                .onErrorContinue { t, u ->
                    logger.warn("Could not import reward opt-outs: $u", t)
                }
                .filter { it }
                .count()
                .toFlux()
    }

    /**
     * Gets all of the guilds who have users opted-out for rewards
     */
    private fun getAllRewardOptOuts(): List<RewardOptOutsDTO> {
        return jedis.keys("*:rewardoptouts")
                .map { it.substringBefore(":") }
                .toSet()
                .map { RewardOptOutsDTO(it, redis.getOptOuts(it)!!) }
    }
}