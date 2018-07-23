package server;

public class Message {
	
	public String data = null;
	
	public int status = 0;//1代表未处理，0代表已处理
	
	public Message()
	{
		
	}
	
	public void newMSG(String data)
	{
		this.data = data;
		this.status = 1;//表示数据未处理
	}
	
	public void oldMSG()
	{
		this.status = 0;//表示数据未处理
	}

}
