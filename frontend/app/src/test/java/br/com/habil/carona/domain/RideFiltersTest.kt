package br.com.habil.carona.domain

import br.com.habil.carona.domain.model.Affiliation
import br.com.habil.carona.domain.model.Ride
import br.com.habil.carona.domain.model.RideFilter
import br.com.habil.carona.domain.model.RidePeriod
import br.com.habil.carona.domain.model.UserRole
import br.com.habil.carona.domain.model.applyFilter
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class RideFiltersTest {
    private val zone = ZoneOffset.UTC
    private val now = 1_700_000_000_000L
    private val hour = 3_600_000L
    private val day = 24 * hour

    private fun ride(
        id: String,
        offset: Long,
        taken: Int = 0,
        destination: String = "Campus",
        driverEmail: String = "m@faculdade.edu.br",
    ) = Ride(
        id = id,
        driverEmail = driverEmail,
        driverName = "Marcos",
        driverAffiliation = Affiliation(UserRole.STUDENT, "Direito"),
        origin = "Centro",
        destination = destination,
        route = "",
        departureAt = now + offset,
        seatsTotal = 2,
        seatsTaken = taken,
        vehicle = "",
        notes = "",
        synced = true,
    )

    @Test
    fun hidesRidesThatAlreadyDeparted() {
        val result = listOf(ride("past", -hour), ride("future", hour)).applyFilter(RideFilter(), now, zone)
        assertEquals(listOf("future"), result.map { it.id })
    }

    @Test
    fun onlyAvailableHidesFullRides() {
        val rides = listOf(ride("full", hour, taken = 2), ride("free", 2 * hour, taken = 1))
        val result = rides.applyFilter(RideFilter(onlyAvailable = true), now, zone)
        assertEquals(listOf("free"), result.map { it.id })
    }

    @Test
    fun queryMatchesAnyTextFieldIgnoringCase() {
        val rides = listOf(ride("a", hour, destination = "Campus Norte"), ride("b", 2 * hour, destination = "Shopping"))
        val result = rides.applyFilter(RideFilter(query = "norte"), now, zone)
        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun weekPeriodExcludesRidesBeyondSevenDays() {
        val rides = listOf(ride("soon", 2 * day), ride("later", 10 * day))
        val result = rides.applyFilter(RideFilter(period = RidePeriod.WEEK), now, zone)
        assertEquals(listOf("soon"), result.map { it.id })
    }

    @Test
    fun onlyContactsKeepsRidesFromContactEmails() {
        val rides = listOf(
            ride("friend", hour, driverEmail = "Amiga@faculdade.edu.br"),
            ride("stranger", 2 * hour, driverEmail = "outro@faculdade.edu.br"),
        )
        val result = rides.applyFilter(
            RideFilter(onlyContacts = true),
            now,
            zone,
            contactEmails = setOf("amiga@faculdade.edu.br"),
        )
        assertEquals(listOf("friend"), result.map { it.id })
    }

    @Test
    fun resultIsSortedByDeparture() {
        val rides = listOf(ride("b", 3 * hour), ride("a", hour))
        assertEquals(listOf("a", "b"), rides.applyFilter(RideFilter(), now, zone).map { it.id })
    }
}
