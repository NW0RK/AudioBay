package com.audiobookbay.app.data.remote

import com.audiobookbay.app.domain.repository.AppUpdate
import kotlinx.serialization.json.*

internal data class ReleaseVersion(val major: Long, val minor: Long, val patch: Long, val pre: List<String>) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int {
        for ((a, b) in listOf(major to other.major, minor to other.minor, patch to other.patch)) {
            a.compareTo(b).takeIf { it != 0 }?.let { return it }
        }
        if (pre.isEmpty() || other.pre.isEmpty()) return when {
            pre == other.pre -> 0
            pre.isEmpty() -> 1
            else -> -1
        }
        for (i in 0 until minOf(pre.size, other.pre.size)) {
            val a = pre[i]; val b = other.pre[i]
            val numericA = a.all(Char::isDigit); val numericB = b.all(Char::isDigit)
            val result = when {
                numericA && numericB -> a.length.compareTo(b.length).takeIf { it != 0 } ?: a.compareTo(b)
                numericA -> -1
                numericB -> 1
                else -> a.compareTo(b)
            }
            if (result != 0) return result
        }
        return pre.size.compareTo(other.pre.size)
    }

    companion object {
        fun parse(value: String): ReleaseVersion? {
            val match = Regex("^[vV]?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$").matchEntire(value) ?: return null
            val pre = match.groupValues[4].takeIf { it.isNotEmpty() }?.split('.') ?: emptyList()
            if (pre.any { it.isEmpty() || it.all(Char::isDigit) && it.length > 1 && it.startsWith('0') }) return null
            return ReleaseVersion(match.groupValues[1].toLongOrNull() ?: return null,
                match.groupValues[2].toLongOrNull() ?: return null,
                match.groupValues[3].toLongOrNull() ?: return null, pre)
        }
    }
}

internal object UpdateReleaseParser {
    const val REPOSITORY = "NW0RK/AudioBay"
    const val MAX_APK_BYTES = 100L * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true }

    fun select(body: String, installed: String, abis: List<String>): AppUpdate? {
        val current = requireNotNull(ReleaseVersion.parse(installed))
        if ("arm64-v8a" !in abis) return null
        return json.parseToJsonElement(body).jsonArray.mapNotNull { element ->
            runCatching {
                val release = element.jsonObject
                if (release["draft"]?.jsonPrimitive?.booleanOrNull != false) return@runCatching null
                val tag = release.getValue("tag_name").jsonPrimitive.content
                val version = ReleaseVersion.parse(tag) ?: return@runCatching null
                if (version <= current) return@runCatching null
                val name = tag.removePrefix("v").removePrefix("V")
                val assets = release.getValue("assets").jsonArray.map { it.jsonObject }
                val apk = assets.singleOrNull {
                    it["name"]?.jsonPrimitive?.content in setOf("AudioBay-$name.apk", "AudioBay-$name-preview.apk") &&
                        it["state"]?.jsonPrimitive?.content == "uploaded"
                } ?: return@runCatching null
                val checksum = assets.singleOrNull { it["name"]?.jsonPrimitive?.content == "SHA256SUMS.txt" &&
                    it["state"]?.jsonPrimitive?.content == "uploaded" } ?: return@runCatching null
                val size = apk.getValue("size").jsonPrimitive.long
                if (size !in 1..MAX_APK_BYTES) return@runCatching null
                val apkUrl = apk.getValue("browser_download_url").jsonPrimitive.content
                val checksumUrl = checksum.getValue("browser_download_url").jsonPrimitive.content
                if (!validAssetUrl(apkUrl) || !validAssetUrl(checksumUrl)) return@runCatching null
                version to AppUpdate(name, release["body"]?.jsonPrimitive?.contentOrNull.orEmpty().take(20_000),
                    apk.getValue("name").jsonPrimitive.content, apkUrl, checksumUrl, size)
            }.getOrNull()
        }.maxByOrNull { it.first }?.second
    }

    fun validAssetUrl(value: String): Boolean =
        value.startsWith("https://github.com/$REPOSITORY/releases/download/") &&
            value.none { it.isWhitespace() || it.isISOControl() } && !value.contains("/../")

    fun checksum(body: String, assetName: String): String {
        val matches = body.lineSequence().mapNotNull { line ->
            Regex("^([0-9a-fA-F]{64})[ \\t]+\\*?(.+)$").matchEntire(line.trim())?.let {
                if (it.groupValues[2] == assetName) it.groupValues[1].lowercase() else null
            }
        }.toList()
        require(matches.size == 1) { "Missing or ambiguous checksum" }
        return matches.single()
    }

    fun shouldCheck(now: Long, previous: Long): Boolean = previous <= 0 || now < previous || now - previous >= 86_400_000
}
