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

package com.google.ai.edge.gallery.ui.modelconverter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A card that displays an indeterminate or animated progress bar together with a status label.
 *
 * @param label   Short human-readable description of the current operation.
 * @param progress  0f–1f for determinate progress, or null for indeterminate.
 */
@Composable
fun ConversionProgressCard(
  label: String,
  progress: Float? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (progress == null) {
      // Indeterminate.
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
      // Determinate with smooth animation.
      val animatedProgress = remember { Animatable(0f) }
      LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, animationSpec = tween(150))
      }
      LinearProgressIndicator(
        progress = { animatedProgress.value },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}
