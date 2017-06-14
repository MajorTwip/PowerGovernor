package ch.comstock.powergov;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.eventbus.*;
import ch.comstock.powergov.eventbus.events.*;
import jssc.*;

//
//case 0: Serial.println("Idle");break;
//case 1: Serial.println("Running");break;
//case 10: Serial.println("Starting, preparing Jock");break;
//case 11: Serial.println("Starting, ignition");break;
//case 12: Serial.println("Starting, wait for RPM");break;
//case 13: Serial.println("Starting, evaluation");break;
//case 14: Serial.println("Starting, warming up");break;
//case 15: Serial.println("Starting, shut down Jock, wait");break;
//case 20: Serial.println("Testing, preparing Jock");break;
//case 21: Serial.println("Testing, ignition");break;
//case 22: Serial.println("Testing, wait for RPM");break;
//case 23: Serial.println("Testing, evaluation");break;
//case 24: Serial.println("Testing, warming up");break;
//case 25: Serial.println("Testing, shut down Jock, wait");break;
//case 26: Serial.println("Testing, heating up a bit, then stop");break;
//case 90: Serial.println("Stopping!!");break;
//case 91: Serial.println("Stopping, measure RPM");break;
//case 92: Serial.println("Stopped, returning to idle");break;
//case 101: Serial.println("Critical Error: Motor is not shutting down!");break;
//case 102: Serial.println("Critical Error: Motor is not starting up!");break;



public class GeneratorGov {
	
	boolean desiredStat = false;
	int stat = 200;
	EventBus eventbus;
	SerialPort genSerPort;
	StringBuilder genInputBuf = new StringBuilder();
	static final Logger log = LogManager.getLogger(PowerGov.class);
	
	
	public GeneratorGov(EventBus eventbus){
		this.eventbus = eventbus;
		new Timer().scheduleAtFixedRate(new TimerTask(){
		    @Override
		    public void run(){
		    	if(genSerPort!=null){
		    		if(desiredStat){
		    			if(((stat == 0)||(stat>=20))&&!(stat>=100)){
							try {
								genSerPort.writeString("3\r\n");
								log.debug("Generator starting");

							} catch (SerialPortException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		    			}
		    		}else{
		    			if((stat > 0)&&!((stat>=20)&&stat<30)&&!((stat>=90)&&(stat<100))){
							try {
								genSerPort.writeString("4\r\n");
								log.debug("Generator stopping");
							} catch (SerialPortException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		    			}
		    		}
		    	}	
		    }
		},0,10000);
	}
	
	public void subscribe(){
		eventbus.register(this);
	}
	
	public String connectGen(String port){
		System.out.println("Serialport Generator: " + port);
		
		
		System.out.println("Connecting to Generator on port " + port);
		genSerPort = new SerialPort(port);
		try {
			genSerPort.openPort();
			genSerPort.setParams(SerialPort.BAUDRATE_9600,
		                         SerialPort.DATABITS_8,
		                         SerialPort.STOPBITS_1,
		                         SerialPort.PARITY_NONE);
			genSerPort.setRTS(false); 
			genSerPort.setDTR(false);
			genSerPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
			System.out.println("Port opened, registering shutdown hook");
			Runtime.getRuntime().addShutdownHook(new Thread() {
			    @Override
				public void run() {
			    	log.warn("Shutting down...");
			    	try {
			    		genSerPort.closePort();
			    	} catch (SerialPortException e) {
			    		// TODO Auto-generated catch block
			    	}
			    }
			 });
			return "Opened port";
		}catch (SerialPortException ex) {
			    log.error("There are an error with port : " + ex);
			return "error opening port";
		}
			


	}
	
	@Subscribe
	public void handleMessage(MessageEvent event){
		if(event.getDest()=="gen"){
			try {
				switch(event.getMessage()){
					case "testrun":
						genSerPort.writeString("5\r\n");
			    		log.info("Generatortest started");
			    		break;
					case "start":
						genSerPort.writeString("7\r\n");
						desiredStat=true;
			    		log.info("Generatorstat changed to On");
			    		break;
					case "stop":
						desiredStat=false;
			    		log.info("Generatorstat changed to Off");
			    		break;
					case "reset":
						genSerPort.writeString("7\r\n");
						break;
				}
			} catch (SerialPortException e) {
					eventbus.post(new MessageEvent("gen","err","tty not open"));
			}

		}
	}
	
	private class PortReader implements SerialPortEventListener{
		@Override
	    public void serialEvent(SerialPortEvent event) {
			if(event.isRXCHAR() && event.getEventValue() > 0){
		        try {
		            byte buffer[] = genSerPort.readBytes();
		            for (byte b: buffer) {
		                    if ( (b == '\r' || b == '\n') && genInputBuf.length() > 0) {
		                        String toProcess = genInputBuf.toString();
		                        new Thread(() -> {
		                        	String TtoProcess = toProcess;
		                        	//System.out.println(TtoProcess);
		                        	String[] msgParts = TtoProcess.split(" = ");
		                        	if(msgParts.length>1){
		                        		String id = msgParts[0].trim();
		                        		String msg = msgParts[1].trim();
		                        		eventbus.post(new MessageEvent("gen",id,msg));
		                        		if(id.equals("Processor")){
		                        			
		                        			int newstat = Integer.parseInt(msg.split(" ")[0]);
		                        			log.trace("Generator is in State: " + msg.split(" ")[0] + ":"+ newstat);
		                        			if(newstat!=stat)eventbus.post(new MessageEvent("gen","stat",String.valueOf(newstat)));
		                        			stat = newstat;
		                        		}
		                        	}
		                        }).start();
		                        genInputBuf.setLength(0);
		                    }
		                    else {
		                        genInputBuf.append((char)b);
		                    }
		            }                
		        }
		        catch (SerialPortException ex) {
		        	System.out.println("There are an error with port : " + ex);
		        }
		    }
		}
	}
	
	
}
