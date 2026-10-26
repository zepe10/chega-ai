package br.com.habil.carona.data

import br.com.habil.carona.data.repository.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun sameInputProducesSameHash() {
        val salt = PasswordHasher.newSalt()
        assertEquals(PasswordHasher.hash("segredo1", salt), PasswordHasher.hash("segredo1", salt))
    }

    @Test
    fun differentSaltOrPasswordChangesHash() {
        val salt = PasswordHasher.newSalt()
        assertNotEquals(PasswordHasher.hash("segredo1", salt), PasswordHasher.hash("segredo2", salt))
        assertNotEquals(PasswordHasher.hash("segredo1", salt), PasswordHasher.hash("segredo1", PasswordHasher.newSalt()))
    }
}
