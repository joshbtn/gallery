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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

/**
 * Main screen for the Image Generator task.
 *
 * Displays a prompt text field, an iterations slider, a seed row with a randomize button, a
 * "Generate Image" button, and the resulting bitmap once it has been produced.
 */
@Composable
fun ImageGeneratorScreen(
  modelManagerViewModel: ModelManagerViewModel,
  viewModel: ImageGeneratorViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val selectedModel = modelManagerUiState.selectedModel
  val isModelReady = modelManagerUiState.isModelInitialized(model = selectedModel)

  // Derive the model directory path from the model instance (set during initializeModelFn).
  val modelPath = selectedModel.instance as? String ?: ""

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {

    // ── Prompt field ────────────────────────────────────────────────────────
    OutlinedTextField(
      value = uiState.prompt,
      onValueChange = viewModel::updatePrompt,
      label = { Text("Prompt") },
      placeholder = { Text("A cat wearing a wizard hat, photorealistic") },
      minLines = 3,
      maxLines = 6,
      modifier = Modifier.fillMaxWidth(),
      enabled = !uiState.isGenerating,
    )

    // ── Iterations slider ────────────────────────────────────────────────────
    Column {
      Text(
        text = "Diffusion steps: ${uiState.iterations}",
        style = MaterialTheme.typography.labelMedium,
      )
      Slider(
        value = uiState.iterations.toFloat(),
        onValueChange = { viewModel.updateIterations(it.toInt()) },
        valueRange = 1f..50f,
        steps = 48, // 50 - 1 - 1 = 48 discrete steps
        enabled = !uiState.isGenerating,
      )
    }

    // ── Seed row ─────────────────────────────────────────────────────────────
    Column {
      Text(text = "Seed", style = MaterialTheme.typography.labelMedium)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        var seedText by remember(uiState.seed) { mutableStateOf(uiState.seed.toString()) }
        OutlinedTextField(
          value = seedText,
          onValueChange = { text ->
            seedText = text
            text.toLongOrNull()?.let { viewModel.updateSeed(it) }
          },
          label = { Text("Seed value") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1f),
          enabled = !uiState.isGenerating,
        )
        OutlinedButton(
          onClick = viewModel::randomizeSeed,
          enabled = !uiState.isGenerating,
        ) {
          Text("🎲 Randomize")
        }
      }
    }

    // ── Generate button ──────────────────────────────────────────────────────
    Button(
      onClick = { viewModel.generate(context = context, modelPath = modelPath) },
      enabled = isModelReady && !uiState.isGenerating && uiState.prompt.isNotBlank(),
      modifier = Modifier.fillMaxWidth(),
    ) {
      if (uiState.isGenerating) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.onPrimary,
        )
      } else {
        Text("Generate Image")
      }
    }

    // ── Error message ────────────────────────────────────────────────────────
    uiState.errorMessage?.let { error ->
      Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
      )
    }

    // ── Generated image ──────────────────────────────────────────────────────
    uiState.generatedBitmap?.let { bitmap ->
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = "Generated image",
          modifier = Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height),
        )
      }
    }

    // ── Model not ready placeholder ──────────────────────────────────────────
    if (!isModelReady && !uiState.isGenerating) {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
          text = "Waiting for model to initialize…",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
