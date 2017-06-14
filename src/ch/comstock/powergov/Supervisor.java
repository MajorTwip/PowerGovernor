package ch.comstock.powergov;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ch.comstock.powergov.eventbus.events.MessageEvent;

public class Supervisor {
	EventBus eventbus;
	static final Logger log = LogManager.getLogger(PowerGov.class);
	int timeoutGen = 0;
	int timeoutUps1 = 0;
	int timeoutUps2 = 0;
	boolean lineStat = true;
	boolean emcStarting = false;
	boolean emcStopping = false;
	boolean emcEng=false;

	
	public Supervisor(EventBus eventbus) {
		this.eventbus = eventbus;
		new Timer().scheduleAtFixedRate(new TimerTask(){
		    @Override
		    public void run(){
		    	if(timeoutGen>=20){
		    		eventbus.post(new MessageEvent("sv","setGenStat","err"));
		    	}else{
		    		timeoutGen++;
		    	}
		    	if(timeoutUps1>=20){
		    		eventbus.post(new MessageEvent("sv","setUps1Stat","err"));
		    	}else{
		    		timeoutUps1++;
		    	}
		    	if(timeoutUps2>=20){
		    		eventbus.post(new MessageEvent("sv","setUps2Stat","err"));
		    	}else{
		    		timeoutUps2++;
		    	}
		    }
		},0,500);
	}
	
	public void subscribe(){
		eventbus.register(this);
	}
	
	@Subscribe
	public void handleMessage(MessageEvent event){
		//log.debug("Got event from Gen. ID:" + event.getId() + " Msg:" + event.getMessage());
		switch(event.getSender()){
			case "gen":
				if(event.getId().equals("stat")){
					log.debug("Generator is in State " + event.getMessage());

					int stat = Integer.parseInt(event.getMessage());
					if(stat == 0){
						eventbus.post(new MessageEvent("sv","setGenStat","ok"));
						procEmcMode(false);
					}else if(stat==1){
						eventbus.post(new MessageEvent("sv","setGenStat","eng"));
						procEmcMode(true);
					}else if((stat>1)&&stat<100){
						eventbus.post(new MessageEvent("sv","setGenStat","low"));
						procEmcMode(false);
					}else if(stat>100){
						eventbus.post(new MessageEvent("sv","setGenStat","err"));
						procEmcMode(false);
					}
				}
	    		
	    		timeoutGen=0;
	    		break;
			case "ups1":
				if(event.getId().equals("ups.status")){
					switch(event.getMessage().substring(0, 2)){
					case "OL":
						eventbus.post(new MessageEvent("sv","setUps1Stat","ok"));
						break;
					case "OB":
						eventbus.post(new MessageEvent("sv","setUps1Stat","eng"));
						break;
					case "LB":
						eventbus.post(new MessageEvent("sv","setUps1Stat","low"));
						break;
					}
					timeoutUps1=0;
				}
	    		break;
			case "ups2":
				if(event.getId().equals("ups.status")){
					switch(event.getMessage().substring(0, 2)){
					case "OL":
						eventbus.post(new MessageEvent("sv","setUps2Stat","ok"));
						break;
					case "OB":
						eventbus.post(new MessageEvent("sv","setUps2Stat","eng"));
						break;
					case "LB":
						eventbus.post(new MessageEvent("sv","setUps2Stat","low"));
						break;
					}
					timeoutUps2=0;
				}
				break;
			case "gpio":
				log.trace("Event. ID:" + event.getId() + " Msg:" + event.getMessage());
				switch(event.getId()){
					case "btnEmergency":
						if(event.getMessage().equals("ON")){
							startEmcMode(true);
						}else if(event.getMessage().equals("OFF")){
							stopEmcMode();
						}
						break;
					case "sensLine":
						if(event.getMessage().equals("ON")){
							eventbus.post(new MessageEvent("sv","setSvStat","eng"));
							lineStat = true;
							stopEmcMode();
						}
						if(event.getMessage().equals("OFF")){
							eventbus.post(new MessageEvent("sv","setSvStat","low"));
							lineStat=false;
							new Timer().schedule(new TimerTask(){
								@Override
								public void run(){
									startEmcMode(false);
								}
							},3*60*1000);
						}
					}
				break;
			}
		}
		private void startEmcMode(boolean forced){
			log.info("initiating Emergency Mode");
			if(forced || !lineStat)eventbus.post(new MessageEvent("sv","gen","CMD","start"));
			emcStarting=true;
			emcStopping=false;
		}
		private void stopEmcMode(){
			log.info("initiating Emergency Mode Stop");
			emcStarting=false;
			emcStopping=true;
			procEmcMode(true);
		}
		
		private void procEmcMode(boolean geneStat){
			if(geneStat&&emcStarting&&!emcEng){
				emcEng=true;
				eventbus.post(new MessageEvent("sv","setInpBr",""));
				log.debug("InputBridge set");
				new Timer().schedule(new TimerTask(){
					@Override
					public void run(){
						eventbus.post(new MessageEvent("sv","setInpSw",""));
						log.debug("InputSwitch set to Generator");
					}
				},1000);
				new Timer().schedule(new TimerTask(){
					@Override
					public void run(){
						eventbus.post(new MessageEvent("sv","resetInpBr",""));
						log.debug("InputBridge reset");
						emcStarting=false;
					}
				},6000);
			}
			if(emcStopping&&emcEng){
				emcEng=false;
				eventbus.post(new MessageEvent("sv","setInpBr",""));
				log.debug("InputBridge set");
				new Timer().schedule(new TimerTask(){
					@Override
					public void run(){
						eventbus.post(new MessageEvent("sv","resetInpSw",""));
						log.debug("InputSwitch reset to line");
					}
				},1000);
				new Timer().schedule(new TimerTask(){
					@Override
					public void run(){
						eventbus.post(new MessageEvent("sv","resetInpBr",""));
						eventbus.post(new MessageEvent("sv","gen","CMD","stop"));
						log.debug("InputBridge reset");
						emcStopping=false;
					}
				},6000);
			}
			if(emcStopping&&!emcEng){
				eventbus.post(new MessageEvent("sv","gen","CMD","stop"));

				new Timer().schedule(new TimerTask(){
					@Override
					public void run(){
						eventbus.post(new MessageEvent("sv","resetInpBr",""));
						eventbus.post(new MessageEvent("sv","resetInpSw",""));
						emcStopping=false;
					}
				},7000);
			}
		}
	}


