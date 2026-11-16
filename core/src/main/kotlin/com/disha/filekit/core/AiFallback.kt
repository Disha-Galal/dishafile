package com.disha.filekit.core

import java.io.IOException
import java.net.URI

// تمر الطلبات بالترتيب المحدد، ولا تدخل السحابة إلا باختيار صريح لكل طلب.
object AiFallback {
    data class Target(val model: String, val cloud: Boolean = false)
    data class Result(val model: String, val text: String, val failures: List<String>)
    class Unavailable(val failures: List<String>) : IOException("AI unavailable")

    fun localEndpoint(value: String): URI {
        val uri = URI(value.trim().trimEnd('/'))
        require(uri.scheme == "http" && uri.host in setOf("127.0.0.1", "localhost", "[::1]", "::1"))
        require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null && uri.path.isNullOrEmpty())
        require(uri.port == -1 || uri.port in 1..65535)
        return uri
    }

    fun generate(targets: List<Target>, allowCloud: Boolean, attempt: (Target) -> String): Result {
        val failures = mutableListOf<String>()
        for (target in targets.distinct()) {
            if (target.cloud && !allowCloud) continue
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            try {
                val text = attempt(target)
                if (text.isBlank()) throw IOException("Empty response")
                return Result(target.model, text, failures.toList())
            } catch (e: InterruptedException) { throw e }
            catch (_: Exception) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                failures.add(target.model)
            }
        }
        throw Unavailable(failures)
    }
}
