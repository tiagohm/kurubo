@file:JvmName("FirmataProtocol")

package br.tiagohm.kurubo.client.protocol

// Message command bytes (128-255/0x80-0xFF).
const val DIGITAL_MESSAGE = 0x90
const val ANALOG_MESSAGE = 0xE0
const val REPORT_ANALOG = 0xC0
const val REPORT_DIGITAL = 0xD0
const val SET_PIN_MODE = 0xF4
const val SET_DIGITAL_PIN_VALUE = 0xF5
const val REPORT_VERSION = 0xF9
const val SYSTEM_RESET = 0xFF
const val START_SYSEX = 0xF0
const val END_SYSEX = 0xF7

// Extended command set using sysex (0-127/0x00-0x7F)
// 0x00-0x0F reserved for user-defined commands
const val RESERVED_COMMAND = 0x00
const val SERIAL_MESSAGE = 0x60
const val ENCODER_DATA = 0x61
const val SERVO_CONFIG = 0x70
const val STRING_DATA = 0x71
const val STEPPER_DATA = 0x72
const val ONE_WIRE_DATA = 0x73
const val SHIFT_DATA = 0x75
const val TWO_WIRE_REQUEST = 0x76
const val TWO_WIRE_REPLY = 0x77
const val TWO_WIRE_CONFIG = 0x78
const val EXTENDED_ANALOG = 0x6F
const val PIN_STATE_QUERY = 0x6D
const val PIN_STATE_RESPONSE = 0x6E
const val CAPABILITY_QUERY = 0x6B
const val CAPABILITY_RESPONSE = 0x6C
const val ANALOG_MAPPING_QUERY = 0x69
const val ANALOG_MAPPING_RESPONSE = 0x6A
const val REPORT_FIRMWARE = 0x79
const val SAMPLING_INTERVAL = 0x7A
const val SCHEDULER_DATA = 0x7B
const val SYSEX_NON_REALTIME = 0x7E
const val SYSEX_REALTIME = 0x7F

// Pin modes.
const val PIN_MODE_INPUT = 0x00
const val PIN_MODE_OUTPUT = 0x01
const val PIN_MODE_ANALOG = 0x02
const val PIN_MODE_PWM = 0x03
const val PIN_MODE_SERVO = 0x04
const val PIN_MODE_SHIFT = 0x05
const val PIN_MODE_TWO_WIRE = 0x06
const val PIN_MODE_ONEWIRE = 0x07
const val PIN_MODE_STEPPER = 0x08
const val PIN_MODE_ENCODER = 0x09
const val PIN_MODE_SERIAL = 0x0A
const val PIN_MODE_PULLUP = 0x0B
const val PIN_MODE_IGNORE = 0x7F
const val TOTAL_PIN_MODES = 13

const val TWO_WIRE_WRITE = 0x00
const val TWO_WIRE_READ = 0x08
const val TWO_WIRE_READ_CONTINUOUS = 0x10
const val TWO_WIRE_STOP_READ_CONTINUOUS = 0x18

const val MIN_SAMPLING_INTERVAL = 10
const val MAX_SAMPLING_INTERVAL = 100

fun decode7Bit(data: ByteArray, offset: Int): Int {
    return (data[offset + 1].toInt() and 0x01 shl 7) or (data[offset].toInt() and 0x7F)
}
