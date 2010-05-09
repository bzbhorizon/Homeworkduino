int r = 0;

void setup() {
  Serial.begin(9600);
  for (int i = 2; i < 13; i++) {
    pinMode(i, OUTPUT);
  }
  
}

void loop() {
  if (Serial.available() > 0) {
     digitalWrite(r, LOW);
     r = Serial.read() - 46;
     Serial.println(r);
     digitalWrite(r, HIGH);
  }
}


