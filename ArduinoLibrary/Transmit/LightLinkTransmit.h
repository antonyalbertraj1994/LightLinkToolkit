#ifndef LightLinkTransmit_h
#define LightLinkTransmit_h

#include <Arduino.h>

class LightLinkTransmit {
  public:
    // Constructor to set analogPin, ledPin, commsSpeed, and maxSensorValue
    LightLinkTransmit(int analogPin, int ledPin, int commsSpeed, int maxSensorValue);

    // Function to transmit analog data using LED
    void transmitAnalogData();

    // Function to control LED behavior
    void ledout(int on_time);

  private:
    int _analogPin;      // Analog input pin
    int _ledPin;         // LED output pin
    int _commsSpeed;     // Communication speed (delay between actions)
    int _maxSensorValue; // Maximum sensor value (for mapping)
};

#endif
