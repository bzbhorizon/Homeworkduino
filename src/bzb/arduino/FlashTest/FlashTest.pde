#include "Rainbow.h"

void setup()
{
  _init();
  close_all_line;
  open_line0;
  open_line1;
  open_line2;
  open_line3;
  open_line4;
  open_line5;
  open_line6;
  open_line7;
  Serial.begin(9600);                
}
void loop()
{
  char data;
  if (Serial.available() > 0) {
    data = Serial.read();
    shift_24_bit(data,0,0);
  }
}
void _init(void)    // define the pin mode
{
  DDRD=0xff;
  DDRC=0xff;
  DDRB=0xff;
  PORTD=0;
  PORTB=0;
}
void shift_1_bit(unsigned char LS) 
{
  if(LS) {shift_data_1;}
  else {shift_data_0;}
  clk_rising;
}
void shift_24_bit(int Red,int Green,int Blue)  
{
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
