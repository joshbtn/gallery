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
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator.ImageGeneratorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AGImageGeneratorHelper"

/**
 * Helper object that wraps MediaPipe's [ImageGenerator] task.
 *
 * It manages a single [ImageGenerator] instance, created from a stable-diffusion `.task` model
 * directory placed on the device's external storage.
 */
object ImageGeneratorHelper {

  /** The active [ImageGenerator] instance, or `null` when not initialized. */
  var instance: ImageGenerator? = null
    private set

  /**
   * Initializes the [ImageGenerator] from the given model directory path.
   *
   * This runs on [Dispatchers.IO] so callers are free to invoke it from any coroutine context.
   *
   * @param context   Application context.
   * @param modelPath Path to the directory containing the `.task` model file.
   * @param onDone    Called on the IO thread when initialization completes. Pass an empty string
   *                  on success, or an error message on failure.
   */
  suspend fun initialize(context: Context, modelPath: String, onDone: (String) -> Unit) {
    withContext(Dispatchers.IO) {
      try {
        close()
        val options =
          ImageGeneratorOptions.builder().setImageGeneratorModelDirectory(modelPath).build()
        instance = ImageGenerator.createFromOptions(context, options)
        Log.d(TAG, "ImageGenerator initialized from: $modelPath")
        onDone("")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize ImageGenerator", e)
        onDone(e.message ?: "Failed to initialize image generator")
      }
    }
  }

  /** Generates an image for the given [prompt]. */
  suspend fun generate(prompt: String, iterations: Int = 20, seed: Long): Bitmap =
    withContext(Dispatchers.Default) {
      val generator =
        instance ?: throw IllegalStateException("ImageGenerator is not initialized")
      // MediaPipe execute() takes an Int seed; clamp to avoid overflow for large Long values.
      val seedInt = seed.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
      val result = runGeneration(generator = generator, prompt = prompt, iterations = iterations, seed = seedInt)
      extractBitmap(result) ?: throw IllegalStateException("No image was returned by the generator")
    }

  /** Releases the underlying [ImageGenerator] resources. */
  fun close() {
    try {
      instance?.let { generator ->
        val closeMethod =
          generator.javaClass.methods.firstOrNull { it.name == "close" && it.parameterCount == 0 }
            ?: generator.javaClass.methods.firstOrNull {
              it.name == "release" && it.parameterCount == 0
            }
            ?: generator.javaClass.methods.firstOrNull {
              it.name == "shutdown" && it.parameterCount == 0
            }
            ?: generator.javaClass.methods.firstOrNull {
              it.name == "delete" && it.parameterCount == 0
            }
        if (closeMethod != null) {
          closeMethod.invoke(generator)
        } else {
          Log.w(TAG, "No known close method found on ImageGenerator")
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error closing ImageGenerator", e)
    } finally {
      instance = null
    }
  }

  private fun runGeneration(
    generator: ImageGenerator,
    prompt: String,
    iterations: Int,
    seed: Int,
  ): Any? {
    val methods = generator.javaClass.methods

    val executeWithPrompt =
      methods.firstOrNull {
        it.name == "execute" &&
          it.parameterCount == 3 &&
          it.parameterTypes[0] == String::class.java &&
          it.parameterTypes[1] == Int::class.javaPrimitiveType &&
          it.parameterTypes[2] == Int::class.javaPrimitiveType
      }
    if (executeWithPrompt != null) {
      return executeWithPrompt.invoke(generator, prompt, iterations, seed)
    }

    val setInputs =
      methods.firstOrNull {
        it.name == "setInputs" &&
          it.parameterCount == 3 &&
          it.parameterTypes[0] == String::class.java &&
          it.parameterTypes[1] == Int::class.javaPrimitiveType &&
          it.parameterTypes[2] == Int::class.javaPrimitiveType
      }
    val executeStepped =
      methods.firstOrNull {
        it.name == "execute" &&
          it.parameterCount == 1 &&
          it.parameterTypes[0] == Boolean::class.javaPrimitiveType
      }
    if (setInputs != null && executeStepped != null) {
      setInputs.invoke(generator, prompt, iterations, seed)
      var lastResult: Any? = null
      repeat(iterations.coerceAtLeast(1)) { lastResult = executeStepped.invoke(generator, true) }
      return lastResult
    }

    val executeNoArg = methods.firstOrNull { it.name == "execute" && it.parameterCount == 0 }
    if (executeNoArg != null) {
      return executeNoArg.invoke(generator)
    }

    throw IllegalStateException("Unsupported ImageGenerator API in tasks-vision runtime")
  }

  private fun extractBitmap(result: Any?): Bitmap? {
    val generatedImageMethod =
      result?.javaClass?.methods?.firstOrNull {
        it.name == "generatedImage" && it.parameterCount == 0
      } ?: return null
    val mpImage = generatedImageMethod.invoke(result) ?: return null
    return BitmapExtractor.extract(mpImage)
  }
}
