/*
 * *
 *  * Copyright (C) 2023 , StagePlay  - All Rights Reserved
 *  *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Nikhil-z,  20/09/23, 2:45 pm
 *
 */

package com.stage.play.player

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.stage.play.player.events.OnPlayEvents
import com.stage.play.player.events.PlayEvents
import com.stage.play.player.events.PlayerInfo


open class StagePlayActivity : BasicActivity(), OnPlayEvents {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPlayEventCallback(this)
    }


    override fun playerEvents(events: PlayEvents, info: PlayerInfo) {
        logs("${events.name} \n $info")

    }

    override fun heartBeats(elapsedTime: Long) {
        logs("heartBeats $elapsedTime")

    }
}