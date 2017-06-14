package ch.comstock.powergov.eventbus.events;

public class MessageEvent {
	protected String id;
	protected String message;
	protected String sender;
	protected String dest;
	
	
	public MessageEvent(String sender, String id, String message) {
		this(sender,"",id,message);
	}
	
	public MessageEvent(String sender, String dest, String id, String message){
		this.sender = sender;
		this.dest = dest;
		this.id = id;
		this.message = message;
	}
	
	
	public String getSender(){
		return sender;
	}
	
	public String getDest(){
		return dest;
	}
	
	public String getId(){
		return id;
	}
	
	public String getMessage(){
		return message;
	}
}
