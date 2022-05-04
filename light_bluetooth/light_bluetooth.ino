int val; //0-9
int type;
int isOpen = 0;
const int redPin = 11;  
const int relayPin =7; //the "s" of relay module attach to
void setup() {

// sets the redPin to be an output   
pinMode(9, OUTPUT); 
pinMode(10, OUTPUT); 
pinMode(11, OUTPUT); 
pinMode(relayPin, OUTPUT); 
// pinMode(0, INPUT); 
// pinMode(1, OUTPUT); 

  // put your setup code here, to run once:
 Serial.begin(9600);//连接到串行端口，波特率为9600
 Serial.println("light ready" ) ;  
}

void loop() {
  // put your main code here, to run repeatedly:
 type=Serial.read();//读取蓝牙端口的值
 val=type;
if(val=='1' )
  {

  Serial.print(1);
  Serial.println();
  digitalWrite(9, LOW);  
  digitalWrite(11, LOW); 
  digitalWrite(10, LOW);  
      
  }

  if(val=='2' )
  {

  Serial.print(2);
  Serial.println();
  digitalWrite(9, HIGH);  
  digitalWrite(11, HIGH); 
  digitalWrite(10, LOW);    
  
  }

  if(val=='3' )
  {
  Serial.print(3);
  Serial.println();
   digitalWrite(9, HIGH);  
   digitalWrite(10, HIGH);  
   digitalWrite(11, LOW);     
  }

  

 
 
}