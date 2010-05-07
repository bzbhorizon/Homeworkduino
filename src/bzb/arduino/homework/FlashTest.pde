#include "Rainbow.h"
unsigned char NumTab[9]=
{ 
  //0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f
  0,1,2,3,4,5,6,7,8
};
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
                
}
void loop()
{
  int i;
  for(i=0;i<9;i++)
  {
    shift_24_bit(NumTab[i],0,0);
    delay(500);
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