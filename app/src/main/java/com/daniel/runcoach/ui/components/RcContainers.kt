package com.daniel.runcoach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons

/** TopBar: 48dp, back arrow left, title (Manrope bold), actions right (DESIGN-RESKIN section 3). */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    tabPage: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            RcIconButton(icon = RcIcons.ArrowBack, contentDescription = null, onClick = onBack, tint = colors.textPrimary)
        } else {
            Box(Modifier.width(spacing.s2))
        }
        Text(
            title,
            style = type.titleS,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f).padding(start = spacing.s1),
        )
        actions()
    }
}

/** Big uppercase page title for tab pages (LATIHAN / RIWAYAT / STATISTIK / PENGATURAN). */
@Composable
fun RcScreenTitle(text: String, modifier: Modifier = Modifier) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Text(text.uppercase(), style = type.titleL, color = colors.textPrimary, maxLines = 1, modifier = modifier)
}

/** One bottom-bar tab. */
data class RcTabItem(val icon: Int, val label: String)

/** BottomBar: 68dp + inset, 4 tabs, active accent bar 24x3 at the top edge (DESIGN-RESKIN section 2). */
@Composable
fun RcBottomBar(
    items: List<RcTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val active = index == selectedIndex
                val tint = if (active) colors.accentText else colors.textSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(24.dp)
                            .background(if (active) colors.accent else Color.Transparent, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)),
                    )
                    Icon(
                        painterResource(item.icon),
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.padding(top = 6.dp).size(24.dp),
                    )
                    Text(item.label.uppercase(), style = type.tabBar, color = tint)
                }
            }
        }
    }
}

/** RcSheet: bottom sheet, surface, top radius 28, drag handle (DESIGN.md section 6). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val spacing = LocalRcSpacing.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        scrimColor = colors.scrim,
        shape = RoundedCornerShape(topStart = shapes.radiusXL, topEnd = shapes.radiusXL),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = spacing.s3)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.outlineStrong, RoundedCornerShape(2.dp)),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenH)
                .padding(bottom = spacing.s5),
        ) {
            content()
        }
    }
}

/** RcDialog: surfaceRaised, radius 28, max width 320dp, title + body + actions (DESIGN.md section 6). */
@Composable
fun RcDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
    confirm: (@Composable () -> Unit)? = null,
    dismiss: (@Composable () -> Unit)? = null,
) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp, vertical = spacing.s2)
                .background(colors.surfaceRaised, RoundedCornerShape(shapes.radiusXL))
                .border(1.dp, colors.outline, RoundedCornerShape(shapes.radiusXL))
                .padding(spacing.s5),
            verticalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            Text(title.uppercase(), style = type.titleDlg, color = colors.textPrimary)
            content()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismiss?.invoke()
                confirm?.invoke()
            }
        }
    }
}
