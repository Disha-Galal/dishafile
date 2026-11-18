package com.disha.filekit.core

import java.io.IOException
import java.net.URI
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// تمر الطلبات بالترتيب المحدد، ولا تدخل السحابة إلا باختيار صريح لكل طلب.
object AiFallback {
    data class Target(val model: String, val cloud: Boolean = false)
    enum class Reason { CONNECTION, TIMEOUT, AUTH, QUOTA, MODEL_NOT_FOUND, SERVER, RUNTIME, MEMORY, INCOMPLETE, BLOCKED, EMPTY, INVALID, UNKNOWN }
    data class Failure(val model: String, val reason: Reason)
    data class Result(val model: String, val text: String, val failures: List<String>, val details: List<Failure> = emptyList())
    class AttemptFailed(val reason: Reason) : IOException(reason.name)
    class Unavailable(val failures: List<String>, val details: List<Failure> = emptyList()) : IOException("AI unavailable")

    fun reason(error: Exception): Reason = when (error) {
        is AttemptFailed -> error.reason
        is SocketTimeoutException -> Reason.TIMEOUT
        is ConnectException, is UnknownHostException -> Reason.CONNECTION
        else -> Reason.UNKNOWN
    }

    fun targets(primary: String, secondary: String, gemini: String, provider: String, allowCloud: Boolean, hasKey: Boolean): List<Target> {
        require(provider in setOf("ollama", "gemini"))
        if (provider == "gemini") {
            require(allowCloud && hasKey)
            return listOf(Target(gemini, true))
        }
        return listOf(primary, secondary).filter { it.isNotBlank() }.map { Target(it) }.distinct() +
            if (allowCloud && hasKey) listOf(Target(gemini, true)) else emptyList()
    }

    fun localEndpoint(value: String): URI {
        val uri = URI(value.trim().trimEnd('/'))
        require(uri.scheme == "http" && uri.host in setOf("127.0.0.1", "localhost", "[::1]", "::1"))
        require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null && uri.path.isNullOrEmpty())
        require(uri.port == -1 || uri.port in 1..65535)
        return uri
    }

    fun generate(targets: List<Target>, allowCloud: Boolean, attempt: (Target) -> String): Result {
        val failures = mutableListOf<String>()
        val details = mutableListOf<Failure>()
        for (target in targets.distinct()) {
            if (target.cloud && !allowCloud) continue
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            try {
                val text = attempt(target)
                if (text.isBlank()) throw AttemptFailed(Reason.EMPTY)
                return Result(target.model, text, failures.toList(), details.toList())
            } catch (e: InterruptedException) { throw e }
            catch (error: Exception) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                failures.add(target.model)
                details.add(Failure(target.model, reason(error)))
            }
        }
        throw Unavailable(failures, details)
    }
}
