package ch.comstock.powergov;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.networkupstools.jnut.NutException;

import com.google.common.eventbus.*;
import ch.comstock.powergov.eventbus.events.*;


public class PowerGov {
	
	static String confFile = "config.properties";
	static Properties conf = new Properties();
	static EventBus eventbus = new EventBus();
	static EventBusListener evblisten = new EventBusListener();
	static Logger log = LogManager.getLogger(PowerGov.class);

	
	public static void main(String[] args) throws UnknownHostException, IOException, NutException {
		// TODO Auto-generated method stub
		log.info("Starting Twip-PowerGovernor");
		log.info("Loading Config File...");
		getConfig();
		
		eventbus.register(evblisten);
		
		log.info("Starting Supervisor");
		Supervisor supervisor = new Supervisor(eventbus);
		supervisor.subscribe();
		log.info("Supervisor running");
		
		
		log.info("Starting GPIO-interface");
		GPIO gpio = new GPIO(eventbus);
		gpio.subscribe();
		log.info("GPIO-interface running");
		
		
		log.info("Starting Generator Governor");
		GeneratorGov gengov = new GeneratorGov(eventbus);
		gengov.subscribe();
		gengov.connectGen(conf.getProperty("ttyPort"));
		log.info("GenGov running");

		
		log.info("Starting UPS-Monitoring");
		Ups1Mon ups1mon = new Ups1Mon(eventbus);
		ups1mon.subscribe();
		log.info("UpsMon running");

		
		//Gui gui = new Gui(eventbus);
		//gui.subscribe();
		//gui.show();
		
		
		log.info("Starting MQTT");
		MQTT mqtt = new MQTT(eventbus);
		mqtt.subscribe();
		log.info("MQTT");

	}
	
	
	
	private static void getConfig(){
		//get config, create sample if no file exists
		InputStream inputStream = null;
		try{
			inputStream = new FileInputStream(confFile);
			conf.load(inputStream);
			log.info("Config loaded");
		} catch (Exception e) {
			log.error("Exception: " + e);
			log.warn("Probably no ConfFile existing, creating sample");
			createConfig();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static void createConfig(){
		OutputStream output = null;

		try {

			output = new FileOutputStream("config.properties");

			// set the properties value
			conf.setProperty("ttyPort", "/dev/ttyAMA0");
			conf.setProperty("mqttHost", "192.168.2.246");
			conf.setProperty("mqttTopicBase", "/PowerGov/");

			// save properties to project root folder
			conf.store(output, null);

		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
					log.warn("written, please modify config.properties and restart");
					System.exit(0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private static class EventBusListener{		
		@Subscribe
		public void handleMessage(MessageEvent event){
			//System.out.println(event.getMessage());
		}
	}
}
