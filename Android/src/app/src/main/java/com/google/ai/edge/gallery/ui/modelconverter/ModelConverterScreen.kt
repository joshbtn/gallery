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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.edge.gallery.R
import kotlinx.coroutines.launch

private const val LITERT_DOCS_URL = "https://ai.google.dev/edge/litert/models/convert_tf"
private const val LITERT_COMMUNITY_URL = "https://huggingface.co/litert-community"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConverterScreen(
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ModelConverterViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val focusManager = LocalFocusManager.current

  // File picker for local source files.
  val filePickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == android.app.Activity.RESULT_OK) {
        result.data?.data?.let { uri ->
          val name = getDisplayNameFromUri(context, uri) ?: uri.lastPathSegment ?: "unknown"
          val path = uri.toString()
          viewModel.setLocalFile(path = path, name = name)
        }
      }
    }

  Scaffold(
    modifier = modifier,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.converter_screen_title)) },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_icon))
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.surfaceContainer)
          .padding(top = innerPadding.calculateTopPadding())
          .verticalScroll(rememberScrollState()),
    ) {
      // ── Tab row ──────────────────────────────────────────────────────────
      TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
        Tab(
          selected = uiState.selectedTab == ConverterTab.HUGGING_FACE,
          onClick = { viewModel.selectTab(ConverterTab.HUGGING_FACE) },
          text = { Text(stringResource(R.string.converter_tab_huggingface)) },
        )
        Tab(
          selected = uiState.selectedTab == ConverterTab.LOCAL_FILE,
          onClick = { viewModel.selectTab(ConverterTab.LOCAL_FILE) },
          text = { Text(stringResource(R.string.converter_tab_local)) },
        )
      }

      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        when (uiState.selectedTab) {
          ConverterTab.HUGGING_FACE ->
            HuggingFaceTab(
              uiState = uiState,
              viewModel = viewModel,
              context = context,
              snackbarHostState = snackbarHostState,
              focusManager = focusManager,
            )

          ConverterTab.LOCAL_FILE ->
            LocalFileTab(
              uiState = uiState,
              viewModel = viewModel,
              context = context,
              snackbarHostState = snackbarHostState,
              onPickFile = {
                val intent =
                  Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                  }
                filePickerLauncher.launch(intent)
              },
            )
        }
      }

      Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 16.dp))
    }
  }
}

// ── HuggingFace tab ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun HuggingFaceTab(
  uiState: ModelConverterUiState,
  viewModel: ModelConverterViewModel,
  context: Context,
  snackbarHostState: SnackbarHostState,
  focusManager: androidx.compose.ui.focus.FocusManager,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // ── Intro ─────────────────────────────────────────────────────────────
    Text(
      "Enter a HuggingFace model ID to search for a pre-converted LiteRT version in the " +
        "litert-community organisation. If no pre-converted model exists, the screen shows " +
        "the command to convert it yourself using the ai-edge-torch CLI.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // ── Model ID input ────────────────────────────────────────────────────
    OutlinedTextField(
      value = uiState.hfRepoId,
      onValueChange = { viewModel.updateHfRepoId(it) },
      label = { Text(stringResource(R.string.converter_hf_repo_label)) },
      placeholder = { Text(stringResource(R.string.converter_hf_repo_placeholder)) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = {
        focusManager.clearFocus()
        viewModel.searchLiteRTCommunity()
      }),
    )

    // ── Quantization selector ─────────────────────────────────────────────
    QuantizationSelector(
      selected = uiState.quantization,
      onSelected = { viewModel.updateQuantization(it) },
    )

    // ── Search button ─────────────────────────────────────────────────────
    Button(
      onClick = {
        focusManager.clearFocus()
        viewModel.searchLiteRTCommunity()
      },
      modifier = Modifier.fillMaxWidth(),
      enabled = uiState.hfRepoId.isNotBlank() &&
        uiState.hfSearchState !is HfSearchState.Searching &&
        uiState.hfSearchState !is HfSearchState.Downloading,
    ) {
      Icon(
        Icons.Outlined.Search,
        contentDescription = null,
        modifier = Modifier.padding(end = 8.dp).size(18.dp),
      )
      Text(stringResource(R.string.converter_search_button))
    }

    // ── Search results ────────────────────────────────────────────────────
    when (val state = uiState.hfSearchState) {
      is HfSearchState.Searching ->
        ConversionProgressCard(label = stringResource(R.string.converter_searching))

      is HfSearchState.Downloading ->
        ConversionProgressCard(label = stringResource(R.string.converter_downloading))

      is HfSearchState.Found -> FoundModelCard(state = state, viewModel = viewModel)

      is HfSearchState.Downloaded ->
        SuccessCard(fileName = state.fileName)

      is HfSearchState.NotFound ->
        NotFoundCard(
          repoId = uiState.hfRepoId,
          quantization = uiState.quantization,
          viewModel = viewModel,
          context = context,
          snackbarHostState = snackbarHostState,
        )

      is HfSearchState.Error ->
        ErrorCard(message = state.message, onRetry = { viewModel.searchLiteRTCommunity() })

      is HfSearchState.Idle -> {} // nothing to show yet
    }
  }
}

@Composable
private fun FoundModelCard(
  state: HfSearchState.Found,
  viewModel: ModelConverterViewModel,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
          Icons.Outlined.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          stringResource(R.string.converter_found_title),
          style = MaterialTheme.typography.titleSmall,
        )
      }
      Text(
        "Repo: ${state.repoId}\nFile: ${state.fileName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        "Tap Download to save the pre-converted model and make it available in the app.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SuccessCard(fileName: String, modifier: Modifier = Modifier) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
          Icons.Outlined.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text("Model downloaded!", style = MaterialTheme.typography.titleSmall)
      }
      Text(
        "\"$fileName\" has been downloaded. Go to the Models screen to use it.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun NotFoundCard(
  repoId: String,
  quantization: QuantizationFormat,
  viewModel: ModelConverterViewModel,
  context: Context,
  snackbarHostState: SnackbarHostState,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val command = viewModel.buildConversionCommand(repoId, quantization)
  val commandCopiedMsg = stringResource(R.string.converter_command_copied)

  Card(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
          Icons.Rounded.Error,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(20.dp),
        )
        Text(
          stringResource(R.string.converter_not_found_title),
          style = MaterialTheme.typography.titleSmall,
        )
      }

      Text(
        stringResource(R.string.converter_instructions_title),
        style = MaterialTheme.typography.labelLarge,
      )
      Text(
        stringResource(R.string.converter_instructions_content),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Command block.
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
      ) {
        Text(
          command,
          style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Copy button.
      OutlinedButton(
        onClick = {
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          clipboard.setPrimaryClip(ClipData.newPlainText("conversion_command", command))
          scope.launch { snackbarHostState.showSnackbar(commandCopiedMsg) }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(
          Icons.Outlined.ContentCopy,
          contentDescription = null,
          modifier = Modifier.padding(end = 8.dp).size(18.dp),
        )
        Text(stringResource(R.string.converter_copy_command))
      }

      // Open docs button.
      OutlinedButton(
        onClick = {
          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LITERT_DOCS_URL)))
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(
          Icons.Outlined.OpenInBrowser,
          contentDescription = null,
          modifier = Modifier.padding(end = 8.dp).size(18.dp),
        )
        Text(stringResource(R.string.converter_open_docs))
      }

      Text(
        stringResource(R.string.converter_import_after_conversion),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
          Icons.Rounded.Error,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(20.dp),
        )
        Text("Search failed", style = MaterialTheme.typography.titleSmall)
      }
      Text(message, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
      Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
    }
  }
}

// ── Local file tab ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun LocalFileTab(
  uiState: ModelConverterUiState,
  viewModel: ModelConverterViewModel,
  context: Context,
  snackbarHostState: SnackbarHostState,
  onPickFile: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val commandCopiedMsg = stringResource(R.string.converter_command_copied)

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
      stringResource(R.string.converter_local_instructions),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // ── File picker ───────────────────────────────────────────────────────
    Button(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
      Icon(
        Icons.Outlined.FileOpen,
        contentDescription = null,
        modifier = Modifier.padding(end = 8.dp).size(18.dp),
      )
      Text(stringResource(R.string.converter_local_pick_file))
    }

    if (uiState.localFileName.isNotEmpty()) {
      Text(
        "Selected: ${uiState.localFileName}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // ── Quantization selector ─────────────────────────────────────────────
    QuantizationSelector(
      selected = uiState.quantization,
      onSelected = { viewModel.updateQuantization(it) },
    )

    // ── Conversion command card ───────────────────────────────────────────
    if (uiState.localFileName.isNotEmpty()) {
      val command =
        viewModel.buildConversionCommand(uiState.localFilePath, uiState.quantization)

      Card(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            stringResource(R.string.converter_instructions_title),
            style = MaterialTheme.typography.labelLarge,
          )
          Text(
            stringResource(R.string.converter_instructions_content),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
          ) {
            Text(
              command,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          OutlinedButton(
            onClick = {
              val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("conversion_command", command))
              scope.launch { snackbarHostState.showSnackbar(commandCopiedMsg) }
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(
              Icons.Outlined.ContentCopy,
              contentDescription = null,
              modifier = Modifier.padding(end = 8.dp).size(18.dp),
            )
            Text(stringResource(R.string.converter_copy_command))
          }

          OutlinedButton(
            onClick = {
              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LITERT_DOCS_URL)))
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(
              Icons.Outlined.OpenInBrowser,
              contentDescription = null,
              modifier = Modifier.padding(end = 8.dp).size(18.dp),
            )
            Text(stringResource(R.string.converter_open_docs))
          }

          Text(
            stringResource(R.string.converter_import_after_conversion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

// ── Shared composables ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuantizationSelector(
  selected: QuantizationFormat,
  onSelected: (QuantizationFormat) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      stringResource(R.string.converter_quantization_label),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      QuantizationFormat.entries.forEachIndexed { index, format ->
        SegmentedButton(
          selected = selected == format,
          onClick = { onSelected(format) },
          shape = SegmentedButtonDefaults.itemShape(index = index,
            count = QuantizationFormat.entries.size),
          label = { Text(format.label) },
        )
      }
    }
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────────────────────────

private fun getDisplayNameFromUri(context: Context, uri: Uri): String? {
  return try {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx != -1) cursor.getString(idx) else null
      } else null
    }
  } catch (e: Exception) {
    null
  }
}
