package com.gymgym.app.ui

import androidx.annotation.StringRes
import com.gymgym.app.R
import com.gymgym.app.data.BodyMetric
import com.gymgym.app.profile.LengthUnit
import com.gymgym.app.profile.TrainingMode
import com.gymgym.app.profile.WeightUnit
import com.gymgym.app.settings.AccentTheme
import com.gymgym.app.settings.BackgroundStyle
import com.gymgym.app.settings.FormSensitivity
import com.gymgym.app.settings.RepAnnouncementMode

/**
 * Maps enums to their localized display strings. The enums keep storing/serializing
 * by [Enum.name]; only the on-screen/spoken label is localized here.
 */

@StringRes
fun Exercise.labelRes(): Int = when (this) {
    Exercise.SQUAT -> R.string.exercise_squat
    Exercise.PUSHUP -> R.string.exercise_pushup
    Exercise.PULLUP -> R.string.exercise_pullup
    Exercise.DUMBBELL_PRESS -> R.string.exercise_dumbbell_press
    Exercise.PLANK -> R.string.exercise_plank
}

@StringRes
fun Exercise.framingTipRes(): Int = when (this) {
    Exercise.SQUAT, Exercise.PUSHUP, Exercise.PLANK -> R.string.framing_side
    Exercise.PULLUP -> R.string.framing_pullup
    Exercise.DUMBBELL_PRESS -> R.string.framing_press
}

@StringRes
fun RepAnnouncementMode.labelRes(): Int = when (this) {
    RepAnnouncementMode.OFF -> R.string.rep_off
    RepAnnouncementMode.EVERY_REP -> R.string.rep_every
    RepAnnouncementMode.EVERY_5 -> R.string.rep_every_5
    RepAnnouncementMode.EVERY_10 -> R.string.rep_every_10
}

@StringRes
fun FormSensitivity.labelRes(): Int = when (this) {
    FormSensitivity.LENIENT -> R.string.sensitivity_lenient
    FormSensitivity.STANDARD -> R.string.sensitivity_standard
    FormSensitivity.STRICT -> R.string.sensitivity_strict
}

@StringRes
fun AccentTheme.labelRes(): Int = when (this) {
    AccentTheme.EMERALD -> R.string.accent_emerald
    AccentTheme.AZURE -> R.string.accent_azure
    AccentTheme.VIOLET -> R.string.accent_violet
    AccentTheme.MAGENTA -> R.string.accent_magenta
    AccentTheme.AMBER -> R.string.accent_amber
    AccentTheme.CRIMSON -> R.string.accent_crimson
    AccentTheme.LIME -> R.string.accent_lime
    AccentTheme.AQUA -> R.string.accent_aqua
    AccentTheme.TANGERINE -> R.string.accent_tangerine
    AccentTheme.ROSE -> R.string.accent_rose
    AccentTheme.INDIGO -> R.string.accent_indigo
    AccentTheme.ORCHID -> R.string.accent_orchid
    AccentTheme.CYAN -> R.string.accent_cyan
    AccentTheme.SLATE -> R.string.accent_slate
}

@StringRes
fun BackgroundStyle.labelRes(): Int = when (this) {
    BackgroundStyle.NONE -> R.string.bg_none
    BackgroundStyle.GYM_EMERALD -> R.string.bg_gym_emerald
    BackgroundStyle.GYM_AZURE -> R.string.bg_gym_azure
    BackgroundStyle.GYM_VIOLET -> R.string.bg_gym_violet
    BackgroundStyle.GYM_AMBER -> R.string.bg_gym_amber
    BackgroundStyle.CUSTOM -> R.string.bg_custom
}

@StringRes
fun WeightUnit.labelRes(): Int = when (this) {
    WeightUnit.KG -> R.string.unit_kg
    WeightUnit.LB -> R.string.unit_lb
}

@StringRes
fun LengthUnit.labelRes(): Int = when (this) {
    LengthUnit.CM -> R.string.unit_cm
    LengthUnit.IN -> R.string.unit_in
}

@StringRes
fun TrainingMode.labelRes(): Int = when (this) {
    TrainingMode.SMART_CYCLE -> R.string.profile_mode_smart
    TrainingMode.WEEKLY_SCHEDULE -> R.string.profile_mode_weekly
}

@StringRes
fun BodyMetric.labelRes(): Int = when (this) {
    BodyMetric.WEIGHT -> R.string.metric_weight
    BodyMetric.ARM -> R.string.metric_arm
    BodyMetric.LEG -> R.string.metric_leg
    BodyMetric.CHEST -> R.string.metric_chest
    BodyMetric.SHOULDERS -> R.string.metric_shoulders
    BodyMetric.CALVES -> R.string.metric_calves
    BodyMetric.WAIST -> R.string.metric_waist
}

@StringRes
fun DateRange.labelRes(): Int = when (this) {
    DateRange.ALL -> R.string.range_all
    DateRange.DAY -> R.string.range_24h
    DateRange.WEEK -> R.string.range_7d
    DateRange.MONTH -> R.string.range_30d
}
