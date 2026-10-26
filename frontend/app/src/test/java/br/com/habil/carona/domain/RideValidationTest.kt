package br.com.habil.carona.domain

import br.com.habil.carona.domain.validation.validateRideForm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideValidationTest {
    private val now = 1_000_000L

    @Test
    fun validFormHasNoErrors() {
        val errors = validateRideForm("Centro", "Campus", now + 1, "3", now)
        assertFalse(errors.hasErrors)
    }

    @Test
    fun requiredFieldsAreReported() {
        val errors = validateRideForm("", " ", null, "", now)
        assertNotNull(errors.origin)
        assertNotNull(errors.destination)
        assertNotNull(errors.departure)
        assertNotNull(errors.seats)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun departureMustBeInTheFuture() {
        assertNotNull(validateRideForm("A", "B", now - 1, "2", now).departure)
    }

    @Test
    fun seatsRespectMinimumAndMaximum() {
        assertNotNull(validateRideForm("A", "B", now + 1, "0", now).seats)
        assertNotNull(validateRideForm("A", "B", now + 1, "9", now).seats)
        assertNotNull(validateRideForm("A", "B", now + 1, "1", now, minSeats = 2).seats)
        assertNull(validateRideForm("A", "B", now + 1, "2", now, minSeats = 2).seats)
    }
}
