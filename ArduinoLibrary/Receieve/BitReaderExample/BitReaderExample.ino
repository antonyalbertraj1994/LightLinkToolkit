#include <BitReader.h>

BitReader reader(A0, 500, 50); // pin, threshold, bit length in ms

void setup() {
  Serial.begin(9600);
}

void loop() {
  int* bits = reader.readBits();
  Serial.print("Received: ");
  for (int i = 0; i < 8; i++) {
    Serial.print(bits[i]);
  }
  Serial.println();
  delay(1000);
}
