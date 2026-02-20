package com.overlandtracker.app.domain.navigation

data class PingDecision(
    val intervalMillis: Long,
    val nextPingAtMillis: Long
)

class PingPolicy(
    private val baseIntervalMillis: Long = 5 * 60_000L,
    private val mediumIntervalMillis: Long = 2 * 60_000L,
    private val nearIntervalMillis: Long = 60_000L,
    private val movingAwayMultiplier: Double = 1.5,
    private val nearDistanceMeters: Double = 400.0,
    private val mediumDistanceMeters: Double = 1_200.0,
    private val nearEtaMillis: Long = 10 * 60_000L,
    private val mediumEtaMillis: Long = 30 * 60_000L
) {
    fun evaluate(
        nowMillis: Long,
        distanceToTargetMeters: Double?,
        etaMillis: Long?,
        movingAwayFromTarget: Boolean,
        adaptiveEnabled: Boolean
    ): PingDecision {
        val interval = if (!adaptiveEnabled) {
            baseIntervalMillis
        } else {
            computeAdaptiveInterval(distanceToTargetMeters, etaMillis, movingAwayFromTarget)
        }

        return PingDecision(
            intervalMillis = interval,
            nextPingAtMillis = nowMillis + interval
        )
    }

    private fun computeAdaptiveInterval(
        distanceToTargetMeters: Double?,
        etaMillis: Long?,
        movingAwayFromTarget: Boolean
    ): Long {
        var interval = baseIntervalMillis

        val nearByDistance = distanceToTargetMeters != null && distanceToTargetMeters <= nearDistanceMeters
        val mediumByDistance = distanceToTargetMeters != null && distanceToTargetMeters <= mediumDistanceMeters
        val nearByEta = etaMillis != null && etaMillis <= nearEtaMillis
        val mediumByEta = etaMillis != null && etaMillis <= mediumEtaMillis

        interval = when {
            nearByDistance || nearByEta -> nearIntervalMillis
            mediumByDistance || mediumByEta -> mediumIntervalMillis
            else -> baseIntervalMillis
        }

        if (movingAwayFromTarget) {
            interval = (interval * movingAwayMultiplier).toLong()
        }

        return interval
    }
}
