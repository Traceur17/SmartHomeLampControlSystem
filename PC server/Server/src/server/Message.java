package server;

public class Message {
	
	public String data = null;
	
	public int status = 0;//1����δ����0�����Ѵ���
	
	public Message()
	{
		
	}
	
	public void newMSG(String data)
	{
		this.data = data;
		this.status = 1;//��ʾ����δ����
	}
	
	public void oldMSG()
	{
		this.status = 0;//��ʾ����δ����
	}

}
