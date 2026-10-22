package io.github.clfundev.better.occur

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun LocalDateTime.toMilli(): Long =
    this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()