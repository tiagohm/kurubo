package br.tiagohm.kurubo.client

import br.tiagohm.kurubo.client.protocol.PIN_MODE_IGNORE
import br.tiagohm.kurubo.client.protocol.TOTAL_PIN_MODES

enum class PinMode {
    INPUT,
    OUTPUT,
    ANALOG,
    PWM,
    SERVO,
    SHIFT,
    I2C,
    ONE_WIRE,
    STEPPER,
    ENCODER,
    SERIAL,
    PULL_UP,
    UNSUPPORTED,
    IGNORED;

    companion object {

        fun resolve(modeToken: Int): PinMode {
            return if (modeToken == PIN_MODE_IGNORE) IGNORED
            else if (modeToken == TOTAL_PIN_MODES) UNSUPPORTED
            else entries[modeToken]
        }
    }
}
