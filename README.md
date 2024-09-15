# Kurubo

A Firmata Client written in Kotlin.

## Install

## Client

```kotlin
val transport = SerialTransport("/dev/ttyUSB0")
// val transport = NetworkTransport("192.168.31.137", 27016)
val arduino = ArduinoUno(transport)

arduino.run()
arduino.ensureInitializationIsDone()

val am2320 = AM2320(arduino)
am2320.addThermometerListener { println(it.temperature) }
am2320.addHygrometerListener { println(it.humidity) }
```
