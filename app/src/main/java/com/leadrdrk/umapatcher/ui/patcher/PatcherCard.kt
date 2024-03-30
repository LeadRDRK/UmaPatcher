package com.leadrdrk.umapatcher.ui.patcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PatcherCard(
    label: String,
    rootRequired: Boolean = false,
    icon: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    ElevatedCard {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
        ) {
            Row {
                icon()
                Spacer(Modifier.width(20.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            content()
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                content = buttons
            )
        }
    }
}