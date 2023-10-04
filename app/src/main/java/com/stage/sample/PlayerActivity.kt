/*
 * *
 *  * Copyright (C) 2023 , StagePlay  - All Rights Reserved
 *  *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Nikhil-z,  20/09/23, 12:07 pm
 *
 */

package com.stage.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.stage.play.player.StagePlayActivity
import com.stage.play.player.events.PlayEvents
import com.stage.play.player.events.PlayerInfo
import com.stage.play.player.logs

class PlayerActivity : StagePlayActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }


    override fun playerEvents(events: PlayEvents, info: PlayerInfo) {
        super.playerEvents(events, info)

        when (events) {
            PlayEvents.ERROR -> {
                Toast.makeText(this, info.errorMsg, Toast.LENGTH_SHORT).show()
            }

            else -> {

            }
        }
    }

    override fun heartBeats(elapsedTime: Long) {
        super.heartBeats(elapsedTime)

    }
}