package ch.comstock.powergov;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ch.comstock.powergov.eventbus.events.MessageEvent;

public class MQTT {
	EventBus eventbus;
	static final Logger log = LogManager.getLogger(PowerGov.class);

	
	String broker       = "tcp://192.168.2.246:1883";
    String clientId     = "PowerGov";
    MemoryPersistence persistence = new MemoryPersistence();
    MqttClient mqttClient;

	public MQTT(EventBus eventbus) {
		this.eventbus = eventbus;

	}
	
	public void subscribe(){
		eventbus.register(this);
        try {
            mqttClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            log.debug("Connecting to broker: "+broker);
            mqttClient.connect(connOpts);
            log.debug("Connected");
        } catch(MqttException me) {
            log.error("reason "+me.getReasonCode());
            log.error("msg "+me.getMessage());
            log.error("loc "+me.getLocalizedMessage());
            log.error("cause "+me.getCause());
            log.error("excep "+me);
            me.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
		    @Override
			public void run() {
		    	try {
					if(mqttClient.isConnected())mqttClient.disconnect();
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
        });
		
	}
	
	@Subscribe
	public void handleMessage(MessageEvent event){
		String content = event.getMessage();
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(2);
        if(mqttClient!=null){
			try {
				if(mqttClient.isConnected())mqttClient.publish("/PowerGov/" + event.getSender() + "/" +event.getId(), message);
			} catch (MqttPersistenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		
	}

}
