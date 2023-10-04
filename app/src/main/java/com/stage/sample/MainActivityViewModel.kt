/*
 * *
 *  * Copyright (C) 2023 , StagePlay  - All Rights Reserved
 *  *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Nikhil-z,  14/09/23, 12:24 pm
 *
 */

package com.stage.sample

import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import java.util.UUID

class MainActivityViewModel : ViewModel() {

    val drm_widewine_uuid: UUID = C.WIDEVINE_UUID

    init {

    }
}