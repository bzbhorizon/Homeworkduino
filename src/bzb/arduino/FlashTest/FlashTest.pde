#include "Rainbow.h"

int NUMBER_OF_ROWS = 3;
int buffer[9];
int nextSlot = 0;
//boolean delayNext = true;

void setup() {
	_init();
	
	close_all_line;
	//open_line0; // 8 LEDs per line; we want 20 LEDs, so we need 3 lines
	//open_line1;
	//open_line2;
	/*open_line3;
	open_line4;
	open_line5;
	open_line6;
	open_line7;*/
	
	Serial.begin(115200);
}

boolean failed = false;

void loop() {
  //delayNext = true;
	while (Serial.available() > 0 && nextSlot < 9) {
		buffer[nextSlot] = Serial.read();
                Serial.println(buffer[nextSlot]);
		nextSlot++;
   // delayNext = false;
	}

   if (nextSlot == 9) {
  		Serial.println("s");
		nextSlot = 0;
	} else if (nextSlot > 0) {
                Serial.println("f");
                failed = true;
		nextSlot = 0;
	}

  if (!failed) {
    close_all_line;
  
    for (int i = 0; i < NUMBER_OF_ROWS; i++) {
    
      if (i == 0){
                          open_line0;
                        } else if (i == 1) {
                          open_line1;
                        } else if (i == 2) {
                          open_line2;
                        }  
			shift_24_bit(buffer[i * 3], buffer[i * 3 + 1], buffer[i * 3 + 2]);
                        delay(5);
		}
    
  }
}

void _init(void) {// define the pin mode
  DDRD=0xff;
  DDRC=0xff;
  DDRB=0xff;
  PORTD=0;
  PORTB=0;
}

void shift_1_bit(unsigned char LS) {
  if(LS) {shift_data_1;}
  else {shift_data_0;}
  clk_rising;
}

void shift_24_bit(int Red,int Green,int Blue) {
  unsigned char i;
  le_high;
  for (i=0;i<8;i++)
  {
    if ((Green<<i)&0x80)   shift_1_bit(1);
    else                 shift_1_bit(0);

  }
  for (i=0;i<8;i++)
  {
    if ((Red<<i)&0x80) shift_1_bit(1);
    else          shift_1_bit(0);
  }
  for (i=0;i<8;i++)
  {
    if ((Blue<<i)&0x80)  shift_1_bit(1);
    else             shift_1_bit(0);
  } 
  le_low;
}
