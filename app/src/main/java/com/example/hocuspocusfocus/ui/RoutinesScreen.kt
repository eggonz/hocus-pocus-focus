package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hocuspocusfocus.R
import com.example.hocuspocusfocus.Routine
import com.example.hocuspocusfocus.RoutineIcon
import com.example.hocuspocusfocus.ui.theme.DisplayFontFamily
import com.example.hocuspocusfocus.ui.theme.HocusPocusFocusTheme

/** The routines screen: a title bar with a plus button to add a routine, over a grid of [routines]. */
@Composable
fun RoutinesScreen(
    routines: List<Routine>,
    onOpenRoutine: (id: Int) -> Unit,
    onAddRoutine: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.routines),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = DisplayFontFamily,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onAddRoutine) {
                    Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.new_routine))
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(routines, key = { it.id }) { routine ->
                    RoutineTile(routine, onClick = { onOpenRoutine(routine.id) })
                }
            }
        }
    }
}

@Composable
private fun RoutineTile(routine: Routine, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
        ) {
            // The title below already names the routine
            Icon(
                painterResource(routine.icon.drawable),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                routine.title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = DisplayFontFamily,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatDuration(routine.durationMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutinesPreview() {
    HocusPocusFocusTheme {
        RoutinesScreen(
            routines = listOf(
                Routine(0, RoutineIcon.SUN, "Morning", 60),
                Routine(1, RoutineIcon.BOOK, "Reading", 30),
            ),
            onOpenRoutine = {},
            onAddRoutine = {},
            onBack = {},
        )
    }
}
