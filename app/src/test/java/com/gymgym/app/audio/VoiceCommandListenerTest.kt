package com.gymgym.app.audio

import com.gymgym.app.audio.VoiceCommandListener.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Locks the command disambiguation, whose ordering is subtle. */
class VoiceCommandListenerTest {

    private fun cmd(text: String) = VoiceCommandListener.commandFor(text)

    @Test
    fun recordingBeatsBareStartStop() {
        assertEquals(VoiceCommand.START_RECORDING, cmd("start recording"))
        assertEquals(VoiceCommand.START_RECORDING, cmd("record"))
        assertEquals(VoiceCommand.STOP_RECORDING, cmd("stop recording"))
        assertEquals(VoiceCommand.STOP_RECORDING, cmd("end recording"))
    }

    @Test
    fun timerStartStop() {
        assertEquals(VoiceCommand.START_TIMER, cmd("start timer"))
        assertEquals(VoiceCommand.START_TIMER, cmd("start"))
        assertEquals(VoiceCommand.START_TIMER, cmd("begin"))
        assertEquals(VoiceCommand.STOP_TIMER, cmd("stop timer"))
        assertEquals(VoiceCommand.STOP_TIMER, cmd("stop"))
        assertEquals(VoiceCommand.STOP_TIMER, cmd("done"))
    }

    @Test
    fun switchCamera() {
        assertEquals(VoiceCommand.SWITCH_CAMERA, cmd("switch camera"))
        assertEquals(VoiceCommand.SWITCH_CAMERA, cmd("flip camera"))
        assertEquals(VoiceCommand.SWITCH_CAMERA, cmd("flip"))
    }

    @Test
    fun restartIsResetNotStart() {
        assertEquals(VoiceCommand.RESET, cmd("restart"))
        assertEquals(VoiceCommand.RESET, cmd("reset"))
        assertEquals(VoiceCommand.RESET, cmd("again"))
    }

    @Test
    fun existingWorkoutCommands() {
        assertEquals(VoiceCommand.NEXT, cmd("next"))
        assertEquals(VoiceCommand.NEXT, cmd("skip"))
        assertEquals(VoiceCommand.PAUSE, cmd("pause"))
        assertEquals(VoiceCommand.RESUME, cmd("resume"))
    }

    @Test
    fun unknownIsNull() {
        assertNull(cmd("hello there"))
        assertNull(cmd(""))
    }

    // --- Localized vocabularies ---

    private fun cmd(text: String, tag: String) =
        VoiceCommandListener.commandFor(text, CommandVocabulary.forLanguageTag(tag))

    @Test
    fun spanishCommands() {
        assertEquals(VoiceCommand.START_TIMER, cmd("empieza", "es"))
        assertEquals(VoiceCommand.STOP_TIMER, cmd("para", "es"))
        assertEquals(VoiceCommand.PAUSE, cmd("pausa", "es"))
        assertEquals(VoiceCommand.NEXT, cmd("siguiente", "es"))
        assertEquals(VoiceCommand.SWITCH_CAMERA, cmd("cambia la cámara", "es"))
        assertEquals(VoiceCommand.START_RECORDING, cmd("graba", "es"))
        assertEquals(VoiceCommand.STOP_RECORDING, cmd("para de grabar", "es"))
    }

    @Test
    fun russianCommands() {
        assertEquals(VoiceCommand.START_TIMER, cmd("начать", "ru"))
        assertEquals(VoiceCommand.STOP_TIMER, cmd("стоп", "ru"))
        assertEquals(VoiceCommand.PAUSE, cmd("пауза", "ru"))
        assertEquals(VoiceCommand.RESET, cmd("заново", "ru"))
        // "запись" must read as recording, not as the start/stop timer.
        assertEquals(VoiceCommand.START_RECORDING, cmd("начать запись", "ru"))
        assertEquals(VoiceCommand.STOP_RECORDING, cmd("останови запись", "ru"))
    }

    @Test
    fun chineseCommandsAvoidStopPauseCollision() {
        assertEquals(VoiceCommand.START_TIMER, cmd("开始", "zh-CN"))
        assertEquals(VoiceCommand.STOP_TIMER, cmd("停止", "zh-CN"))
        // 暂停 (pause) contains 停 but must not be swallowed by the stop rule.
        assertEquals(VoiceCommand.PAUSE, cmd("暂停", "zh-CN"))
        assertEquals(VoiceCommand.SWITCH_CAMERA, cmd("切换摄像头", "zh-CN"))
        assertEquals(VoiceCommand.NEXT, cmd("下一个", "zh-CN"))
        assertEquals(VoiceCommand.STOP_RECORDING, cmd("停止录像", "zh-CN"))
    }

    @Test
    fun englishStillWorksInAnyLanguage() {
        // English terms are merged into every language's vocabulary.
        assertEquals(VoiceCommand.START_TIMER, cmd("start", "ar"))
        assertEquals(VoiceCommand.SWITCH_CAMERA, cmd("switch camera", "zh-CN"))
    }

    @Test
    fun arabicCommands() {
        assertEquals(VoiceCommand.START_TIMER, cmd("ابدأ", "ar"))
        assertEquals(VoiceCommand.STOP_TIMER, cmd("توقف", "ar"))
        assertEquals(VoiceCommand.SWITCH_CAMERA, cmd("بدل الكاميرا", "ar"))
        assertEquals(VoiceCommand.START_RECORDING, cmd("سجل", "ar"))
    }
}
