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
}
