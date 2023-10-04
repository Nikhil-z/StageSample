/*
 * *
 *  * Copyright (C) 2023 , StagePlay  - All Rights Reserved
 *  *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Nikhil-z,  20/09/23, 12:59 pm
 *
 */

package com.stage.play.player.events

data class PlayerInfo(
    var mediaId: Int = -1,
    var elapsedTime: Long = -0L,
    var errorMsg: String = "",
    var custom: HashMap<String, String> = hashMapOf()
)
