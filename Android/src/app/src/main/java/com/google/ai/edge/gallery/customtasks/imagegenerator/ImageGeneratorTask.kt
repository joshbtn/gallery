/*
 * Copyright 2025 Google LLC
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

package com.google.ai.edge.gallery.customtasks.imagegenerator

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.litertlm.Contents
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Custom task that adds an on-device Image Generator powered by MediaPipe's
 * [com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator].
 *
 * Place your stable-diffusion `.task` model file in the device's external files directory at:
 *   /storage/emulated/0/Android/data/com.google.aiedge.gallery/files/image_generator/
 *
 * The task appears on the home screen under the LLM category alongside Chat, Prompt Lab, etc.
 */
class ImageGeneratorTask @Inject constructor() : CustomTask {

  override val task: Task =
    Task(
      id = "image_generator",
      label = "Image Generator",
      category = Category.LLM,
      icon = Icons.Outlined.AutoAwesome,
      description =
        "Generate images on-device from text prompts using a stable-diffusion model " +
          "powered by MediaPipe. Place a compatible `.task` model file in the " +
          "`image_generator/` directory on your device's external storage.",
      shortDescription = "Text-to-image on device",
      models =
        mutableListOf(
          Model(
            name = "SD-TFLite",
            info =
              "Expects a stable-diffusion `.task` model file manually pushed to " +
                "`{ext_files_dir}/image_generator/`. See the task description for details.",
            localFileRelativeDirPathOverride = "image_generator/",
          )
        ),
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    systemInstruction: Contents?,
    onDone: (String) -> Unit,
  ) {
    model.instance = null

    // Derive the model directory from the localFileRelativeDirPathOverride.
    val modelDir =
      File(context.getExternalFilesDir(null), model.localFileRelativeDirPathOverride.trimEnd('/'))
        .absolutePath

    coroutineScope.launch {
      ImageGeneratorHelper.initialize(context = context, modelPath = modelDir) { error ->
        model.instance = if (error.isEmpty()) ImageGeneratorHelper.instance else null
        onDone(error)
      }
    }
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    ImageGeneratorHelper.close()
    model.instance = null
    onDone()
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskData
    ImageGeneratorScreen(modelManagerViewModel = myData.modelManagerViewModel)
  }
}
