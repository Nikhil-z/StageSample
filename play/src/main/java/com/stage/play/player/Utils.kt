/*
 * *
 *  * Copyright (C) 2023 , StagePlay  - All Rights Reserved
 *  *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Nikhil-z,  20/09/23, 5:37 pm
 *
 */

package com.stage.play.player

import android.util.Log


fun logs(msg: String) {
    Log.d("StagePlay-debug", msg)
}

const val DEFAULT_HEARTBEATS_INTERVAL: Long = 15000