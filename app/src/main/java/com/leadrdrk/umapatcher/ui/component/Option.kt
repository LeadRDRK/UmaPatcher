package com.leadrdrk.umapatcher.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly

@Composable
fun OptionBase(
    title: String,
    desc: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                OptionTitleAndDesc(title, desc)
            }
            Spacer(Modifier.width(16.dp))
            content()
        }
        Divider()
    }
}

@Composable
fun OptionTitleAndDesc(
    title: String,
    desc: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = desc,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun BooleanOption(
    title: String,
    desc: String,
    state: MutableState<Boolean>,
    enabled: Boolean = true
) {
    OptionBase(
        title = title,
        desc = desc,
        onClick = {
            if (enabled)
                state.value = !state.value
        }
    ) {
        Switch(
            checked = state.value,
            onCheckedChange = { state.value = it },
            enabled = enabled
        )
    }
}

@Composable
fun StringOption(
    title: String,
    state: MutableState<String>,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions()
) {
    var openDialog by remember { mutableStateOf(false) }
    OptionBase(
        title = title,
        desc = state.value,
        onClick = { openDialog = true }
    ) {}

    var tmpValue by remember { mutableStateOf(state.value) }
    LaunchedEffect(openDialog) {
        // Reset value every time dialog is reopened
        tmpValue = state.value
    }

    if (openDialog) {
        SimpleOkCancelDialog(
            title = title,
            onClose = { ok ->
                openDialog = false
                if (ok) state.value = tmpValue
            }
        ) {
            TextField(
                value = tmpValue,
                onValueChange = { tmpValue = it },
                placeholder = {
                    if (placeholder != null)
                        Text(placeholder)
                },
                keyboardOptions = keyboardOptions
            )
        }
    }
}

@Composable
fun IntOption(
    title: String,
    state: MutableIntState,
    placeholder: String? = null
) {
    val strValue = remember { mutableStateOf(state.intValue.toString()) }

    LaunchedEffect(strValue.value) {
        val str = strValue.value
        if (str.isDigitsOnly()) {
            val value = str.toInt()
            if (state.intValue != value)
                state.intValue = value
        }
    }

    StringOption(
        title = title,
        state = strValue,
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
fun RadioGroupOption(
    title: String,
    desc: String,
    choices: Array<String>,
    state: MutableState<Int>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp, 16.dp, 16.dp, bottom = 8.dp)
        ) {
            OptionTitleAndDesc(title, desc)
        }
        Column {
            choices.forEachIndexed { i, choice ->
                Row(
                    modifier = Modifier
                        .clickable { state.value = i }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = (state.value == i),
                            onClick = { state.value = i }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = choice,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Divider()
    }
}