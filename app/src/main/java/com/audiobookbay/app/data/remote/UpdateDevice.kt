package com.audiobookbay.app.data.remote

import android.os.Build
import javax.inject.Inject

class UpdateDevice @Inject constructor() {
    val abis: List<String> get() = Build.SUPPORTED_ABIS.toList()
}
