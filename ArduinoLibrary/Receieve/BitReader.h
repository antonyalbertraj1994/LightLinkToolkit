#ifndef BITREADER_H
#define BITREADER_H

#include <Arduino.h>

class BitReader {
public:
    BitReader(int pin, int threshold, int bitLength);
    int* readBits();

private:
    int _pin;
    int _threshold;
    int _bitLength;
    int _bits[8];
};

#endif
