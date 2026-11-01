package com.audiobookbay.app.data.remote

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
    suspend fun getAudiobooks(
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("extended") extended: Int = 1,
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
    @SerialName("url_other") val urlOther: String? = null,
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
