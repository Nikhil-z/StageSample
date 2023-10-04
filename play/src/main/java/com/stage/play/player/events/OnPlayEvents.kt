/*
 * *
 *  * Copyright (C) 2023 , StagePlay  - All Rights Reserved
 *  *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Nikhil-z,  20/09/23, 12:57 pm
 *
 */

package com.stage.play.player.events

interface OnPlayEvents {

    fun playerEvents(events: PlayEvents, info: PlayerInfo = PlayerInfo())
    fun heartBeats(elapsedTime: Long)
}