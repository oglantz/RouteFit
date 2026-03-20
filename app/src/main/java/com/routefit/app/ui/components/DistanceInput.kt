package com.routefit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.routefit.app.model.DistanceUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistanceInput(
    distance: String,
    onDistanceChange: (String) -> Unit,
    unit: DistanceUnit,
    onUnitChange: (DistanceUnit) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = distance,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() || it == '.' }
                if (filtered.count { it == '.' } <= 1) {
                    onDistanceChange(filtered)
                }
            },
            label = { Text("Target Distance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )

        SingleChoiceSegmentedButtonRow {
            DistanceUnit.entries.forEachIndexed { index, distanceUnit ->
                SegmentedButton(
                    selected = unit == distanceUnit,
                    onClick = { onUnitChange(distanceUnit) },
                    shape = SegmentedButtonDefaults.itemShape(index, DistanceUnit.entries.size)
                ) {
                    Text(distanceUnit.abbreviation)
                }
            }
        }
    }
}
