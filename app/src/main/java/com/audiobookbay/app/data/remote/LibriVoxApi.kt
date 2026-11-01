package com.audiobookbay.app.data.remote

import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Verified REST API contract for LibriVox (public domain audiobooks).
 * Free to use, no API key or Cloudflare anti-bot restrictions required.
 * Base URL: https://librivox.org/api/feed/
 */
interface LibriVoxApi {

    @GET("audiobooks")
    suspend fun getAudiobook(
        @Query("id") id: String,
        @Query("format") format: String = "json",
        @Query("extended") extended: Int = 1,
        @Query("coverart") coverArt: Int = 1,
    ): LibriVoxResponse

    @GET("audiobooks")
    suspend fun getAudiobooks(
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("extended") extended: Int = 1,
        @Query("coverart") coverArt: Int = 1,
        @Query("sort_order") sortOrder: String = "asc",
    ): LibriVoxResponse

    @GET("audiobooks")
    suspend fun searchAudiobooks(
        @Query("title") title: String? = null,
        @Query("author") author: String? = null,
        @Query("genre") genre: String? = null,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("extended") extended: Int = 1,
        @Query("coverart") coverArt: Int = 1,
        @Query("sort_order") sortOrder: String = "asc",
    ): LibriVoxResponse
}

@Serializable
data class LibriVoxResponse(
    val books: List<LibriVoxBookDto> = emptyList(),
)

@Serializable
data class LibriVoxBookDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("url_zip_file") val urlZipFile: String? = null,
    @SerialName("url_project") val urlProject: String? = null,
    @SerialName("url_librivox") val urlLibriVox: String? = null,
    @SerialName("url_other") val urlOther: JsonElement? = null,
    @SerialName("coverart_jpg") val coverArt: String? = null,
    @SerialName("coverart_thumbnail") val coverThumbnail: String? = null,
    @SerialName("totaltime") val totalTime: String? = null,
    @SerialName("totaltimesecs") val totalTimeSecs: Long? = null,
    val authors: List<LibriVoxAuthorDto> = emptyList(),
    val sections: List<LibriVoxSectionDto> = emptyList(),
)

@Serializable
data class LibriVoxAuthorDto(
    val id: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val dob: String? = null,
    val dod: String? = null,
)

@Serializable
data class LibriVoxSectionDto(
    val id: String,
    @SerialName("section_number") val sectionNumber: String? = null,
    val title: String? = null,
    @SerialName("listen_url") val listenUrl: String? = null,
    val language: String? = null,
    @SerialName("playtime") val playTime: String? = null,
)

internal fun LibriVoxBookDto.toAudiobook() = Audiobook(
    id = "librivox:$id",
    title = org.jsoup.Jsoup.parse(title).text(),
    author = authors.joinToString(", ") { listOfNotNull(it.firstName, it.lastName).joinToString(" ").trim() }
        .ifBlank { "Unknown" },
    coverImageUrl = coverArt ?: coverThumbnail,
    detailUrl = urlLibriVox,
    durationSeconds = totalTimeSecs,
    source = AudiobookSource.LIBRIVOX,
)

internal fun LibriVoxBookDto.toDetail(): com.audiobookbay.app.domain.model.AudiobookDetail {
    val book = toAudiobook()
    return com.audiobookbay.app.domain.model.AudiobookDetail(
        id = book.id, title = book.title, author = book.author,
        description = org.jsoup.Jsoup.parse(description.orEmpty()).text(),
        coverImageUrl = book.coverImageUrl, durationSeconds = totalTimeSecs,
        source = AudiobookSource.LIBRIVOX,
        chapters = sections.mapIndexed { index, section ->
            com.audiobookbay.app.domain.model.Chapter(
                id = "${book.id}:${section.id}", audiobookId = book.id,
                title = section.title ?: "${index + 1}",
                trackNumber = section.sectionNumber?.toIntOrNull() ?: index + 1,
                durationMs = ((section.playTime?.toDoubleOrNull() ?: 0.0) * 1000).toLong(),
                filePath = section.listenUrl,
            )
        }.sortedBy { it.trackNumber },
    )
}
