package br.tiagohm.kurubo.client

inline val Int.bit0
    get() = (this and 0x01) != 0

inline val Int.bit1
    get() = (this and 0x02) != 0

inline val Int.bit2
    get() = (this and 0x04) != 0

inline val Int.bit3
    get() = (this and 0x08) != 0

inline val Int.bit4
    get() = (this and 0x10) != 0

inline val Int.bit5
    get() = (this and 0x20) != 0

inline val Int.bit6
    get() = (this and 0x40) != 0

inline val Int.bit7
    get() = (this and 0x80) != 0

inline val Int.loByte
    get() = this and 0xFF

inline val Int.hiByte
    get() = this shr 8 and 0xFF

inline val Int.higherByte
    get() = this shr 16 and 0xFF

inline val Int.highestByte
    get() = this shr 24 and 0xFF
