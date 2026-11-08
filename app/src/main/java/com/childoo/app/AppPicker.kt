package com.childoo.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.childoo.designsystem.ChildooColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Launchable apps on this phone, loaded once off the main thread. Null while loading. */
@Composable
fun rememberInstalledApps(): State<List<InstalledApp>?> {
    val context = LocalContext.current
    return produceState<List<InstalledApp>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { loadLaunchableApps(context) }
    }
}

/**
 * Searchable app list. Popular kid apps that are installed are pinned in their own section,
 * so most parents are done in two taps.
 */
@Composable
fun AppPicker(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    header: @Composable () -> Unit = {},
) {
    val apps by rememberInstalledApps()
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = remember(apps, query) {
        val all = apps.orEmpty()
        if (query.isBlank()) all else all.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val popular = remember(filtered) { filtered.filter { it.packageName in SuggestedTargets } }
    val others = remember(filtered) { filtered.filter { it.packageName !in SuggestedTargets } }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { header() }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text(stringResource(R.string.apps_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    unfocusedBorderColor = ChildooColors.Line,
                    focusedBorderColor = ChildooColors.Navy,
                ),
            )
        }
        if (apps == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChildooColors.Navy)
                }
            }
        } else {
            if (popular.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.apps_popular)) }
                items(popular, key = { "p_" + it.packageName }) { app ->
                    AppRow(app, checked = app.packageName in selected, onToggle = { onToggle(app.packageName) })
                }
            }
            if (others.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.apps_all)) }
                items(others, key = { "o_" + it.packageName }) { app ->
                    AppRow(app, checked = app.packageName in selected, onToggle = { onToggle(app.packageName) })
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.apps_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChildooColors.InkSoft,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = ChildooColors.InkSoft,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp),
    )
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (checked) ChildooColors.Mint.copy(alpha = 0.07f) else Color.White)
            .border(1.5.dp, if (checked) ChildooColors.Mint else ChildooColors.Line, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, size = 44)
        Spacer(Modifier.width(14.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.titleSmall,
            color = ChildooColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        CheckCircle(checked)
    }
}

@Composable
fun AppIcon(app: InstalledApp, size: Int) {
    val icon = app.icon
    if (icon != null) {
        Image(icon, contentDescription = app.label, modifier = Modifier.size(size.dp))
    } else {
        Box(
            Modifier
                .size(size.dp)
                .background(ChildooColors.Sand, RoundedCornerShape((size / 4).dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(app.label.take(1), style = MaterialTheme.typography.titleMedium, color = ChildooColors.InkSoft)
        }
    }
}

@Composable
private fun CheckCircle(checked: Boolean) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (checked) ChildooColors.Mint else Color.Transparent)
            .border(2.dp, if (checked) ChildooColors.Mint else ChildooColors.Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/** Selected apps as an overlapping stack of icons, for summaries. */
@Composable
fun SelectedAppsStrip(apps: List<InstalledApp>, modifier: Modifier = Modifier, max: Int = 5) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        apps.take(max).forEach { AppIcon(it, size = 40) }
        if (apps.size > max) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(ChildooColors.Sand, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+${apps.size - max}", style = MaterialTheme.typography.labelMedium, color = ChildooColors.InkSoft)
            }
        }
    }
}
