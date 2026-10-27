package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClickableContextualText(
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onBackground,
    fontWeight: FontWeight = FontWeight.SemiBold,
    onWordClick: (word: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = remember(text) {
        text.split(" ").filter { it.isNotBlank() }
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Start,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        for (token in tokens) {
            val cleanWord = token.trim { !it.isLetterOrDigit() }
            val interactionSource = remember { MutableInteractionSource() }

            Text(
                text = "$token ",
                style = style,
                fontWeight = fontWeight,
                color = color,
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = cleanWord.isNotBlank()
                    ) {
                        if (cleanWord.isNotBlank()) {
                            onWordClick(cleanWord)
                        }
                    }
                    .padding(vertical = 1.dp)
            )
        }
    }
}
