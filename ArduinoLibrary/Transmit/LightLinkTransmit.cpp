#include "LightLinkTransmit.h"

// Constructor to initialize the analog pin, LED pin, communication speed, and maxSensorValue
LightLinkTransmit::LightLinkTransmit(int analogPin, int ledPin, int commsSpeed, int maxSensorValue) {
  _analogPin = analogPin;
  _ledPin = ledPin;
  _commsSpeed = commsSpeed;
  _maxSensorValue = maxSensorValue;

  pinMode(_ledPin, OUTPUT);  // Initialize the LED pin as an output
  pinMode(_analogPin, INPUT);  // Initialize the analog input pin
}

void LightLinkTransmit::ledout(int on_time) {
  int off_time = 100 - on_time;
  if (on_time == 0) {
    digitalWrite(_ledPin, LOW);
    delay(_commsSpeed);
  } else if (off_time == 0) {
    digitalWrite(_ledPin, HIGH);
    delay(_commsSpeed);
  } else {
    int delaytime = _commsSpeed * 10;
    for (int i = 0; i < delaytime; i++) {
      digitalWrite(_ledPin, HIGH);
      delayMicroseconds(on_time);
      digitalWrite(_ledPin, LOW);
      delayMicroseconds(off_time);
    }
  }
}

void LightLinkTransmit::transmitAnalogData() {
  int sensorValue = analogRead(_analogPin);  // Read the analog sensor value
  int mappedValue = map(sensorValue, 0, _maxSensorValue, 5, 95);  // Map to 5-95 range using maxSensorValue
  ledout(95);  // Transmit a baseline value
  ledout(mappedValue);  // Transmit the mapped sensor value
}
