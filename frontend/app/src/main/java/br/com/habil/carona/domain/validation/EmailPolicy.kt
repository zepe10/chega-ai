package br.com.habil.carona.domain.validation

object EmailPolicy {
    val allowedDomains: List<String> = listOf("edu.br", "habil.com.br")

    private val formatRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun normalize(email: String): String = email.trim().lowercase()

    fun hasValidFormat(email: String): Boolean = formatRegex.matches(normalize(email))

    fun isInstitutional(email: String): Boolean {
        val domain = normalize(email).substringAfter('@', missingDelimiterValue = "")
        return domain.isNotEmpty() && allowedDomains.any { domain == it || domain.endsWith(".$it") }
    }

    fun validate(email: String): String? = when {
        email.isBlank() -> "Informe o e-mail institucional"
        !hasValidFormat(email) -> "E-mail inválido"
        !isInstitutional(email) -> "Use seu e-mail institucional (@...edu.br)"
        else -> null
    }
}
