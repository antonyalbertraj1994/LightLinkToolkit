#include <LightLinkTransmit.h>

// Define the analog pin, LED pin, communication speed, and max sensor value
LightLinkTransmit lightController(A0, 13, 100, 1023);  // Analog pin (A0), LED pin (13), commsSpeed (100), maxSensorValue (1023)

void setup() {
  // Initialize serial communication (optional, for debugging)
  Serial.begin(9600);
}

void loop() {
  lightController.transmitAnalogData();  // Transmit the analog data using the LED
  delay(1000);  // Delay to avoid overloading the loop
}
