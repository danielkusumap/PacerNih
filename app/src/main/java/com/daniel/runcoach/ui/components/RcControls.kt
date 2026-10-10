package com.daniel.runcoach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons

/** SegmentedControl: 44dp track, active filled accent (DESIGN.md section 6). */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val type = LocalRcTypography.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.surfaceRaised, RoundedCornerShape(shapes.radiusM))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(if (active) colors.accent else Color.Transparent, RoundedCornerShape(shapes.radiusM - 2.dp))
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = type.titleS, color = if (active) colors.onAccent else colors.textSecondary)
            }
        }
    }
}

/** RcSwitchRow: ListRow with a custom switch, no Material pill (DESIGN.md section 6). */
@Composable
fun RcSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = type.titleS, color = colors.textPrimary)
            if (subtitle != null) Text(subtitle, style = type.bodySmall, color = colors.textSecondary)
        }
        Switch(checked = checked)
    }
}

/** Custom switch: 52x32 track, 24dp thumb (DESIGN.md section 6). */
@Composable
fun Switch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalRcColors.current
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .background(if (checked) colors.accent else colors.outlineStrong, RoundedCornerShape(16.dp))
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if (checked) colors.onAccent else colors.textSecondary, RoundedCornerShape(12.dp)),
        )
    }
}

/** NumberStepper: -/+ 48dp buttons, value centred, step label (DESIGN.md section 6). */
@Composable
fun NumberStepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    stepLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StepperButton(icon = RcIcons.ChevronLeft, enabled = enabled, onClick = onDecrement)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = type.numberM, color = colors.textPrimary)
            Text(stepLabel, style = type.caption, color = colors.textTertiary)
        }
        StepperButton(icon = RcIcons.ChevronRight, enabled = enabled, onClick = onIncrement)
    }
}

@Composable
private fun StepperButton(icon: Int, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .background(colors.surfaceRaised, RoundedCornerShape(shapes.radiusS))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(24.dp))
    }
}

/** RcTextField: 56dp (multiline min 96dp), surfaceRaised, focus accent, error fail (DESIGN.md section 6). */
@Composable
fun RcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    multiline: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val type = LocalRcTypography.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        isError -> colors.fail
        focused -> colors.accent
        else -> colors.outlineStrong
    }
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (multiline) 96.dp else 56.dp)
                .background(colors.surfaceRaised, RoundedCornerShape(shapes.radiusM))
                .border(if (focused) 2.dp else 1.5.dp, borderColor, RoundedCornerShape(shapes.radiusM))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column {
                Text(label, style = type.label, color = colors.textSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = type.body, color = colors.textTertiary)
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            enabled = enabled,
                            textStyle = type.body.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.accent),
                            singleLine = !multiline,
                            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                            interactionSource = interaction,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    trailing?.invoke()
                }
            }
        }
        if (isError && errorText != null) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(painterResource(RcIcons.Error), contentDescription = null, tint = colors.fail, modifier = Modifier.size(16.dp))
                Text(errorText, style = type.bodySmall, color = colors.fail)
            }
        }
    }
}
