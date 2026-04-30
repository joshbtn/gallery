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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AGModelConverterVM"

/** Quantization formats supported for conversion. */
enum class QuantizationFormat(val label: String, val flag: String) {
  INT4("int4", "int4"),
  INT8("int8", "int8"),
  FLOAT16("float16", "float16"),
}

/** Tabs in the converter screen. */
enum class ConverterTab { HUGGING_FACE, LOCAL_FILE }

/** State for the HuggingFace search flow. */
sealed class HfSearchState {
  object Idle : HfSearchState()
  object Searching : HfSearchState()
  data class Found(val repoId: String, val fileName: String, val downloadUrl: String) :
    HfSearchState()
  object NotFound : HfSearchState()
  data class Error(val message: String) : HfSearchState()
}

data class ModelConverterUiState(
  val selectedTab: ConverterTab = ConverterTab.HUGGING_FACE,
  val hfRepoId: String = "",
  val quantization: QuantizationFormat = QuantizationFormat.INT4,
  val hfSearchState: HfSearchState = HfSearchState.Idle,
  val localFilePath: String = "",
  val localFileName: String = "",
)

/**
 * ViewModel for the Model Converter screen.
 *
 * Searches the `litert-community` HuggingFace organisation for a pre-converted `.task` file
 * matching the requested model and quantization.  If a match is found the user can download it
 * directly into the app's import directory; otherwise the screen shows the `ai-edge-torch` CLI
 * command required to convert the model on a PC.
 */
class ModelConverterViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(ModelConverterUiState())
  val uiState = _uiState.asStateFlow()

  fun selectTab(tab: ConverterTab) {
    _uiState.update { it.copy(selectedTab = tab) }
  }

  fun updateHfRepoId(repoId: String) {
    _uiState.update { it.copy(hfRepoId = repoId, hfSearchState = HfSearchState.Idle) }
  }

  fun updateQuantization(quantization: QuantizationFormat) {
    _uiState.update { it.copy(quantization = quantization, hfSearchState = HfSearchState.Idle) }
  }

  fun setLocalFile(path: String, name: String) {
    _uiState.update { it.copy(localFilePath = path, localFileName = name) }
  }

  /**
   * Searches the `litert-community` HuggingFace org for a pre-converted version of the requested
   * model.  Looks for a repo named after the model's short name in the `litert-community`
   * namespace, then checks whether a file matching the quantization format exists.
   */
  fun searchLiteRTCommunity() {
    val repoId = _uiState.value.hfRepoId.trim()
    val quant = _uiState.value.quantization
    if (repoId.isBlank()) return

    _uiState.update { it.copy(hfSearchState = HfSearchState.Searching) }

    viewModelScope.launch(Dispatchers.IO) {
      try {
        // The litert-community mirror repo is usually named after the original model's short name.
        val modelShortName = repoId.substringAfterLast("/")
        val communityRepoId = "litert-community/$modelShortName"
        val apiUrl =
          "https://huggingface.co/api/models/$communityRepoId?full=true&blobs=false"
        Log.d(TAG, "Querying HF API: $apiUrl")

        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          setRequestProperty("Accept", "application/json")
          connectTimeout = 10_000
          readTimeout = 10_000
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
          val body = connection.inputStream.bufferedReader().readText()
          // Parse the HuggingFace API response to find a matching .task or .litertlm file.
          val quantToken = quant.flag
          val matchedFile = findMatchingFile(body, quantToken)
          if (matchedFile != null) {
            val downloadUrl =
              "https://huggingface.co/$communityRepoId/resolve/main/$matchedFile?download=true"
            Log.d(TAG, "Found pre-converted model: $matchedFile")
            _uiState.update {
              it.copy(
                hfSearchState =
                  HfSearchState.Found(
                    repoId = communityRepoId,
                    fileName = matchedFile,
                    downloadUrl = downloadUrl,
                  )
              )
            }
          } else {
            Log.d(TAG, "No matching file found for quantization $quantToken in $communityRepoId")
            _uiState.update { it.copy(hfSearchState = HfSearchState.NotFound) }
          }
        } else {
          Log.d(TAG, "Repo $communityRepoId not found (HTTP $responseCode)")
          _uiState.update { it.copy(hfSearchState = HfSearchState.NotFound) }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Search failed", e)
        _uiState.update {
          it.copy(hfSearchState = HfSearchState.Error(e.message ?: "Unknown error"))
        }
      }
    }
  }

  fun resetSearch() {
    _uiState.update { it.copy(hfSearchState = HfSearchState.Idle) }
  }

  /**
   * Returns the `ai-edge-torch` CLI command the user should run on their PC to convert the model.
   */
  fun buildConversionCommand(repoId: String, quantization: QuantizationFormat): String {
    val quant = quantization.flag
    return "python -m ai_edge_torch.generative.examples.convert " +
      "--model_name=${repoId} " +
      "--quantization=$quant " +
      "--output_path=./output/"
  }

  /**
   * Parses the HuggingFace model API JSON response and returns the name of the first file that
   * matches the given quantization token and has a `.task` or `.litertlm` extension, or null if
   * no such file exists.
   */
  private fun findMatchingFile(responseBody: String, quantToken: String): String? {
    return try {
      val json = Gson().fromJson(responseBody, JsonObject::class.java)
      val siblings = json.getAsJsonArray("siblings") ?: return null
      siblings
        .mapNotNull { it.asJsonObject?.get("rfilename")?.asString }
        .firstOrNull { fileName ->
          val lower = fileName.lowercase()
          lower.contains(quantToken.lowercase()) &&
            (lower.endsWith(".task") || lower.endsWith(".litertlm"))
        }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse HF API response", e)
      null
    }
  }
}
