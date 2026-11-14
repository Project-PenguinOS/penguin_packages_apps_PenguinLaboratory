/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.penguin.laboratory.styles

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.penguin.laboratory.R
import com.penguin.laboratory.ui.ConnectedChoice
import com.penguin.laboratory.ui.DropdownItem
import com.penguin.laboratory.ui.LabTheme
import com.penguin.laboratory.ui.SectionHeader
import com.penguin.laboratory.ui.SegmentedBlock
import com.penguin.laboratory.ui.SegmentedGroup
import com.penguin.laboratory.ui.SwitchItem

/** The looks PenguinSystemUI can draw its panels in. All in Settings.Secure, read by SystemUI. */
object StylesStore {
    const val QS_PANEL = "qs_panel_style"
    const val QS_FOLDER = "qs_connectivity_folder"
    const val QS_ONE_UI = "qs_oneui_experimental"
    const val VOLUME_PANEL = "volume_panel_style"
    const val POWER_MENU = "power_menu_style"

    /** A style with the value SystemUI reads and its label. */
    class Choice(val value: Int, @StringRes val label: Int)

    val qsPanel =
        listOf(
            Choice(1, R.string.styles_qs_penguin),
            Choice(2, R.string.styles_qs_myui),
            Choice(3, R.string.styles_qs_harmony),
            Choice(6, R.string.styles_qs_stock),
            Choice(5, R.string.styles_qs_coloros),
        )
    val volumePanel =
        listOf(
            Choice(1, R.string.styles_volume_expandable),
            Choice(2, R.string.styles_volume_oneui),
            Choice(3, R.string.styles_volume_myui),
        )
    val powerMenu =
        listOf(Choice(0, R.string.styles_power_penguin), Choice(1, R.string.styles_power_ios))

    fun get(context: Context, key: String, default: Int) =
        Settings.Secure.getInt(context.contentResolver, key, default)

    fun put(context: Context, key: String, value: Int) {
        Settings.Secure.putInt(context.contentResolver, key, value)
    }

    /** How many styles are in use other than AOSP's, for the Laboratory's list. */
    fun customised(context: Context) =
        listOf(
                get(context, QS_PANEL, 1) != 0,
                get(context, VOLUME_PANEL, 0) != 0,
                get(context, POWER_MENU, 0) != 0,
                UdfpsPack.ICON.get(context) != 0,
                UdfpsPack.ANIMATION.get(context) != 0,
            )
            .count { it }
}

class StylesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { LabTheme { Styles() } }
    }

    @Composable
    private fun Styles() {
        val context = this
        var generation by remember { mutableIntStateOf(0) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { generation++ }
        val oneUi = remember(generation) { StylesStore.get(context, StylesStore.QS_ONE_UI, 0) != 0 }
        var qs by remember(generation) { mutableStateOf(StylesStore.get(context, StylesStore.QS_PANEL, 1)) }
        var folder by
            remember(generation) {
                mutableStateOf(StylesStore.get(context, StylesStore.QS_FOLDER, 1) != 0)
            }
        var volume by
            remember(generation) { mutableStateOf(StylesStore.get(context, StylesStore.VOLUME_PANEL, 0)) }
        var power by
            remember(generation) { mutableStateOf(StylesStore.get(context, StylesStore.POWER_MENU, 0)) }
        val udfpsIcons = remember { UdfpsPack.ICON.load(context) }
        val udfpsAnimations = remember { UdfpsPack.ANIMATION.load(context) }
        val udfpsIcon = remember(generation) { UdfpsPack.ICON.get(context) }
        val udfpsAnimation = remember(generation) { UdfpsPack.ANIMATION.get(context) }
        var playing by remember { mutableStateOf(true) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { playing = true }
        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { playing = false }

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                MediumFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.styles_title)) },
                    subtitle = { Text(stringResource(R.string.styles_entry_summary)) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(painterResource(R.drawable.ic_back), contentDescription = null)
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 24.dp,
                    )
            ) {
                item { SectionHeader(stringResource(R.string.styles_qs_title)) }
                item {
                    SegmentedGroup {
                        // Too many styles with long names for a button row, so a menu.
                        val labels =
                            mapOf(0 to R.string.styles_default) +
                                StylesStore.qsPanel.associate { it.value to it.label }
                        DropdownItem(
                            title = stringResource(R.string.styles_qs_style),
                            options = labels.keys.toList(),
                            selected = qs,
                            onSelect = {
                                StylesStore.put(context, StylesStore.QS_PANEL, it)
                                qs = it
                            },
                            label = { stringResource(labels[it] ?: R.string.styles_default) },
                            index = 0,
                            count = 2,
                            enabled = !oneUi,
                            supporting =
                                if (oneUi) stringResource(R.string.styles_qs_oneui_active) else null,
                        )
                        SwitchItem(
                            title = stringResource(R.string.styles_qs_folder),
                            supporting = stringResource(R.string.styles_qs_folder_summary),
                            checked = folder,
                            onCheckedChange = {
                                StylesStore.put(context, StylesStore.QS_FOLDER, if (it) 1 else 0)
                                folder = it
                            },
                            index = 1,
                            count = 2,
                            icon = R.drawable.materialsymbols_ic_grid_view_rounded_filled,
                        )
                    }
                }
                item { SectionHeader(stringResource(R.string.styles_volume_title)) }
                item {
                    SegmentedBlock(index = 0, count = 1) {
                        StyleChoice(
                            choices = StylesStore.volumePanel,
                            selected = volume,
                            // AOSP's own volume panel, for when none of these is wanted.
                            default = Pair(0, R.string.styles_default),
                        ) {
                            StylesStore.put(context, StylesStore.VOLUME_PANEL, it)
                            volume = it
                        }
                    }
                }
                item { SectionHeader(stringResource(R.string.styles_power_title)) }
                item {
                    SegmentedBlock(index = 0, count = 1) {
                        StyleChoice(choices = StylesStore.powerMenu, selected = power) {
                            StylesStore.put(context, StylesStore.POWER_MENU, it)
                            power = it
                        }
                    }
                }
                val udfps =
                    listOfNotNull(
                        Triple(UdfpsPack.ICON, udfpsIcons, udfpsIcon).takeIf { udfpsIcons.isNotEmpty() },
                        Triple(UdfpsPack.ANIMATION, udfpsAnimations, udfpsAnimation)
                            .takeIf { udfpsAnimations.isNotEmpty() },
                    )
                if (udfps.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.styles_udfps_title)) }
                    item {
                        SegmentedGroup {
                            udfps.forEachIndexed { index, (pack, entries, chosen) ->
                                UdfpsItem(pack, entries.getOrNull(chosen), chosen, playing, index, udfps.size)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun UdfpsItem(
        pack: UdfpsPack,
        chosen: UdfpsPack.Entry?,
        chosenIndex: Int,
        playing: Boolean,
        index: Int,
        count: Int,
    ) {
        val icon = pack == UdfpsPack.ICON
        SegmentedListItem(
            onClick = { startActivity(UdfpsPickerActivity.intent(this, pack)) },
            shapes = ListItemDefaults.segmentedShapes(index, count),
            leadingContent = {
                Icon(
                    painterResource(
                        if (icon) R.drawable.materialsymbols_ic_fingerprint_rounded_filled
                        else R.drawable.materialsymbols_ic_animation_rounded_filled
                    ),
                    contentDescription = null,
                )
            },
            trailingContent = {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    val px = with(LocalDensity.current) { 48.dp.roundToPx() }
                    when {
                        chosen == null || (!icon && chosenIndex == 0) ->
                            Icon(
                                painterResource(R.drawable.materialsymbols_ic_block_rounded_filled),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp),
                            )
                        else ->
                            PackImage(
                                pack,
                                chosen.drawable,
                                if (icon) px * 2 / 3 else px,
                                18,
                                playing,
                                if (icon) Modifier.size(32.dp) else Modifier.fillMaxSize(),
                            )
                    }
                }
            },
        ) {
            Text(stringResource(if (icon) R.string.styles_udfps_icon else R.string.styles_udfps_animation))
        }
    }

    /** One button per style, [default] first when AOSP's look is one of the options. */
    @Composable
    private fun StyleChoice(
        choices: List<StylesStore.Choice>,
        selected: Int,
        default: Pair<Int, Int>? = null,
        onSelect: (Int) -> Unit,
    ) {
        val options = listOfNotNull(default?.first) + choices.map { it.value }
        val labels = (listOfNotNull(default) + choices.map { it.value to it.label }).toMap()
        ConnectedChoice(
            options = options,
            selected = selected,
            onSelect = onSelect,
            label = { stringResource(labels.getValue(it)) },
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}
