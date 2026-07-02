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

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the Image Generator screen. */
data class ImageGeneratorUiState(
  val prompt: String = "",
  val iterations: Int = 20,
  val seed: Long = 42L,
  val isGenerating: Boolean = false,
  val generatedBitmap: Bitmap? = null,
  val errorMessage: String? = null,
)

/** ViewModel that drives the Image Generator screen. */
@HiltViewModel
class ImageGeneratorViewModel @Inject constructor() : ViewModel() {

  private companion object {
    const val MIN_ITERATIONS = 1
    const val MAX_ITERATIONS = 50
  }

  private val _uiState = MutableStateFlow(ImageGeneratorUiState())
  val uiState: StateFlow<ImageGeneratorUiState> = _uiState.asStateFlow()

  fun updatePrompt(prompt: String) {
    _uiState.update { it.copy(prompt = prompt, errorMessage = null) }
  }

  fun updateIterations(iterations: Int) {
    _uiState.update { it.copy(iterations = iterations.coerceIn(MIN_ITERATIONS, MAX_ITERATIONS)) }
  }

  fun updateSeed(seed: Long) {
    _uiState.update { it.copy(seed = seed) }
  }

  fun randomizeSeed() {
    _uiState.update { it.copy(seed = Random.nextLong(0L, Long.MAX_VALUE)) }
  }

  /**
   * Runs image generation on [ImageGeneratorHelper].
   *
   * The blocking [ImageGeneratorHelper.generate] call is dispatched to [kotlinx.coroutines.Dispatchers.Default]
   * internally, so this function is safe to call from the UI.
   */
  fun generate() {
    val state = _uiState.value
    if (state.prompt.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please enter a prompt") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isGenerating = true, errorMessage = null, generatedBitmap = null) }
      try {
        val bitmap =
          ImageGeneratorHelper.generate(
            prompt = state.prompt,
            iterations = state.iterations,
            seed = state.seed,
          )
        _uiState.update { it.copy(isGenerating = false, generatedBitmap = bitmap) }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isGenerating = false,
            errorMessage = e.message ?: "Image generation failed. Please check model compatibility and try again.",
          )
        }
      }
    }
  }
}
