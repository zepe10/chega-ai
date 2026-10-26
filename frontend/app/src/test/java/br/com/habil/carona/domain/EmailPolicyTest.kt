package br.com.habil.carona.domain

import br.com.habil.carona.domain.validation.EmailPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailPolicyTest {
    @Test
    fun acceptsAcademicDomainsAndSubdomains() {
        assertTrue(EmailPolicy.isInstitutional("aluno@faculdade.edu.br"))
        assertTrue(EmailPolicy.isInstitutional("aluno@aluno.faculdade.edu.br"))
        assertTrue(EmailPolicy.isInstitutional("colaborador@habil.com.br"))
    }

    @Test
    fun rejectsPersonalDomainsAndLookalikes() {
        assertFalse(EmailPolicy.isInstitutional("fulano@gmail.com"))
        assertFalse(EmailPolicy.isInstitutional("fulano@fakeedu.br"))
        assertFalse(EmailPolicy.isInstitutional("fulano@edu.br.malicioso.com"))
    }

    @Test
    fun normalizesCaseAndWhitespace() {
        assertEquals("aluno@faculdade.edu.br", EmailPolicy.normalize("  Aluno@Faculdade.EDU.br "))
        assertNull(EmailPolicy.validate(" ALUNO@faculdade.edu.br "))
    }

    @Test
    fun validateReturnsFriendlyMessages() {
        assertNotNull(EmailPolicy.validate(""))
        assertEquals("E-mail inválido", EmailPolicy.validate("sem-arroba"))
        assertNotNull(EmailPolicy.validate("fulano@gmail.com"))
    }
}
