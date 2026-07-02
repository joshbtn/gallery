/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.common

import android.content.res.Resources
import android.util.Log
import com.google.ai.edge.gallery.R
import java.util.concurrent.TimeUnit

fun readGalleryLogcat(resources: Resources, tag: String): String {
  return try {
    val process =
      Runtime.getRuntime().exec(
        arrayOf("logcat", "-d", "-t", "200", "--pid=${android.os.Process.myPid()}")
      )
    try {
      if (!process.waitFor(3, TimeUnit.SECONDS)) {
        return resources.getString(R.string.mobile_actions_logcat_unavailable)
      }
      val output = process.inputStream.bufferedReader().use { it.readText().trim() }
      if (output.isNotEmpty()) output else resources.getString(R.string.mobile_actions_logcat_empty)
    } finally {
      process.destroy()
    }
  } catch (e: Exception) {
    Log.w(tag, "Unable to read logcat output.", e)
    resources.getString(R.string.mobile_actions_logcat_unavailable)
  }
}
