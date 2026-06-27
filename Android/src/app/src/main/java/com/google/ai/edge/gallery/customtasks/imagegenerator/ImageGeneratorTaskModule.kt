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

import com.google.ai.edge.gallery.customtasks.common.CustomTask
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Hilt module that registers [ImageGeneratorTask] into the app's [CustomTask] set.
 *
 * The home screen discovers all [CustomTask] implementations through this set binding, so no
 * manual navigation wiring is required.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object ImageGeneratorTaskModule {
  @Provides
  @IntoSet
  fun provideImageGeneratorTask(): CustomTask {
    return ImageGeneratorTask()
  }
}
