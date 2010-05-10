int r = 0;

void setup() {
  Serial.begin(9600);
  for (int i = 2; i < 13; i++) {// for testing
    pinMode(i, OUTPUT);
  }
  
}

void loop() {
  if (Serial.available() > 0) {
     digitalWrite(r, LOW);
     r = Serial.read();
     Serial.println(r);
     digitalWrite(r + 2, HIGH); // for testing
  }
}


