package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class Main {

	public static ArrayList<Socket> SocketList = new ArrayList<Socket>();
	public static ArrayList<String> ZigBeeDevList = new ArrayList<String>();//todo 写到文件里保存
	
	public static void main (String[] args) throws IOException
	{
		String addr = InetAddress.getLocalHost().getHostAddress();
		System.out.println("服务器已启动\n");
		ServerSocket ss = new ServerSocket(30000);
		
		new Port().initPort("COM4");
				
		
		while (true)
		{
			try
			{
				Socket s = ss.accept();
				System.out.println("有新客户连接");
				SocketList.add(s);
				new Thread(new Server(s)).start();
			} catch (SocketException e)
			{
				System.out.println("socket连接有问题");
				e.printStackTrace();
				ss.close();
			}
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
		}
	}
	
	public static void Saving()
	{
		
	}
	
	
}
