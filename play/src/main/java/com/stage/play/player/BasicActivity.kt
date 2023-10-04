/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stage.play.player

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Pair
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ErrorMessageProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.AdsConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer.DecoderInitializationException
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil.DecoderQueryException
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ads.AdsLoader
import androidx.media3.exoplayer.util.DebugTextViewHelper
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import com.stage.play.R
import com.stage.play.player.events.OnPlayEvents
import com.stage.play.player.events.PlayEvents
import com.stage.play.player.events.PlayerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.checkerframework.checker.nullness.qual.MonotonicNonNull

/** An activity that plays media using [ExoPlayer].  */


sealed class BasicActivity : AppCompatActivity(), View.OnClickListener,
    ControllerVisibilityListener {
    private lateinit var playerView: PlayerView
    private lateinit var debugRootView: LinearLayout
    private lateinit var debugTextView: TextView
    var player: ExoPlayer? = null
    private var isShowingTrackSelectionDialog = false
    private lateinit var selectTracksButton: Button
    private var dataSourceFactory: DataSource.Factory? = null
    private var mediaItems: List<MediaItem>? = null
    private var trackSelectionParameters: TrackSelectionParameters? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var lastSeenTracks: Tracks? = null
    private var startAutoPlay = false
    private var startItemIndex = 0
    private var startPosition: Long = 0

    // For ad playback only.
    private lateinit var clientSideAdsLoader: AdsLoader

    // TODO: Annotate this and serverSideAdsLoaderState below with @OptIn when it can be applied to
    // fields (needs http://r.android.com/2004032 to be released into a version of
    // androidx.annotation:annotation-experimental).
    private var serverSideAdsLoader: ImaServerSideAdInsertionMediaSource.AdsLoader? = null
    private var serverSideAdsLoaderState: @MonotonicNonNull ImaServerSideAdInsertionMediaSource.AdsLoader.State? =
        null
    lateinit var onPlayEvents: OnPlayEvents
    val playerInfo = PlayerInfo()
    private var heartBeatJob: Job? = null

    private var _heartbeats: Boolean = false
    private var _heartbeatsIntervals: Long = DEFAULT_HEARTBEATS_INTERVAL

    // Activity lifecycle.
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataSourceFactory = DemoUtil.getDataSourceFactory( /* context= */this)
        setContentView()
        debugRootView = findViewById(R.id.controls_root)
        debugTextView = findViewById(R.id.debug_text_view)
        selectTracksButton = findViewById(R.id.select_tracks_button)
        selectTracksButton.setOnClickListener(this)
        playerView = findViewById(R.id.player_view)
        playerView.setControllerVisibilityListener(this)
        playerView.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView.requestFocus()
        if (intent.hasExtra(KEY_HEARTBEAT)) {
            _heartbeats = intent.getBooleanExtra(KEY_HEARTBEAT, false)
            _heartbeatsIntervals =
                intent.getLongExtra(KEY_HEARTBEAT_INTERVAL, DEFAULT_HEARTBEATS_INTERVAL)
        }
        if (savedInstanceState != null) {
            try {
                trackSelectionParameters = TrackSelectionParameters.fromBundle(
                    savedInstanceState.getBundle(KEY_TRACK_SELECTION_PARAMETERS)!!
                )
                startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY)
                startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX)
                startPosition = savedInstanceState.getLong(KEY_POSITION)
                _heartbeats = savedInstanceState.getBoolean(KEY_HEARTBEAT)
                _heartbeatsIntervals = savedInstanceState.getLong(KEY_HEARTBEAT_INTERVAL)
                restoreServerSideAdsLoaderState(savedInstanceState)
            } catch (e: Exception) {
                logs("savedInstanceState Exception  ${e.message}")

            }
        } else {
            trackSelectionParameters = TrackSelectionParameters.Builder( /* context= */this).build()
            clearStartPosition()
        }
    }

    internal fun setPlayEventCallback(playEvents: OnPlayEvents) {
        this.onPlayEvents = playEvents
    }

    /**Provide time in millisecond, the default will be 15 sec*/
    private fun requestHeartBeats() {
        heartBeatJob = startHeartBeat(_heartbeatsIntervals)
        heartBeatJob?.start()
    }

    private fun startHeartBeat(interval: Long): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                onPlayEvents.heartBeats(System.currentTimeMillis())
                delay(interval)
            }
        }
    }

    private fun stopHeartBeat() {
        if (heartBeatJob != null) {
            heartBeatJob?.cancel()
        } else logs("*** Heartbeat is not in operational***")
    }


    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releasePlayer()
        releaseClientSideAdsLoader()
        clearStartPosition()
        setIntent(intent)
    }

    public override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer()
            if (this::playerView.isInitialized) {
                playerView.onResume()
                onPlayEvents.playerEvents(PlayEvents.ON_RESUME)
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer()
            if (this::playerView.isInitialized) {
                playerView.onResume()
                onPlayEvents.playerEvents(PlayEvents.ON_RESUME)
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) {
            if (this::playerView.isInitialized) {
                stopHeartBeat()
                playerView.onPause()
                onPlayEvents.playerEvents(PlayEvents.ON_PAUSE)
            }
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            if (this::playerView.isInitialized) {
                stopHeartBeat()
                playerView.onPause()
                onPlayEvents.playerEvents(PlayEvents.ON_PAUSE)
            }
            releasePlayer()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        releaseClientSideAdsLoader()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty()) {
            // Empty results are triggered if a permission is requested while another request was already
            // pending and can be safely ignored in this case.
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer()
        } else {
            showToast(R.string.storage_permission_denied)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        updateTrackSelectorParameters()
        updateStartPosition()
        outState.putBundle(KEY_TRACK_SELECTION_PARAMETERS, trackSelectionParameters!!.toBundle())
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay)
        outState.putInt(KEY_ITEM_INDEX, startItemIndex)
        outState.putLong(KEY_POSITION, startPosition)
        outState.putBoolean(KEY_HEARTBEAT, _heartbeats)
        outState.putLong(KEY_HEARTBEAT_INTERVAL, _heartbeatsIntervals)
        saveServerSideAdsLoaderState(outState)
    }


    // Activity input
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // See whether the player view wants to handle media or DPAD keys events.
        return playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    // OnClickListener methods
    override fun onClick(view: View) {
        if (view === selectTracksButton && !isShowingTrackSelectionDialog
            && TrackSelectionDialog.willHaveContent(player!!)
        ) {
            isShowingTrackSelectionDialog = true
            val trackSelectionDialog = TrackSelectionDialog.createForPlayer(
                player!!
            )  /* onDismissListener= */
            { dismissedDialog: DialogInterface? -> isShowingTrackSelectionDialog = false }
            trackSelectionDialog.show(supportFragmentManager,  /* tag= */null)
        }
    }

    // PlayerView.ControllerVisibilityListener implementation
    override fun onVisibilityChanged(visibility: Int) {
        debugRootView.visibility = visibility
    }

    // Internal methods
    private fun setContentView() {
        setContentView(R.layout.player_activity)
    }

    /**
     * @return Whether initialization was successful.
     */
    private fun initializePlayer(): Boolean {
        if (player == null) {
            val intent = intent
            mediaItems = createMediaItems(intent)
            if (mediaItems!!.isEmpty()) {
                return false
            }
            lastSeenTracks = Tracks.EMPTY
            val playerBuilder = ExoPlayer.Builder( /* context= */this)
                .setMediaSourceFactory(createMediaSourceFactory())
            setRenderersFactory(
                playerBuilder,
                intent.getBooleanExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, false)
            )
            player = playerBuilder.build()
            player!!.trackSelectionParameters = trackSelectionParameters!!
            player!!.addListener(PlayerEventListener())
            player!!.addAnalyticsListener(EventLogger())
            player!!.setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */true)
            player!!.playWhenReady = startAutoPlay
            playerView.player = player
            configurePlayerWithServerSideAdsLoader()
            debugViewHelper = DebugTextViewHelper(player!!, debugTextView)
            debugViewHelper!!.start()
        }
        val haveStartPosition = startItemIndex != C.INDEX_UNSET
        if (haveStartPosition) {
            player!!.seekTo(startItemIndex, startPosition)
        }
        player!!.setMediaItems(mediaItems!!,  /* resetPosition= */!haveStartPosition)
        player!!.prepare()
        updateButtonVisibility()
        return true
    }

    @OptIn(markerClass = [UnstableApi::class]) // SSAI configuration
    private fun createMediaSourceFactory(): MediaSource.Factory {
        val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
        drmSessionManagerProvider.setDrmHttpDataSourceFactory(
            DemoUtil.getHttpDataSourceFactory( /* context= */this)
        )
        val serverSideAdLoaderBuilder =
            ImaServerSideAdInsertionMediaSource.AdsLoader.Builder( /* context= */this, playerView!!)
        if (serverSideAdsLoaderState != null) {
            serverSideAdLoaderBuilder.setAdsLoaderState(serverSideAdsLoaderState!!)
        }
        serverSideAdsLoader = serverSideAdLoaderBuilder.build()
        val imaServerSideAdInsertionMediaSourceFactory =
            ImaServerSideAdInsertionMediaSource.Factory(
                serverSideAdsLoader!!,
                DefaultMediaSourceFactory( /* context= */this)
                    .setDataSourceFactory(dataSourceFactory!!)
            )
        return DefaultMediaSourceFactory( /* context= */this)
            .setDataSourceFactory(dataSourceFactory!!)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)
            .setLocalAdInsertionComponents({ adsConfiguration: AdsConfiguration ->
                getClientSideAdsLoader(
                    adsConfiguration
                )
            },  /* adViewProvider= */playerView!!)
            .setServerSideAdInsertionMediaSourceFactory(imaServerSideAdInsertionMediaSourceFactory)
    }

    @OptIn(markerClass = [UnstableApi::class])
    private fun setRenderersFactory(
        playerBuilder: ExoPlayer.Builder, preferExtensionDecoders: Boolean
    ) {
        val renderersFactory =
            DemoUtil.buildRenderersFactory( /* context= */this, preferExtensionDecoders)
        playerBuilder.setRenderersFactory(renderersFactory)
    }

    @OptIn(markerClass = [UnstableApi::class])
    private fun configurePlayerWithServerSideAdsLoader() {
        serverSideAdsLoader!!.setPlayer(player!!)
    }

    private fun createMediaItems(intent: Intent): List<MediaItem> {
        val action = intent.action
        val actionIsListView = IntentUtil.ACTION_VIEW_LIST == action
        if (!actionIsListView && IntentUtil.ACTION_VIEW != action) {
            showToast(getString(R.string.unexpected_intent_action, action))
            finish()
            return emptyList()
        }
        val mediaItems = createMediaItems(intent, DemoUtil.getDownloadTracker( /* context= */this))
        for (i in mediaItems.indices) {
            val mediaItem = mediaItems[i]
            if (!Util.checkCleartextTrafficPermitted(mediaItem)) {
                showToast(R.string.error_cleartext_not_permitted)
                finish()
                return emptyList()
            }
            if (Util.maybeRequestReadExternalStoragePermission( /* activity= */this, mediaItem)) {
                // The player will be reinitialized if the permission is granted.
                return emptyList()
            }
            val drmConfiguration = mediaItem.localConfiguration!!.drmConfiguration
            if (drmConfiguration != null) {
                if (Build.VERSION.SDK_INT < 18) {
                    showToast(R.string.error_drm_unsupported_before_api_18)
                    finish()
                    return emptyList()
                } else if (!FrameworkMediaDrm.isCryptoSchemeSupported(drmConfiguration.scheme)) {
                    showToast(R.string.error_drm_unsupported_scheme)
                    finish()
                    return emptyList()
                }
            }
        }
        return mediaItems
    }

    private fun getClientSideAdsLoader(adsConfiguration: AdsConfiguration): AdsLoader {
        // The ads loader is reused for multiple playbacks, so that ad playback can resume.
        if (!this::clientSideAdsLoader.isInitialized) {
            clientSideAdsLoader = ImaAdsLoader.Builder( /* context= */this).build()
        }
        clientSideAdsLoader.setPlayer(player)
        return clientSideAdsLoader
    }

    private fun releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters()
            updateStartPosition()
            releaseServerSideAdsLoader()
            debugViewHelper!!.stop()
            debugViewHelper = null
            player!!.release()
            player = null
            playerView.player = null
            mediaItems = emptyList()

            onPlayEvents.playerEvents(PlayEvents.RELEASE)
        }
        if (this::clientSideAdsLoader.isInitialized) {
            clientSideAdsLoader.setPlayer(null)
        } else {
            playerView.adViewGroup.removeAllViews()
        }
    }

    @OptIn(markerClass = [UnstableApi::class])
    private fun releaseServerSideAdsLoader() {
        serverSideAdsLoaderState = serverSideAdsLoader!!.release()
        serverSideAdsLoader = null
    }

    private fun releaseClientSideAdsLoader() {
        if (this::clientSideAdsLoader.isInitialized) {
            clientSideAdsLoader.release()
            //clientSideAdsLoader = null
            playerView.adViewGroup.removeAllViews()
        }
    }

    @OptIn(markerClass = [UnstableApi::class])
    private fun saveServerSideAdsLoaderState(outState: Bundle) {
        if (serverSideAdsLoaderState != null) {
            outState.putBundle(
                KEY_SERVER_SIDE_ADS_LOADER_STATE,
                serverSideAdsLoaderState!!.toBundle()
            )
        }
    }

    @OptIn(markerClass = [UnstableApi::class])
    private fun restoreServerSideAdsLoaderState(savedInstanceState: Bundle) {
        val adsLoaderStateBundle = savedInstanceState.getBundle(KEY_SERVER_SIDE_ADS_LOADER_STATE)
        if (adsLoaderStateBundle != null) {
            serverSideAdsLoaderState =
                ImaServerSideAdInsertionMediaSource.AdsLoader.State.CREATOR.fromBundle(
                    adsLoaderStateBundle
                )
        }
    }

    private fun updateTrackSelectorParameters() {
        if (player != null) {
            trackSelectionParameters = player!!.trackSelectionParameters
        }
    }

    private fun updateStartPosition() {
        if (player != null) {
            startAutoPlay = player!!.playWhenReady
            startItemIndex = player!!.currentMediaItemIndex
            startPosition = Math.max(0, player!!.contentPosition)
        }
    }

    private fun clearStartPosition() {
        startAutoPlay = true
        startItemIndex = C.INDEX_UNSET
        startPosition = C.TIME_UNSET
    }

    // User controls
    private fun updateButtonVisibility() {
        selectTracksButton.isEnabled =
            player != null && TrackSelectionDialog.willHaveContent(player!!)
    }

    private fun showControls() {
        debugRootView.visibility = View.VISIBLE
    }

    private fun showToast(messageId: Int) {
        showToast(getString(messageId))
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
            if (playbackState == Player.STATE_ENDED) {
                onPlayEvents.playerEvents(PlayEvents.ENDED)
                showControls()
            }
            if (playbackState == Player.STATE_READY) {
                if (_heartbeats) {
                    requestHeartBeats()
                }

            }
            updateButtonVisibility()
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player!!.seekToDefaultPosition()
                player!!.prepare()
            } else {
                updateButtonVisibility()
                showControls()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateButtonVisibility()
            if (tracks === lastSeenTracks) {
                return
            }
            if (tracks.containsType(C.TRACK_TYPE_VIDEO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO,  /* allowExceedsCapabilities= */true)
            ) {
                showToast(R.string.error_unsupported_video)
            }
            if (tracks.containsType(C.TRACK_TYPE_AUDIO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO,  /* allowExceedsCapabilities= */true)
            ) {
                showToast(R.string.error_unsupported_audio)
            }
            lastSeenTracks = tracks
        }
    }

    private inner class PlayerErrorMessageProvider : ErrorMessageProvider<PlaybackException?> {
        @OptIn(markerClass = [UnstableApi::class])
        override fun getErrorMessage(e: PlaybackException): Pair<Int, String> {
            var errorString = getString(R.string.error_generic)
            val cause = e.cause
            if (cause is DecoderInitializationException) {
                // Special case for decoder initialization failures.
                errorString = if (cause.codecInfo == null) {
                    if (cause.cause is DecoderQueryException) {
                        getString(R.string.error_querying_decoders)
                    } else if (cause.secureDecoderRequired) {
                        getString(
                            R.string.error_no_secure_decoder,
                            cause.mimeType
                        )
                    } else {
                        getString(
                            R.string.error_no_decoder,
                            cause.mimeType
                        )
                    }
                } else {
                    getString(
                        R.string.error_instantiating_decoder,
                        cause.codecInfo!!.name
                    )
                }
            }
            playerInfo.errorMsg = errorString
            onPlayEvents.playerEvents(PlayEvents.ERROR, playerInfo)
            return Pair.create(0, errorString)
        }
    }

    companion object {
        // Saved instance state keys.
        private const val KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters"
        private const val KEY_SERVER_SIDE_ADS_LOADER_STATE = "server_side_ads_loader_state"
        private const val KEY_ITEM_INDEX = "item_index"
        private const val KEY_POSITION = "position"
        private const val KEY_AUTO_PLAY = "auto_play"

        const val KEY_HEARTBEAT = "heart_beat"
        const val KEY_HEARTBEAT_INTERVAL = "heart_beat_interval"

        private fun createMediaItems(
            intent: Intent,
            downloadTracker: DownloadTracker
        ): List<MediaItem> {
            val mediaItems: MutableList<MediaItem> = ArrayList()
            for (item in IntentUtil.createMediaItemsFromIntent(intent)) {
                mediaItems.add(
                    maybeSetDownloadProperties(
                        item, downloadTracker.getDownloadRequest(item.localConfiguration!!.uri)
                    )
                )
            }
            return mediaItems
        }

        @OptIn(markerClass = [UnstableApi::class])
        private fun maybeSetDownloadProperties(
            item: MediaItem, downloadRequest: DownloadRequest?
        ): MediaItem {
            if (downloadRequest == null) {
                return item
            }
            val builder = item.buildUpon()
            builder
                .setMediaId(downloadRequest.id)
                .setUri(downloadRequest.uri)
                .setCustomCacheKey(downloadRequest.customCacheKey)
                .setMimeType(downloadRequest.mimeType)
                .setStreamKeys(downloadRequest.streamKeys)
            val drmConfiguration = item.localConfiguration!!.drmConfiguration
            if (drmConfiguration != null) {
                builder.setDrmConfiguration(
                    drmConfiguration.buildUpon().setKeySetId(downloadRequest.keySetId).build()
                )
            }
            return builder.build()
        }
    }
}