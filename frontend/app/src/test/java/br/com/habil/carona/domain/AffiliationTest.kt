package br.com.habil.carona.domain

import br.com.habil.carona.data.mapper.affiliationOf
import br.com.habil.carona.domain.model.Affiliation
import br.com.habil.carona.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AffiliationTest {
    @Test
    fun studentLabelIncludesCourse() {
        assertEquals("Estudante de Direito", Affiliation(UserRole.STUDENT, "Direito").label)
        assertEquals("Estudante", Affiliation(UserRole.STUDENT, " ").label)
    }

    @Test
    fun professorAndStaffHaveFixedLabels() {
        assertEquals("Professor(a)", Affiliation(UserRole.PROFESSOR, "ignorado").label)
        assertEquals("Colaborador(a)", Affiliation(UserRole.STAFF).label)
    }

    @Test
    fun unknownRoleHasNoLabel() {
        val unknown = affiliationOf("", "")
        assertNull(unknown.role)
        assertFalse(unknown.isKnown)
        assertEquals("", unknown.label)
    }

    @Test
    fun rebuildsAffiliationFromStoredColumns() {
        val restored = affiliationOf("PROFESSOR", "")
        assertTrue(restored.isKnown)
        assertEquals(UserRole.PROFESSOR, restored.role)
    }
}
