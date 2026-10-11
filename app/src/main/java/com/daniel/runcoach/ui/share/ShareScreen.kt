package com.daniel.runcoach.ui.share

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.engine.model.WorkoutJson
import com.daniel.runcoach.share.ShareData
import com.daniel.runcoach.share.ShareImageRenderer
import com.daniel.runcoach.share.ShareImageStore
import com.daniel.runcoach.share.ShareItem
import com.daniel.runcoach.share.ShareTemplate
import com.daniel.runcoach.share.ShareTextColor
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonSize
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcScreenTitle
import com.daniel.runcoach.ui.components.RcSwitchRow
import com.daniel.runcoach.ui.components.RcTextButton
import com.daniel.runcoach.ui.components.RcTextButtonTone
import com.daniel.runcoach.ui.components.SectionHeader
import com.daniel.runcoach.ui.components.SegmentedControl
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** F3/F4: "Bagikan sesi" — pick data, pick template, pick text colour, preview, share or save. */
@Composable
fun ShareScreen(sessionId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SessionRepository(context) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    var data by remember { mutableStateOf<ShareData?>(null) }
    var selected by remember { mutableStateOf(setOf(ShareItem.NAME, ShareItem.DISTANCE, ShareItem.TIME, ShareItem.PACE)) }
    var template by remember { mutableStateOf(ShareTemplate.COMPACT) }
    var textColor by remember { mutableStateOf(ShareTextColor.WHITE) }
    var preview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionId) {
        val session = repository.get(sessionId) ?: return@LaunchedEffect
        val segments = repository.segments(sessionId)
        val splits = repository.splits(sessionId)
        val track = repository.trackPoints(sessionId)
        val passed = segments.filter { it.status == "PASS" }.map { "#${it.idx}" to (it.avgPace ?: 0.0) }
        val splitPairs = splits.map { (if (it.distanceM < 1000.0) "%.2f".format(it.distanceM / 1000.0) else "${it.idx}") to (it.paceSecPerKm ?: 0.0) }
        val hasPassed = passed.isNotEmpty()
        val hasTrack = track.size >= 2
        selected = selected + buildSet {
            if (hasPassed) add(ShareItem.SEGMENTS)
            if (hasTrack) add(ShareItem.ROUTE)
        }
        data = ShareData(
            name = session.name,
            dateTime = SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(Date(session.startedAtEpochMs)),
            distanceKm = session.totalDistanceM / 1000.0,
            movingMs = session.movingMs,
            avgPaceSecPerKm = session.avgPaceSecPerKm,
            splits = splitPairs,
            passedSegments = passed,
            track = track.map { it.lat to it.lon },
        )
    }

    LaunchedEffect(data, selected, template, textColor) {
        val d = data ?: return@LaunchedEffect
        preview = withContext(Dispatchers.Default) {
            ShareImageRenderer.render(context, d, selected, template, textColor)
        }
    }

    fun render(): android.graphics.Bitmap? {
        val d = data ?: return null
        return ShareImageRenderer.render(context, d, selected, template, textColor)
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        TopBar(title = stringResource(R.string.share_title), onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
        ) {
            error?.let { Text(it, style = type.bodySmall, color = colors.fail) }

            SectionHeader(stringResource(R.string.share_pick_data))
            RcCard {
                val hasPassed = data?.passedSegments?.isNotEmpty() == true
                val hasTrack = (data?.track?.size ?: 0) >= 2
                ShareItemRow(stringResource(R.string.share_item_name), ShareItem.NAME, selected, { selected = toggle(selected, it) })
                ShareItemRow(stringResource(R.string.share_item_distance), ShareItem.DISTANCE, selected, { selected = toggle(selected, it) })
                ShareItemRow(stringResource(R.string.share_item_time), ShareItem.TIME, selected, { selected = toggle(selected, it) })
                ShareItemRow(stringResource(R.string.share_item_pace), ShareItem.PACE, selected, { selected = toggle(selected, it) })
                ShareItemRow(stringResource(R.string.share_item_splits), ShareItem.SPLITS, selected, { selected = toggle(selected, it) })
                ShareItemRow(
                    if (hasPassed) stringResource(R.string.share_item_segments) else stringResource(R.string.no_passed_segments),
                    ShareItem.SEGMENTS, selected, { selected = toggle(selected, it) }, enabled = hasPassed,
                )
                ShareItemRow(
                    stringResource(R.string.share_item_route), ShareItem.ROUTE, selected, { selected = toggle(selected, it) },
                    enabled = hasTrack, showDivider = false,
                )
            }

            SectionHeader(stringResource(R.string.share_pick_template))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                TemplateCard(stringResource(R.string.tpl_compact), "1:1", ShareTemplate.COMPACT, template, { template = it }, Modifier.weight(1f))
                TemplateCard(stringResource(R.string.tpl_split), "4:5", ShareTemplate.SPLIT, template, { template = it }, Modifier.weight(1f))
                TemplateCard(stringResource(R.string.tpl_full), "9:16", ShareTemplate.FULL, template, { template = it }, Modifier.weight(1f))
            }

            SectionHeader(stringResource(R.string.share_text_color))
            SegmentedControl(
                options = listOf(ShareTextColor.WHITE to stringResource(R.string.share_white), ShareTextColor.BLACK to stringResource(R.string.share_black)),
                selected = textColor,
                onSelect = { textColor = it },
            )

            SectionHeader(stringResource(R.string.share_preview))
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp).background(colors.surfaceRaised, RoundedCornerShape(14.dp)).padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                preview?.let {
                    Image(bitmap = it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = spacing.s4))
        }

        Column(modifier = Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = spacing.screenH).padding(vertical = spacing.s3), verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            RcButton(
                stringResource(R.string.share),
                onClick = {
                    val bmp = render() ?: return@RcButton
                    runCatching {
                        val uri = ShareImageStore.saveToCache(context, bmp)
                        context.startActivity(Intent.createChooser(ShareImageStore.shareIntent(uri), context.getString(R.string.share_title)))
                    }.onFailure { error = it.message }
                },
                fillWidth = true,
            )
            RcButton(
                stringResource(R.string.save_image),
                onClick = {
                    val bmp = render() ?: return@RcButton
                    if (ShareImageStore.saveToGallery(context, bmp)) {
                        Toast.makeText(context, R.string.saved_gallery, Toast.LENGTH_SHORT).show()
                    } else {
                        error = context.getString(R.string.save_image) + " gagal"
                    }
                },
                variant = RcButtonVariant.Secondary,
                fillWidth = true,
            )
        }
    }
}

private fun toggle(set: Set<ShareItem>, item: ShareItem): Set<ShareItem> =
    if (item in set) set - item else set + item

@Composable
private fun ShareItemRow(label: String, item: ShareItem, selected: Set<ShareItem>, onToggle: (ShareItem) -> Unit, enabled: Boolean = true, showDivider: Boolean = true) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val supported = true // grey-out is handled by template cards via supports()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).clickable(enabled = enabled) { onToggle(item) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = type.body, color = if (enabled) colors.textPrimary else colors.menuDisabled, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .background(if (item in selected && enabled) colors.accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp))
                    .border(1.5.dp, if (item in selected && enabled) colors.accent else colors.outlineStrong, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (item in selected && enabled) Text("✓", style = type.chip, color = colors.onAccent)
            }
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
    }
}

@Composable
private fun TemplateCard(label: String, ratio: String, value: ShareTemplate, selectedTemplate: ShareTemplate, onSelect: (ShareTemplate) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val active = value == selectedTemplate
    Column(
        modifier = modifier
            .background(if (active) colors.accentContainer else colors.surface, RoundedCornerShape(20.dp))
            .border(1.5.dp, if (active) colors.accent else colors.outline, RoundedCornerShape(20.dp))
            .clickable { onSelect(value) }
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(value.width.toFloat() / value.height.toFloat()).background(colors.surfaceRaised, RoundedCornerShape(8.dp)),
        )
        Text(label.uppercase(), style = type.titleS, color = colors.textPrimary, modifier = Modifier.padding(top = 8.dp))
        Text(ratio, style = type.caption, color = colors.textSecondary)
    }
}
