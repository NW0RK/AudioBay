package com.audiobookbay.app.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.audiobookbay.app.domain.model.Chapter
import com.audiobookbay.app.domain.model.PlaybackState
import com.audiobookbay.app.domain.repository.AudiobookPlayer
import com.audiobookbay.app.player.service.AudioPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AudiobookPlayer] interacting with [AudioPlaybackService] via MediaController.
 */
@Singleton
class AudiobookPlayerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudiobookPlayer {

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    init {
        connectController()
    }

    private fun connectController() {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync().apply {
            addListener(
                {
                    try {
                        val controller = get()
                        mediaController = controller
                        setupListener(controller)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to connect to MediaSessionService")
                    }
                },
                MoreExecutors.directExecutor(),
            )
        }
    }

    private fun setupListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                _playbackState.value = _playbackState.value.copy(isBuffering = isBuffering)
            }
        })
    }

    override suspend fun playAudiobook(
        audiobookId: String,
        chapters: List<Chapter>,
        startChapterIndex: Int,
        startPositionMs: Long,
    ) {
        val controller = mediaController ?: return
        val mediaItems = chapters.map { ch ->
            MediaItem.Builder()
                .setMediaId(ch.id)
                .setUri(ch.filePath)
                .build()
        }
        controller.setMediaItems(mediaItems, startChapterIndex, startPositionMs)
        controller.prepare()
        controller.play()

        _playbackState.value = _playbackState.value.copy(
            currentAudiobookId = audiobookId,
            currentChapterIndex = startChapterIndex,
            currentPositionMs = startPositionMs,
            isPlaying = true,
        )
    }

    override fun play() {
        mediaController?.play()
    }

    override fun pause() {
        mediaController?.pause()
    }

    override fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    override fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    override fun skipForward(intervalMs: Long) {
        mediaController?.seekForward()
    }

    override fun rewindBackward(intervalMs: Long) {
        mediaController?.seekBack()
    }

    override fun nextChapter() {
        mediaController?.seekToNextMediaItem()
    }

    override fun previousChapter() {
        mediaController?.seekToPreviousMediaItem()
    }

    override fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
    }

    override fun setSleepTimer(durationMinutes: Int?) {
        // TODO: Implement sleep timer countdown
    }

    override fun setSleepTimerEndOfChapter() {
        // TODO: Implement pause on next media item transition
    }

    override fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
