package com.voria.kenaret.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.ui.theme.phaseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KenaretTopBar(title: String, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

/** A check (✓) or cross (✕) line used in the partner's do / don't lists. */
@Composable
fun MarkedLine(text: String, positive: Boolean) {
    val color = if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(22.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (positive) "✓" else "✕", color = color, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun BulletLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 9.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@StringRes
fun phaseNameRes(phase: CyclePhase): Int = when (phase) {
    CyclePhase.MENSTRUAL -> R.string.phase_menstrual
    CyclePhase.FOLLICULAR -> R.string.phase_follicular
    CyclePhase.OVULATION -> R.string.phase_ovulation
    CyclePhase.LUTEAL -> R.string.phase_luteal
}

@Composable
fun PhaseChip(phase: CyclePhase, modifier: Modifier = Modifier) {
    val color = phaseColor(phase)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(phaseNameRes(phase)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Small italic-style note reminding that information is general and approximate. */
@Composable
fun NoteText(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
