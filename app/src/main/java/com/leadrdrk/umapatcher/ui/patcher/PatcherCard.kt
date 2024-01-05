package com.leadrdrk.umapatcher.ui.patcher

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PatcherCard(
    label: String,
    rootRequired: Boolean = false,
    icon: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
        ) {
            icon()
            Column(Modifier.padding(start = 20.dp)) {
                Row {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (rootRequired) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "ROOT",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(100)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                content()
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = buttons
                )
            }
        }
    }
}