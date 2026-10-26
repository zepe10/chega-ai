package br.com.habil.carona.domain.validation

object PasswordPolicy {
    const val MIN_LENGTH = 6

    fun validate(password: String): String? =
        if (password.length < MIN_LENGTH) "A senha precisa ter ao menos $MIN_LENGTH caracteres" else null
}
