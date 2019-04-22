





/*
 *GeneratorGovernor to autostart a cheap generator with electric start
 * Mine e.g. is a Fullex FG 8500XE
 * 
 * 
 * 
 * 
 */
#include <EEPROM.h>
#include <Servo.h>
Servo jock;
#include <MsTimer2.h>
//#include <SoftwareSerial.h>
//SoftwareSerial mySerial(1, 0); // RX, TX



//Set Verison (changes reset EEPROM to default!)

const byte versionnr = 2;

//Set Pins

const byte IOStatusLed = LED_BUILTIN;
const byte IOVBat = 2;
const byte IOStarter = 4;
const byte IOStop = 3;
const byte IORPM = 14;
const byte IOVforRPM = 15;
const byte IO0forRPM = 9;

const byte IOJock = 10;
const byte JOCKOPEN =45;
const byte JOCKCLOSED = 90;

//Set Config Adresses

const byte EEversionnr = 0;
const byte EEinitfiretime = 1;
const byte EEstepupfiretime = 2;
const byte EEmaxretry = 3;
const byte EEmaxretrytest = 4;
const byte EEwaittime = 5;
const byte EEheatuptime = 6;
const byte EEtestduration = 7;
const byte EEvbatdivider = 8;
const byte EEstep = 9;


// init config values
byte initfiretime = 25;    //initial try to start in 100ms
byte stepupfiretime = 10;  //increase startertime, in 100ms
byte maxretry = 6;         //maximum retries, including first try
byte maxretrytest = 3;     //maximum retries on tests, including first try
byte waittime = 10;        //time between tries
byte heatuptime = 10;      //time before opening Jock
byte testduration = 90;    //time before stopping test
byte vbatdivider = 40;     //Batterydivider (A/D 10Bit to Volt)

//init global vars
byte volatile procstatus = 0;       //processor
byte retriesdone = 0;
byte firetime = 0;
int counter = 0;           //Timing-counter, increased every 1/10 sec
int volatile nexttrig = 10;
byte RPMtimer = 0;        //same, to find sec
byte steplocation=20;
byte engineStatBuff = 0;


String inputString = "";    // a string to hold incoming data

//init Statusvalues
String EngineStatus = "OFF";
float VBat = 14;


 void setup() {
  //set pindirections
      pinMode(IOStarter, OUTPUT); digitalWrite(IOStarter,HIGH);
      pinMode(IOStop, OUTPUT); digitalWrite(IOStop,HIGH);
      jock.attach(IOJock);
      pinMode(LED_BUILTIN, OUTPUT);
      pinMode(IOVforRPM, OUTPUT); digitalWrite(IOVforRPM,HIGH);   
      pinMode(IO0forRPM, OUTPUT); digitalWrite(IO0forRPM,LOW);   

       
  //init serial on USB
      Serial.begin(9600);
      Serial1.begin(9600);
      inputString.reserve(200);
      //mySerial.begin(9600);
      Serial1.println("Boot");
      initConf();
  //setup timer
      MsTimer2::set(100,processor);
      MsTimer2::start();
  //set Jock
      jock.write(JOCKOPEN);

}

void loop() {
  // put your main code here, to run repeatedly:
  if(Serial){
    if(Serial.available()){
      char inChar = (char)Serial.read();
      // add it to the inputString:
      inputString += inChar;
      // if the incoming character is a newline, set a flag
      // so the main loop can do something about it:
      if (inChar == '\n') {
        SerialResponse(inputString);
      }
    }
  }
  if (Serial1.available()) {
    char inChar = (char)Serial1.read();
    // add it to the inputString:
    inputString += inChar;
    // if the incoming character is a newline, set a flag
    // so the main loop can do something about it:
    if (inChar == '\n') {
      SerialResponse(inputString);
    }
  }
  
}

//void serialEvent() {
//  while (Serial.available()) {
//    // get the new byte:
//    char inChar = (char)Serial.read();
//    // add it to the inputString:
//    inputString += inChar;
//    // if the incoming character is a newline, set a flag
//    // so the main loop can do something about it:
//    if (inChar == '\n') {
//      SerialResponse(inputString);
//    }
//  }
//}

void serprintln(String msg){
  if(Serial){
    Serial.println(msg);
  }
  Serial1.println(msg);
}

void serprint(String msg){
  if(Serial){
    Serial.print(msg);
  }
  Serial1.print(msg);
}


void initConf(){    //only first time on new device
  if(EEPROM.read(EEversionnr) != versionnr){
    EEPROM.write(EEversionnr,versionnr);
    EEPROM.write(EEinitfiretime,initfiretime);
    EEPROM.write(EEstepupfiretime,stepupfiretime);
    EEPROM.write(EEmaxretry,maxretry);
    EEPROM.write(EEmaxretrytest,maxretrytest);
    EEPROM.write(EEwaittime,waittime);
    EEPROM.write(EEheatuptime,heatuptime);
    EEPROM.write(EEtestduration,testduration);
    EEPROM.write(EEvbatdivider,vbatdivider);
    EEPROM.write(EEstep, steplocation);

  }else{
    initfiretime = EEPROM.read(EEinitfiretime);
    stepupfiretime = EEPROM.read(EEstepupfiretime);
    maxretry = EEPROM.read(EEmaxretry);
    maxretrytest = EEPROM.read(EEmaxretrytest);
    waittime = EEPROM.read(EEwaittime);
    heatuptime = EEPROM.read(EEheatuptime);
    testduration = EEPROM.read(EEtestduration);
    vbatdivider = EEPROM.read(EEvbatdivider);
    
    steplocation = EEPROM.read(EEstep);
    procstatus = EEPROM.read(steplocation);
    steplocation =+ 1; if (steplocation>40){steplocation = 20;}
    EEPROM.write(EEstep,steplocation);
    EEPROM.write(steplocation,procstatus);
    
    
  }
}

void updateProc(byte procupdate){
  EEPROM.update(steplocation,procupdate);
  procstatus = procupdate;
}


void SerialResponse(String inpStr){
  byte confvalue = inpStr.substring(2).toInt();
  serprintln("");
  switch (inpStr.charAt(0)){
    case '1':
      printStatus();
      break;
    case '2':
        //byte confvalue = inpStr.substring(2).toInt();
        if(confvalue>0){
          serprint("Update Key ");serprint(String(inpStr.charAt(1)));serprint(" to "); serprintln(String(confvalue));
          switch (inpStr.charAt(1)){
            case '1':
              initfiretime = confvalue; EEPROM.write(EEinitfiretime,initfiretime);
              break;
            case '2':
              stepupfiretime = confvalue; EEPROM.write(EEstepupfiretime,stepupfiretime);
              break;
            case '3':
              maxretry = confvalue; EEPROM.write(EEmaxretry,maxretry);
             break;
            case '4':
              maxretrytest = confvalue; EEPROM.write(EEmaxretrytest,maxretrytest);
              break;
            case '5':
              waittime = confvalue; EEPROM.write(EEwaittime,waittime);
              break;
            case '6':
              heatuptime = confvalue; EEPROM.write(EEheatuptime,heatuptime);
              break;
            case '7':
              testduration = confvalue; EEPROM.write(EEtestduration,testduration);
              break;
            case '8':
              vbatdivider = confvalue; EEPROM.write(EEvbatdivider,vbatdivider);
              break;
          }
        }
        serprintln("");
        serprintln("Config");
        serprint("1. InitFireTime (1/10s) = "); serprintln(String(initfiretime));
        serprint("2. StepUpTime (1/10s) = "); serprintln(String(stepupfiretime));
        serprint("3. Max Retry = "); serprintln(String(maxretry));
        serprint("4. Max Retry on Tests = "); serprintln(String(maxretrytest));
        serprint("5. WaitTime (s) = "); serprintln(String(waittime));
        serprint("6. HeatUpTime (s) = "); serprintln(String(heatuptime));
        serprint("7. Test Duration (s) = "); serprintln(String(testduration));
        serprint("8. VBat correction vector = "); serprintln(String(vbatdivider));
        serprintln("To change type 2<Nr><Value> (no spaces!)");
        serprintln("Each Value from 1 to 255");
      break;
    case '3':
      updateProc(10);
      nexttrig = 0;
      break;
    case '4':
      updateProc(90);
      nexttrig = 0;
      break;
    case '5':
      updateProc(20);
      nexttrig = 0;
      break;
    case '7':
      updateProc(0);
      nexttrig = 0;
    case '8':
      if(confvalue>0){
          serprint("Set Key ");serprint(String(inpStr.charAt(1)));serprint(" to "); serprintln(String(confvalue));
          switch (inpStr.charAt(1)){
            case '1':
              jock.write(confvalue);
              break;
          }
        }
        serprintln("");
        serprintln("Maintenance");
        serprintln("1. Relay Position");
        serprintln("To change type 2<Nr><Value> (no spaces!)");
        serprintln("Each Value from 1 to 255");
      break;
      
    default:
      serprintln("TwipSEA V0.1");
      serprintln("1. Status");
      serprintln("2. Config");
      serprintln("3. Start");
      serprintln("4. Stop");
      serprintln("5. Testrun");
      serprintln("");
      serprintln("7. Reset Errors");
      serprintln("8. Maintenance");
      serprintln("Press <Nr><ENTER>");
  }
  inputString = "";
  
}

void printStatus(){
  serprintln("");
  serprintln("Status");
  serprint("Processor = ");serprint(String(procstatus));serprint(" --> ");
  switch(procstatus){
    case 0: serprintln("Idle");break;
    case 1: serprintln("Running");break;
    case 10: serprintln("Starting, preparing Jock");break;
    case 11: serprintln("Starting, ignition");break;
    case 12: serprintln("Starting, wait for RPM");break;
    case 13: serprintln("Starting, evaluation");break;
    case 14: serprintln("Starting, warming up");break;
    case 15: serprintln("Starting, shut down Jock, wait");break;
    case 20: serprintln("Testing, preparing Jock");break;
    case 21: serprintln("Testing, ignition");break;
    case 22: serprintln("Testing, wait for RPM");break;
    case 23: serprintln("Testing, evaluation");break;
    case 24: serprintln("Testing, warming up");break;
    case 25: serprintln("Testing, shut down Jock, wait");break;
    case 26: serprintln("Testing, heating up a bit, then stop");break;
    case 90: serprintln("Stopping!!");break;
    case 91: serprintln("Stopping, measure RPM");break;
    case 92: serprintln("Stopped, returning to idle");break;
    case 101: serprintln("Critical Error: Motor is not shutting down!");break;
    case 102: serprintln("Critical Error: Motor is not starting up!");break;
    
  }
  serprint("Engine = "); serprintln(EngineStatus);
  serprint("VBat(v) = "); serprintln(String(VBat));


}




void processor(){
  digitalWrite(LED_BUILTIN, !digitalRead(LED_BUILTIN)); //WatchDog-Led
  counter++;

  
  RPMtimer++;
  if(RPMtimer>10) {
    RPMtimer=0;
    VBat = (analogRead(IOVBat)/(float)vbatdivider + 9*VBat)/10;
    //printStatus();
    if(digitalRead(IORPM)==1){
      EngineStatus="ON";
      engineStatBuff = 10;
    }else{
      if(engineStatBuff==0){
        EngineStatus="OFF";
      }else{
        engineStatBuff--;
      }
    }      
  }
  
  
  if(counter>nexttrig){
    counter = 0;
    switch(procstatus){
      case 0:
        nexttrig=30;
        printStatus();
        if(EngineStatus=="ON"){procstatus=1;}
        break;
      case 1:
        nexttrig=30;
        printStatus();
        if(EngineStatus=="OFF"){procstatus=0;}
        break;



        
      case 10:
        retriesdone = 0;
        firetime = initfiretime;
        jock.write(JOCKCLOSED);
        printStatus();
        updateProc(11);
        nexttrig = 20;
        break;
      case 11:
        digitalWrite(IOStarter,LOW);
        printStatus();
        updateProc(12);
        nexttrig = firetime;
        break;
      case 12:
        digitalWrite(IOStarter,HIGH);
        retriesdone++;
        printStatus();
        updateProc(13);
        nexttrig = waittime*10;
        break;
      case 13:
        if(EngineStatus=="OFF"){
          serprintln("");
          serprint("Tried to start generator, try ");serprint(String(retriesdone));serprintln(" failed");
          serprintln("");
          if(retriesdone>=maxretry){
            updateProc(102);
            nexttrig = 0;
            break;
          }else{
            serprintln("Next try, but longer");
            firetime = firetime + stepupfiretime;
            updateProc(11);
            nexttrig = 20;
            break;
          }
        }else{
          updateProc(14);
          nexttrig = 0;
          break; 
        }
      case 14:
        printStatus();
        updateProc(15);
        nexttrig = heatuptime*10;
        break;
      case 15:
        printStatus();
        jock.write(JOCKOPEN);
        updateProc(1);
        nexttrig = 10;
        break;




      case 20:
        retriesdone = 0;
        firetime = initfiretime;
        jock.write(JOCKCLOSED);
        printStatus();
        updateProc(21);
        nexttrig = 20;
        break;
      case 21:
        digitalWrite(IOStarter,LOW);
        printStatus();
        updateProc(22);
        nexttrig = firetime;
        break;
      case 22:
        digitalWrite(IOStarter,HIGH);
        retriesdone++;
        printStatus();
        updateProc(23);
        nexttrig = waittime*10;
        break;
      case 23:
        if(EngineStatus=="OFF"){
          serprintln("");
          serprint("Tried to start generator, try ");serprint(String(retriesdone));serprintln(" failed");
          serprintln("");
          if(retriesdone>=maxretrytest){
            updateProc(102);
            nexttrig = 0;
            break;
          }else{
            serprintln("Next try, but longer");
            firetime = firetime + stepupfiretime;
            updateProc(21);
            nexttrig = 20;
            break;
          }
        }else{
          updateProc(24);
          nexttrig = 0;
          break; 
        }
      case 24:
        printStatus();
        updateProc(25);
        nexttrig = heatuptime*10;
        break;
      case 25:
        printStatus();
        jock.write(JOCKOPEN);
        updateProc(26);
        nexttrig = 10;
        break;
      case 26:
        printStatus();
        updateProc(90);
        nexttrig = testduration*10;
        break;


        

      case 90:
        digitalWrite(IOStarter,HIGH);
        digitalWrite(IOStop,LOW);
        retriesdone =0;
        printStatus();
        updateProc(91);
        nexttrig = 100;
        break;
      case 91:
        printStatus();
        updateProc(92);
        nexttrig = 30;
      case 92:
        if(EngineStatus="OFF"){
          digitalWrite(IOStop,HIGH);
          printStatus();
          updateProc(0);
          nexttrig = 0;
          break;
        }else{
          updateProc(101);
          nexttrig = 0;
          break;
        }
       case 101:
          printStatus();
          nexttrig = 100;
          break;
       case 102:
          printStatus();
          nexttrig = 100;
          break;
    }
  }
  
}


