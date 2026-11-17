/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.penguin.laboratory.tabs

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.penguin.laboratory.R

/** Which Settings table a key lives in. */
enum class Table {
    System,
    Secure,
    Global;

    fun get(resolver: ContentResolver, key: String): String? =
        when (this) {
            System -> Settings.System.getString(resolver, key)
            Secure -> Settings.Secure.getString(resolver, key)
            Global -> Settings.Global.getString(resolver, key)
        }

    fun put(resolver: ContentResolver, key: String, value: String) {
        when (this) {
            System -> Settings.System.putString(resolver, key, value)
            Secure -> Settings.Secure.putString(resolver, key, value)
            Global -> Settings.Global.putString(resolver, key, value)
        }
    }
}

/** One row on a Lab tab. Rows read and write the same keys Settings used before they moved. */
sealed interface LabRow {
    val title: Int

    /** A switch stored as 1 or 0. [parent] greys it out while that switch is off. */
    class Toggle(
        val table: Table,
        val key: String,
        @StringRes override val title: Int,
        @StringRes val summary: Int? = null,
        val default: Boolean = false,
        @DrawableRes val icon: Int? = null,
        val parent: String? = null,
    ) : LabRow {
        fun read(context: Context) =
            table.get(context.contentResolver, key)?.toIntOrNull()?.let { it != 0 } ?: default

        fun write(context: Context, on: Boolean) =
            table.put(context.contentResolver, key, if (on) "1" else "0")
    }

    /** One of a few values, stored as the value's string. */
    class Choice(
        val table: Table,
        val key: String,
        @StringRes override val title: Int,
        @ArrayRes val labels: Int,
        val values: List<String>,
        val default: String,
        @DrawableRes val icon: Int? = null,
        /** Greys this out while the choice with this key, in the same section, has this value. */
        val disabledWhen: Pair<String, String>? = null,
    ) : LabRow {
        fun read(context: Context) = table.get(context.contentResolver, key) ?: default

        fun write(context: Context, value: String) = table.put(context.contentResolver, key, value)
    }

    /** A page with more to it than switches, still drawn by Settings, opened in place. */
    class Link(
        val fragment: String,
        @StringRes override val title: Int,
        @StringRes val summary: Int? = null,
        @DrawableRes val icon: Int? = null,
    ) : LabRow
}

/** A group of rows. [largeScreenOnly] groups show only on tablets and foldables opened out. */
class LabSection(
    @StringRes val title: Int?,
    val rows: List<LabRow>,
    val largeScreenOnly: Boolean = false,
)

class LabTab(
    @StringRes val label: Int,
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val sections: List<LabSection>,
)

/** The tabs after Originals, holding the PenguinOS options that used to be spread over Settings. */
object LabTabs {
    private const val S = "com.android.settings"

    val all =
        listOf(
            LabTab(
                R.string.tab_status,
                R.string.tab_status_title,
                R.drawable.materialsymbols_ic_top_panel_open_rounded_filled,
                listOf(
                    LabSection(
                        R.string.section_status_bar,
                        listOf(
                            LabRow.Link(
                                "$S.custom.networktraffic.NetworkTrafficSettings",
                                R.string.row_network_speed,
                                R.string.row_network_speed_summary,
                                R.drawable.materialsymbols_ic_swap_vert_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "status_bar_brightness_control",
                                R.string.row_brightness_slide,
                                R.string.row_brightness_slide_summary,
                                icon = R.drawable.materialsymbols_ic_brightness_medium_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "status_bar_brightness_control_lockscreen",
                                R.string.row_brightness_slide_locked,
                                parent = "status_bar_brightness_control",
                            ),
                            LabRow.Link(
                                "$S.display.StatusbarPaddingSettings",
                                R.string.row_status_padding,
                                R.string.row_status_padding_summary,
                                R.drawable.materialsymbols_ic_padding_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_battery_icon,
                        listOf(
                            LabRow.Choice(
                                Table.System,
                                "status_bar_battery_style",
                                R.string.row_battery_style,
                                R.array.battery_style_labels,
                                listOf("0", "1", "3", "2"),
                                "0",
                            ),
                            LabRow.Choice(
                                Table.System,
                                "status_bar_show_battery_percent",
                                R.string.row_battery_percent,
                                R.array.battery_percent_labels,
                                listOf("0", "1", "2"),
                                "0",
                                // The text style is the percentage itself.
                                disabledWhen = "status_bar_battery_style" to "2",
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_island,
                        listOf(
                            LabRow.Link(
                                "$S.display.DynamicIslandSettings",
                                R.string.row_island,
                                R.string.row_island_summary,
                                R.drawable.materialsymbols_ic_camera_front_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_quick_settings,
                        listOf(
                            LabRow.Toggle(
                                Table.Secure,
                                "qs_show_auto_brightness",
                                R.string.row_qs_auto_brightness,
                                R.string.row_qs_auto_brightness_summary,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_brightness_auto_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.network.QsDataUsageSettings",
                                R.string.row_qs_data_usage,
                                R.string.row_qs_data_usage_summary,
                                R.drawable.materialsymbols_ic_data_usage_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_notifications,
                        listOf(
                            LabRow.Link(
                                "$S.notification.HeadsUpSettings",
                                R.string.row_heads_up,
                                R.string.row_heads_up_summary,
                                R.drawable.materialsymbols_ic_notifications_active_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "notification_sound_vib_screen_on",
                                R.string.row_screen_on_alerts,
                                R.string.row_screen_on_alerts_summary,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_notification_sound_rounded_filled,
                            ),
                        ),
                    ),
                ),
            ),
            LabTab(
                R.string.tab_lock,
                R.string.tab_lock_title,
                R.drawable.materialsymbols_ic_lock_rounded_filled,
                listOf(
                    LabSection(
                        R.string.section_lock_look,
                        listOf(
                            LabRow.Toggle(
                                Table.System,
                                "ls_clock_hide",
                                R.string.row_hide_clock,
                                R.string.row_hide_clock_summary,
                                icon = R.drawable.materialsymbols_ic_visibility_off_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "lockscreen_battery_info",
                                R.string.row_charging_info,
                                R.string.row_charging_info_summary,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_battery_charging_full_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "enable_ripple_effect",
                                R.string.row_ripple,
                                R.string.row_ripple_summary,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_fingerprint_rounded_filled,
                            ),
                            LabRow.Choice(
                                Table.System,
                                "lockscreen_display_timeout",
                                R.string.row_lock_timeout,
                                R.array.lock_timeout_labels,
                                listOf("10000", "15000", "30000", "60000", "120000", "300000", "600000", "1800000"),
                                "10000",
                                R.drawable.materialsymbols_ic_lock_clock_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_lock_more,
                        listOf(
                            LabRow.Link(
                                "$S.lockscreen.LockScreenWeather",
                                R.string.row_weather,
                                R.string.row_weather_summary,
                                R.drawable.materialsymbols_ic_partly_cloudy_day_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.display.MediaArtSettings",
                                R.string.row_media_art,
                                R.string.row_media_art_summary,
                                R.drawable.materialsymbols_ic_album_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.display.PulseSettings",
                                R.string.row_pulse,
                                R.string.row_pulse_summary,
                                R.drawable.materialsymbols_ic_equalizer_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.lockscreen.WallpaperDepth",
                                R.string.row_depth,
                                R.string.row_depth_summary,
                                R.drawable.materialsymbols_ic_layers_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.themes.LockGlympsSettings",
                                R.string.row_glymps,
                                R.string.row_glymps_summary,
                                R.drawable.materialsymbols_ic_wallpaper_slideshow_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_lock_security,
                        listOf(
                            LabRow.Toggle(
                                Table.System,
                                "lockscreen_enable_power_menu",
                                R.string.row_lock_power_menu,
                                R.string.row_lock_power_menu_summary,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_power_settings_new_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.Secure,
                                "enable_lockscreen_quick_settings",
                                R.string.row_lock_qs,
                                R.string.row_lock_qs_summary,
                                icon = R.drawable.materialsymbols_ic_block_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.Secure,
                                "qstile_requires_unlocking",
                                R.string.row_lock_tiles,
                                R.string.row_lock_tiles_summary,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_security_rounded_filled,
                            ),
                        ),
                    ),
                ),
            ),
            LabTab(
                R.string.tab_gestures,
                R.string.tab_gestures_title,
                R.drawable.materialsymbols_ic_swipe_rounded_filled,
                listOf(
                    LabSection(
                        R.string.section_gestures,
                        listOf(
                            LabRow.Toggle(
                                Table.Secure,
                                "double_tap_to_sleep",
                                R.string.row_double_tap_sleep,
                                R.string.row_double_tap_sleep_summary,
                                icon = R.drawable.materialsymbols_ic_touch_app_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.gestures.OffscreenGestureSettings",
                                R.string.row_offscreen,
                                R.string.row_offscreen_summary,
                                R.drawable.materialsymbols_ic_gesture_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.gestures.quicklaunch.QuickLaunchSettings",
                                R.string.row_quick_launch,
                                R.string.row_quick_launch_summary,
                                R.drawable.materialsymbols_ic_rocket_launch_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "screenshot_clipboard_only",
                                R.string.row_screenshot_clipboard,
                                R.string.row_screenshot_clipboard_summary,
                                icon = R.drawable.materialsymbols_ic_content_paste_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_buttons,
                        listOf(
                            LabRow.Toggle(
                                Table.System,
                                "volume_rocker_wake",
                                R.string.row_volume_wake,
                                R.string.row_volume_wake_summary,
                                icon = R.drawable.materialsymbols_ic_volume_up_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.gestures.PowerMenuSettings",
                                R.string.row_power_menu,
                                R.string.row_power_menu_summary,
                                R.drawable.materialsymbols_ic_power_settings_new_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_navigation,
                        listOf(
                            LabRow.Choice(
                                Table.Secure,
                                "navbar_layout_mode",
                                R.string.row_navbar_layout,
                                R.array.navbar_layout_labels,
                                listOf("0", "1", "2", "3"),
                                "0",
                                R.drawable.materialsymbols_ic_call_to_action_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.Secure,
                                "back_gesture_arrow",
                                R.string.row_back_arrow,
                                R.string.row_back_arrow_summary,
                                default = true,
                            ),
                            LabRow.Toggle(
                                Table.Secure,
                                "back_gesture_haptic",
                                R.string.row_back_haptic,
                                R.string.row_back_haptic_summary,
                                default = true,
                            ),
                            LabRow.Toggle(
                                Table.Secure,
                                "sysui_show_nav_bar_ime",
                                R.string.row_ime_space,
                                R.string.row_ime_space_summary,
                                default = true,
                            ),
                            LabRow.Link(
                                "$S.gestures.GestureTweaksSettings",
                                R.string.row_back_tweaks,
                                R.string.row_back_tweaks_summary,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_taskbar,
                        listOf(
                            LabRow.Toggle(
                                Table.System,
                                "enable_taskbar",
                                R.string.row_taskbar,
                                R.string.row_taskbar_summary,
                                icon = R.drawable.materialsymbols_ic_dock_to_bottom_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.custom.taskbar.TaskbarSettings",
                                R.string.row_taskbar_more,
                                R.string.row_taskbar_more_summary,
                            ),
                        ),
                        largeScreenOnly = true,
                    ),
                ),
            ),
            LabTab(
                R.string.tab_sound,
                R.string.tab_sound_title,
                R.drawable.materialsymbols_ic_graphic_eq_rounded_filled,
                listOf(
                    LabSection(
                        R.string.section_volume,
                        listOf(
                            LabRow.Toggle(
                                Table.System,
                                "show_app_volume",
                                R.string.row_app_volume,
                                R.string.row_app_volume_summary,
                                icon = R.drawable.materialsymbols_ic_tune_rounded_filled,
                            ),
                            LabRow.Choice(
                                Table.Secure,
                                "volume_panel_on_left",
                                R.string.row_volume_side,
                                R.array.volume_side_labels,
                                listOf("0", "1"),
                                "0",
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "multi_audio_focus_enabled",
                                R.string.row_multi_focus,
                                R.string.row_multi_focus_summary,
                                icon = R.drawable.materialsymbols_ic_queue_music_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "screenshot_shutter_sound",
                                R.string.row_screenshot_sound,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_screenshot_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_haptics,
                        listOf(
                            LabRow.Toggle(
                                Table.System,
                                "fp_success_vibrate",
                                R.string.row_fp_success,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_fingerprint_rounded_filled,
                            ),
                            LabRow.Toggle(Table.System, "fp_error_vibrate", R.string.row_fp_error, default = true),
                            LabRow.Toggle(
                                Table.System,
                                "vibrate_on_connect",
                                R.string.row_vibrate_connect,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_vibration_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "vibrate_on_callwaiting",
                                R.string.row_vibrate_waiting,
                                default = true,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "vibrate_on_disconnect",
                                R.string.row_vibrate_disconnect,
                                default = true,
                            ),
                            LabRow.Link(
                                "$S.sound.CustomVibrationPreferenceFragment",
                                R.string.row_vibration_patterns,
                                R.string.row_vibration_patterns_summary,
                            ),
                        ),
                    ),
                ),
            ),
            LabTab(
                R.string.tab_system,
                R.string.tab_system_title,
                R.drawable.materialsymbols_ic_display_settings_rounded_filled,
                listOf(
                    LabSection(
                        R.string.section_theme,
                        listOf(
                            LabRow.Link(
                                "$S.system.ThemeSettingsFragment",
                                R.string.row_theme,
                                R.string.row_theme_summary,
                                R.drawable.materialsymbols_ic_palette_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.custom.font.FontSettings",
                                R.string.row_fonts,
                                R.string.row_fonts_summary,
                                R.drawable.materialsymbols_ic_font_download_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.custom.monet.MonetSettings",
                                R.string.row_monet,
                                R.string.row_monet_summary,
                                R.drawable.materialsymbols_ic_format_paint_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_display,
                        listOf(
                            LabRow.Link(
                                "$S.custom.smartpixel.SmartPixelsSettings",
                                R.string.row_smart_pixels,
                                R.string.row_smart_pixels_summary,
                                R.drawable.materialsymbols_ic_blur_on_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.display.ColorModePreferenceFragment",
                                R.string.row_display_engine,
                                R.string.row_display_engine_summary,
                                R.drawable.materialsymbols_ic_contrast_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.Global,
                                "wake_when_plugged_or_unplugged",
                                R.string.row_wake_plug,
                                R.string.row_wake_plug_summary,
                                icon = R.drawable.materialsymbols_ic_power_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_battery,
                        listOf(
                            LabRow.Link(
                                "$S.fuelgauge.IdleManagerSettings",
                                R.string.row_idle_manager,
                                R.string.row_idle_manager_summary,
                                R.drawable.materialsymbols_ic_hourglass_bottom_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.fuelgauge.sleepmode.SleepMode",
                                R.string.row_sleep_mode,
                                R.string.row_sleep_mode_summary,
                                R.drawable.materialsymbols_ic_bedtime_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.fuelgauge.powerinsight.PowerInsightSettings",
                                R.string.row_power_insight,
                                R.string.row_power_insight_summary,
                                R.drawable.materialsymbols_ic_monitoring_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_spoofing,
                        listOf(
                            LabRow.Link(
                                "$S.custom.obscura.ObscuraAppSettings",
                                R.string.row_obscura,
                                R.string.row_obscura_summary,
                                R.drawable.materialsymbols_ic_verified_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.custom.spoofing.PlayIntegrityFix",
                                R.string.row_pif,
                                R.string.row_pif_summary,
                                R.drawable.materialsymbols_ic_verified_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.custom.spoofing.TrickyStore",
                                R.string.row_tricky,
                                R.string.row_tricky_summary,
                                R.drawable.materialsymbols_ic_vpn_key_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.custom.spoofing.GameSpoofing",
                                R.string.row_games,
                                R.string.row_games_summary,
                                R.drawable.materialsymbols_ic_sports_esports_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.system.spoofing.UserSelectedAppSpoofSettings",
                                R.string.row_app_spoof,
                                R.string.row_app_spoof_summary,
                                R.drawable.materialsymbols_ic_devices_rounded_filled,
                            ),
                            LabRow.Link(
                                "$S.system.spoofing.AdvancedAppSpoofSettings",
                                R.string.row_hw_spoof,
                                R.string.row_hw_spoof_summary,
                                R.drawable.materialsymbols_ic_memory_rounded_filled,
                            ),
                        ),
                    ),
                    LabSection(
                        R.string.section_system_more,
                        listOf(
                            LabRow.Toggle(
                                Table.Global,
                                "window_ignore_secure",
                                R.string.row_secure_screenshots,
                                R.string.row_secure_screenshots_summary,
                                icon = R.drawable.materialsymbols_ic_screenshot_monitor_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.Global,
                                "pm_downgrade_allowed",
                                R.string.row_downgrade,
                                R.string.row_downgrade_summary,
                                icon = R.drawable.materialsymbols_ic_install_mobile_rounded_filled,
                            ),
                            LabRow.Toggle(
                                Table.System,
                                "usb_mode_picker_dialog",
                                R.string.row_usb_picker,
                                R.string.row_usb_picker_summary,
                                default = true,
                                icon = R.drawable.materialsymbols_ic_usb_rounded_filled,
                            ),
                        ),
                    ),
                ),
            ),
        )
}
