package ch.comstock.powergov;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.networkupstools.jnut.*;

import java.util.TimerTask;

import com.google.common.eventbus.EventBus;

import ch.comstock.powergov.eventbus.events.MessageEvent;


public class Ups1Mon {
	EventBus eventbus;
	Client upsnut = new Client();
	static final Logger log = LogManager.getLogger(PowerGov.class);
	List<String> neededVars = Arrays.asList("ups.status","battery.charge","battery.runtime","input.frequency","input.voltage","ups.load","ups.power");
	private static Map<String, String> upsMap;
	static
    {
        upsMap = new HashMap<String, String>();
        upsMap.put("ups", "ups1");
        upsMap.put("roline", "ups2");
    }

	
	public Ups1Mon(EventBus eventbus)  {
		this.eventbus = eventbus;
		
		new Timer().scheduleAtFixedRate(new TimerTask(){
		    @Override
		    public void run(){
		    		postDevInfo();
		    }
		},0,10000);
		
	}
	
	public void postDevInfo(){
		try {
            upsnut.connect("127.0.0.1",3493,"upsmon","pass");
            Device[] devs = upsnut.getDeviceList();
            if(devs!=null)
            {
                for(int d=0; d<devs.length; d++)
                {
                    Device dev = devs[d];
                    String devName = dev.getName();
                    if(upsMap.containsKey(devName))devName = upsMap.get(devName);
                    
                    try {
                        Variable[] vars = dev.getVariableList();
                        if(vars!=null)
                        {
                            if(vars.length==0)
                                log.debug("  NO VAR");
                            for(int v=0; v<vars.length; v++)
                            {
                                Variable var = vars[v];
                                if(neededVars.contains(var.getName())){
                                	//log.info(var.getName() + "[" + devName + "] : " + var.getValue());
                                	eventbus.post(new MessageEvent(devName,var.getName(),var.getValue()));
                                }
                            }
                        }
                        else
                            System.out.println("  NULL VAR");
                    } catch(NutException e) {
                        e.printStackTrace();
                    }                    
                }
            }
            
            upsnut.disconnect();
            
        }catch(Exception e){
            e.printStackTrace();
        }
	}
	
	public void subscribe(){
		eventbus.register(this);
	}
	
	

}
