package br.com.habil.carona.domain.model

import java.time.Instant
import java.time.ZoneId

fun List<Ride>.applyFilter(
    criteria: RideFilter,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    contactEmails: Set<String> = emptySet(),
): List<Ride> {
    val query = criteria.query.trim().lowercase()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return filter { it.departureAt >= now }
        .filter { !criteria.onlyAvailable || !it.isFull }
        .filter { !criteria.onlyContacts || it.driverEmail.lowercase() in contactEmails }
        .filter { ride ->
            query.isEmpty() || listOf(ride.origin, ride.destination, ride.route, ride.driverName)
                .any { it.lowercase().contains(query) }
        }
        .filter { ride ->
            val day = Instant.ofEpochMilli(ride.departureAt).atZone(zone).toLocalDate()
            when (criteria.period) {
                RidePeriod.ANY -> true
                RidePeriod.TODAY -> day == today
                RidePeriod.WEEK -> !day.isAfter(today.plusDays(7))
            }
        }
        .sortedBy { it.departureAt }
}
