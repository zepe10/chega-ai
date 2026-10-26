package br.com.habil.carona.ui.util

import br.com.habil.carona.domain.model.Affiliation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val locale = Locale("pt", "BR")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd/MM 'às' HH:mm", locale)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)

fun Long.toDateTimeText(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
        .replaceFirstChar { it.uppercase(locale) }

fun Long.toTimeText(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(timeFormatter)

fun displayName(name: String, affiliation: Affiliation): String =
    if (affiliation.isKnown) "$name · ${affiliation.label}" else name

fun String.initials(): String =
    trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
