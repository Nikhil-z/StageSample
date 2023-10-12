/*
 * *
 *  * Copyright (C) 2023 , StagePlay  - All Rights Reserved
 *  *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Nikhil-z,  13/09/23, 12:19 pm
 *
 */

package com.stage.play

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Util
import androidx.media3.ui.DefaultTimeBar
import com.stage.play.player.IntentUtil
import com.stage.play.player.BasicActivity
import com.stage.play.player.BasicActivity.Companion.KEY_HEARTBEAT
import com.stage.play.player.BasicActivity.Companion.KEY_HEARTBEAT_INTERVAL
import com.stage.play.player.DEFAULT_HEARTBEATS_INTERVAL
import com.stage.play.player.StagePlayActivity
import com.stage.play.player.logs
import java.util.UUID

object StagePlay {

    private const val identifier = "StagePlay"

    private var _mediaId: String = ""
    private var _mediaUrl: String = ""
    private lateinit var _drmScheme: UUID
    private var _drmLicenseUrl: String = ""

    private var _lastWatched: Long = 0

    private var _title: String = ""
    private var _disc: String = ""

    private var _heartBeats: Boolean = false
    private var _heartBeatsInterval: Long = DEFAULT_HEARTBEATS_INTERVAL

    open class Prepare(
        private val context: Context,
        private var customPlayerClass: Class<*>? = StagePlayActivity::class.java
    ) {


        fun setMediaId(mediaId: String): Prepare {
            _mediaId = mediaId
            return this
        }

        fun setUrl(mediaUrl: String): Prepare {
            _mediaUrl = mediaUrl
            return this
        }

        fun setDrmLicenseUrl(drmLicenseUrl: String): Prepare {
            _drmLicenseUrl = drmLicenseUrl
            return this
        }

        fun continueWatching(lastWatched: Long): Prepare {
            _lastWatched = lastWatched
            return this
        }

        /**
         * Heartbeat provides elapsed time update in a certain
         * interval. Default will be 15 sec
         * */
        fun enableHeartbeats(interval: Long = DEFAULT_HEARTBEATS_INTERVAL): Prepare {
            if (customPlayerClass == StagePlayActivity::class.java) {
                logs("Heartbeat function not available, its only works with custom Player Activity ")
            } else {
                _heartBeats = true
                _heartBeatsInterval = interval
            }
            return this
        }

        fun setMetaData(title: String = "", disc: String = ""): Prepare {
            _title = title
            _disc = disc
            return this
        }


        /**
         * Supported schemes are
         *  widevine | playready | clearkey
         * */
        fun setDrmScheme(drmScheme: String): Prepare {
            _drmScheme = Util.getDrmUuid(drmScheme)!!
            return this
        }


        fun startPlayer() {
            if (!checkList()) return

            val intent = Intent(context, customPlayerClass)
            intent.putExtra(KEY_HEARTBEAT, _heartBeats)
            intent.putExtra(KEY_HEARTBEAT_INTERVAL, _heartBeatsInterval)
            val mediaItem = MediaItem
                .Builder()
                .setMediaId(_mediaId)
                .setUri(_mediaUrl)
                .setDrmConfiguration(
                    MediaItem.DrmConfiguration
                        .Builder(
                            _drmScheme
                        )
                        .setLicenseUri(_drmLicenseUrl)
                        .build()
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setDisplayTitle(_title)
                        .setDescription(_disc)
                        .build()
                )
                .build()
            val mediaList: ArrayList<MediaItem> = ArrayList()
            mediaList.add(mediaItem)
            IntentUtil.addToIntent(mediaList, intent)
            context.startActivity(intent)
        }


        private fun checkList(): Boolean {
            if (_mediaUrl == "") {
                Log.w(identifier, "You must provide a valid media url")
                Toast.makeText(context, "E400, You may have a broken media", Toast.LENGTH_SHORT)
                    .show()

                return false
            }

            return true
        }

        inline fun <reified T : Any> expectClass(value: T) {
            if (value !is StagePlayActivity) {
                throw IllegalArgumentException("Expected instance of PlayerActivity, but received ${value::class.simpleName}")
            }
        }
    }



}