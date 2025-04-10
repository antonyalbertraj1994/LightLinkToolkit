#include "BitReader.h"

BitReader::BitReader(int pin, int threshold, int bitLength) {
    _pin = pin;
    _threshold = threshold;
    _bitLength = bitLength;
}

int* BitReader::readBits() {
    int delayms_half = _bitLength / 2;

    // Wait for signal to go LOW
    while (analogRead(_pin) > _threshold) {}

    delay(delayms_half);

    for (int i = 0; i < 8; i++) {
        delay(_bitLength);
        int f = analogRead(_pin);
        if (f > _threshold) {
            _bits[i] = 1;
        } else {
            _bits[i] = 0;
        }
    }

    delay(_bitLength);

    // Wait for signal to go HIGH
    while (analogRead(_pin) <= _threshold) {}

    return _bits;
}
