package ch.comstock.powergov;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Shift;

import ch.comstock.powergov.eventbus.events.MessageEvent;

public class GPIO {
	
	static Logger log = LogManager.getLogger(PowerGov.class);

	EventBus eventbus;
	final GpioController gpio = GpioFactory.getInstance();
	GpioPinDigitalInput btnEmergency = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, "MyButton", PinPullResistance.PULL_UP); 
	GpioPinDigitalOutput ledClock = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03,"My LED", PinState.LOW);
	GpioPinDigitalOutput ledSignal = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07,"My LED", PinState.LOW);
	
	GpioPinDigitalOutput sensSup = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_10,"My LED", PinState.HIGH);
	GpioPinDigitalInput sensLine = gpio.provisionDigitalInputPin(RaspiPin.GPIO_11, "MyButton", PinPullResistance.PULL_DOWN); 
	GpioPinDigitalInput sensGen = gpio.provisionDigitalInputPin(RaspiPin.GPIO_06, "MyButton", PinPullResistance.PULL_DOWN); 

	GpioPinDigitalOutput inpBr = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01,"My LED", PinState.LOW);
	GpioPinDigitalOutput inpSw = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04,"My LED", PinState.LOW);
	
	
	boolean ledUps1Red = false;
	boolean ledUps1Blue = false;
	boolean ledUps2Red = false;
	boolean ledUps2Blue = false;
	boolean ledGenRed = false;
	boolean ledGenBlue = false;
	boolean ledSvRed = true;
	boolean ledSvBlue = false;
	
	boolean sensLineStat;
	boolean sensGenStat;
	boolean btnEmergencyStat;
	
	int statSv = 2;
	int statGen=0;
	int statUps1=0;
	int statUps2=0;
	
	
	


	public GPIO(EventBus eventbus) {
		this.eventbus = eventbus;
		
		ledSvRed = false;
		
		
		new Timer().scheduleAtFixedRate(new TimerTask(){
		    @Override
		    public void run(){
		    	
		    	switch(statSv){
		    	case 0:
		    		ledSvRed=true; 
		    		ledSvBlue=false;
		    		break;
		    	case 1:
		    		ledSvRed=false; 
		    		ledSvBlue=true;
		    		break;
		    	case 2:
		    		ledSvRed=false; 
		    		ledSvBlue=!ledSvBlue;
		    		break;
		    	case 3:
		    		ledSvRed = !ledSvRed; 
		    		ledSvBlue = !ledSvRed;
		    		break;
		    	}
		    	
		    	switch(statGen){
		    	case 0:
		    		ledGenRed=true; 
		    		ledGenBlue=false;
		    		break;
		    	case 1:
		    		ledGenRed=false; 
		    		ledGenBlue=true;
		    		break;
		    	case 2:
		    		ledGenRed=false; 
		    		ledGenBlue=!ledGenBlue;
		    		break;
		    	case 3:
		    		ledGenRed = !ledGenRed; 
		    		ledGenBlue = !ledGenRed;
		    		break;
		    	}
		    	
		    	switch(statUps1){
		    	case 0:
		    		ledUps1Red = true; 
		    		ledUps1Blue = false;
		    		break;
		    	case 1:
		    		ledUps1Red = false; 
		    		ledUps1Blue = true;
		    		break;
		    	case 2:
		    		ledUps1Red = false; 
		    		ledUps1Blue = !ledUps1Blue;
		    		break;
		    	case 3:
		    		ledUps1Red = !ledUps1Red; 
		    		ledUps1Blue = !ledUps1Red;
		    		break;
		    	}
		    	
		    	switch(statUps2){
		    	case 0:
		    		ledUps2Red = true; 
		    		ledUps2Blue = false;
		    		break;
		    	case 1:
		    		ledUps2Red = false; 
		    		ledUps2Blue = true;
		    		break;
		    	case 2:
		    		ledUps2Red = false; 
		    		ledUps2Blue = !ledUps2Blue;
		    		break;
		    	case 3:
		    		ledUps2Red = !ledUps2Red; 
		    		ledUps2Blue = !ledUps2Red;
		    		break;
		    	}

		    	setLeds(ledSvBlue,ledSvRed,ledUps1Red,ledUps1Blue,ledUps2Blue,ledUps2Red,ledGenRed,ledGenBlue);

		    	if(sensGen.isHigh()!=sensGenStat){
		    		sensGenStat=sensGen.isHigh();
		    		eventbus.post(new MessageEvent("gpio","sensGen",sensGen.isHigh()?"ON":"OFF"));
		    		log.info("GenLine stat changed to" + String.valueOf(sensGen.isHigh()));

		    	}
		    	if(sensLine.isHigh()!=sensLineStat){
		    		sensLineStat=sensLine.isHigh();
		    		eventbus.post(new MessageEvent("gpio","sensLine",sensLine.isHigh()?"ON":"OFF"));
		    		log.info("Line stat changed to" + String.valueOf(sensLine.isHigh()));
		    	}
		    	if(btnEmergency.isLow()!=btnEmergencyStat){
		    		btnEmergencyStat=btnEmergency.isLow();
		    		eventbus.post(new MessageEvent("gpio","btnEmergency",btnEmergency.isLow()?"ON":"OFF"));
		    		log.info("Emergency stat changed to" + String.valueOf(btnEmergency.isLow()));

		    	}
		    	
		    }
		},0,1000);
		
		
		eventbus.post(new MessageEvent("gpio","btnEmergency",btnEmergency.isLow()?"ON":"OFF"));
		eventbus.post(new MessageEvent("gpio","sensLine",sensLine.isHigh()?"ON":"OFF"));
		eventbus.post(new MessageEvent("gpio","sensGen",sensGen.isHigh()?"ON":"OFF"));

		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    @Override
			public void run() {
		    	System.out.println("Shutting down...");
		    	setLeds(false,true,false,false,false,false,false,false);
		    }
		 });
		
		
		
	}
	
	
	
	public void subscribe(){
		eventbus.register(this);
	}
	
	@Subscribe
	public void handleMessage(MessageEvent event){
		if(event.getSender().equals("sv")){
			int stat=0;
			switch(event.getMessage()){
				case "err": 
					stat=0; 
					break;
				case "ok": 
					stat=1;
					break;
				case "eng": 
					stat=2;
					break;
				case "low": 
					stat=3;
					break;
			}
			switch(event.getId()){
				case "setSvStat": 
					statSv=stat;
					break;
				case "setGenStat": 
					statGen=stat;
					break;
				case "setUps1Stat": 
					statUps1=stat;
					break;
				case "setUps2Stat": 
					statUps2=stat;
					break;
				case "setInpSw":
					inpSw.high();
					break;
				case "resetInpSw":
					inpSw.low();
					break;
				case "setInpBr":
					inpBr.high();
					break;
				case "resetInpBr":
					inpBr.low();
					break;
			}
			log.trace(event.getId() + " to " + event.getMessage());
		}
	}
	
	
	private void setLeds(boolean led0,boolean led1,boolean led2,boolean led3,boolean led4,boolean led5,boolean led6,boolean led7){
		byte b = (byte)((!led0?1<<7:0) + (!led1?1<<6:0) + (!led2?1<<5:0) + (!led3?1<<4:0) + (!led4?1<<3:0) + (!led5?1<<2:0) + (!led6?1<<1:0) + (!led7?1:0));
		//log.trace(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
		Shift.shiftOut((byte)ledSignal.getPin().getAddress(),(byte)ledClock.getPin().getAddress(),(byte)Shift.LSBFIRST,b);
	}

}
