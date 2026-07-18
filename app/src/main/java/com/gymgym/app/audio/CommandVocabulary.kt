package com.gymgym.app.audio

/**
 * Keyword sets used to map recognized speech to a [VoiceCommandListener.VoiceCommand].
 *
 * Matching is substring-based (see `commandFor`), so entries are word stems where
 * that helps inflected languages (e.g. Russian "запис" matches записать/запись).
 * A language's vocabulary is its own words merged with [ENGLISH], so English
 * fitness terms keep working regardless of the selected language.
 *
 * All entries are lowercase; recognized text is lowercased before matching.
 */
data class CommandVocabulary(
    /** "record" — start/stop video recording (STOP_RECORDING when a stop modifier is also present). */
    val record: List<String>,
    /** Words that turn an action into its "stop" variant (used to split record → stop-recording). */
    val stopModifiers: List<String>,
    val switchCamera: List<String>,
    val next: List<String>,
    val reset: List<String>,
    val resume: List<String>,
    val start: List<String>,
    val stop: List<String>,
    val pause: List<String>,
) {
    private fun mergedWith(o: CommandVocabulary) = CommandVocabulary(
        record = record + o.record,
        stopModifiers = stopModifiers + o.stopModifiers,
        switchCamera = switchCamera + o.switchCamera,
        next = next + o.next,
        reset = reset + o.reset,
        resume = resume + o.resume,
        start = start + o.start,
        stop = stop + o.stop,
        pause = pause + o.pause,
    )

    companion object {
        val ENGLISH = CommandVocabulary(
            record = listOf("record"),
            stopModifiers = listOf("stop", "end", "finish", "cancel"),
            switchCamera = listOf("switch", "flip", "camera"),
            next = listOf("next", "skip"),
            reset = listOf("reset", "restart", "again"),
            resume = listOf("resume", "continue", "unpause"),
            start = listOf("start", "begin"),
            stop = listOf("stop", "finish", "done", "end"),
            pause = listOf("pause", "wait", "hold"),
        )

        /** Localized synonyms only; merged onto [ENGLISH] via [forLanguageTag]. */
        private val RUSSIAN = CommandVocabulary(
            record = listOf("запис"),
            stopModifiers = listOf("стоп", "останов", "заверш", "отмен"),
            switchCamera = listOf("камер", "переключ", "смени"),
            next = listOf("след", "пропуст", "дальше"),
            reset = listOf("сброс", "заново", "снова", "перезапуск"),
            resume = listOf("продолж", "возобнов"),
            start = listOf("старт", "нача", "начн", "поехали", "запуск"),
            stop = listOf("стоп", "останов", "заверш", "хватит"),
            pause = listOf("пауз", "погоди", "подожд", "стой"),
        )

        private val SPANISH = CommandVocabulary(
            record = listOf("graba", "grabación"),
            stopModifiers = listOf("para", "detén", "termina", "cancela"),
            switchCamera = listOf("cámara", "camara", "cambia", "voltea", "gira"),
            next = listOf("siguiente", "salta", "próximo", "proximo"),
            reset = listOf("reinicia", "otra vez", "de nuevo"),
            resume = listOf("reanuda", "continúa", "continua", "sigue"),
            start = listOf("empieza", "inicia", "comienza", "arranca", "vamos"),
            stop = listOf("para", "detén", "termina", "alto", "basta"),
            pause = listOf("pausa", "espera", "aguanta"),
        )

        private val CHINESE = CommandVocabulary(
            record = listOf("录", "录制", "录像"),
            stopModifiers = listOf("停", "结束", "取消", "停止"),
            switchCamera = listOf("切换", "翻转", "摄像", "相机", "镜头"),
            next = listOf("下一", "跳过", "下个"),
            reset = listOf("重置", "重来", "重新", "再来"),
            resume = listOf("继续", "恢复"),
            start = listOf("开始", "启动", "计时"),
            stop = listOf("停止", "停下", "结束", "完成"),
            pause = listOf("暂停", "稍等", "等一"),
        )

        private val FRENCH = CommandVocabulary(
            record = listOf("enregistre", "enregistrement"),
            stopModifiers = listOf("arrête", "arrete", "termine", "annule"),
            switchCamera = listOf("caméra", "camera", "change", "bascule", "retourne"),
            next = listOf("suivant", "passe", "saute", "prochain"),
            reset = listOf("réinitialise", "recommence", "encore"),
            resume = listOf("reprends", "continue"),
            start = listOf("démarre", "demarre", "commence", "lance"),
            stop = listOf("arrête", "arrete", "termine", "fini"),
            pause = listOf("pause", "attends", "patiente"),
        )

        private val LATVIAN = CommandVocabulary(
            record = listOf("ieraksti", "ierakst"),
            stopModifiers = listOf("aptur", "beidz", "pārtrauc", "atcel"),
            switchCamera = listOf("kamer", "pārslēdz", "parsledz", "maini", "apgriez"),
            next = listOf("nākam", "nakam", "izlaid", "tālāk", "talak"),
            reset = listOf("atiestat", "vēlreiz", "velreiz", "no jauna"),
            resume = listOf("turpin", "atsāc"),
            start = listOf("sākt", "sāci", "uzsāc", "palaid", "sākums"),
            stop = listOf("aptur", "beidz", "pabeidz"),
            pause = listOf("pauze", "pagaidi", "uzgaidi"),
        )

        private val ARABIC = CommandVocabulary(
            record = listOf("سجل", "تسجيل"),
            stopModifiers = listOf("أوقف", "إيقاف", "توقف", "إلغاء", "قف"),
            switchCamera = listOf("كاميرا", "بدل", "غير", "اقلب"),
            next = listOf("التالي", "تخطى", "تخطي", "تجاوز"),
            reset = listOf("إعادة", "أعد", "جديد", "أخرى"),
            resume = listOf("استأنف", "واصل", "أكمل", "تابع"),
            start = listOf("ابدأ", "بدء", "شغل", "هيا", "انطلق"),
            stop = listOf("أوقف", "إيقاف", "توقف"),
            pause = listOf("مؤقت", "انتظر", "مهلا", "تمهل"),
        )

        /** Vocabulary for a BCP-47 tag: the language's synonyms merged onto English. */
        fun forLanguageTag(tag: String): CommandVocabulary {
            val extra = when (tag.substringBefore('-').lowercase()) {
                "ru" -> RUSSIAN
                "es" -> SPANISH
                "zh" -> CHINESE
                "fr" -> FRENCH
                "lv" -> LATVIAN
                "ar" -> ARABIC
                else -> return ENGLISH
            }
            return ENGLISH.mergedWith(extra)
        }
    }
}
