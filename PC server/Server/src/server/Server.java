package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;



public class Server implements Runnable
{
	Socket s;
	BufferedReader br ;
	OutputStream os ;

	
	public static Message getPortString = new Message();//获取串口端数据
	
	public static Message socketString = new Message();//记录socket发来的数据
	
	public Server(Socket s) throws IOException
	{
		this.s = s;
		br = new BufferedReader(new InputStreamReader(s.getInputStream(),"utf-8"));
		os = s.getOutputStream();
	}
	
	@Override
	//线程执行体
	public void run() 
	{
		while (true)
		{
			String msg = null;

			//处理Socket端收到的信息
			while(( msg = serverGetMSG(br)) != null)
			{
				socketString.newMSG(msg);
				
				if (socketString.data.equals("GET_ZIGBEEDEVLIST"))
				{
					for(Iterator<String> it = Main.ZigBeeDevList.iterator(); it.hasNext();)
					{
						String regMSG = it.next();
						try {
							serverSendOut(os,regMSG);
							System.out.println("返回给手机" +regMSG);
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				else
				{
					//将收到的信息广播给其他Socket
					for(Iterator<Socket> it = Main.SocketList.iterator();it.hasNext();)
					{
						Socket s = it.next();
						try
						{
			    			OutputStream sos = s.getOutputStream();
			    			sos.write((socketString.data + "\n").getBytes("utf-8"));
						} catch (SocketException e)
						{
							e.printStackTrace();
							it.remove();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					//将收到的信息发送给串口
					Port.WritetoPort( Port.os , socketString.data);
				}

				
				socketString.oldMSG();
			}
			
			if (s.isClosed())
			{
				break;
			}

		}
		
	}
	
	/*     初始化函数                    */
	/*  初始化服务器数据             */
    public static void serverInit()
    {
    	
    }


    
    /*   获取收到的数据                                                                                */
    /*   传入输入流参数 br ,返回socket收到的数据                 */
    public String serverGetMSG(BufferedReader br)
    {
    	try
        {	
        	return br.readLine();
        }
        catch(IOException e)
        {
        	Main.SocketList.remove(s);
        }
    	return null;
    }
    
    
    /*   发送指定的数据 msg                           */
    /*   传入输出流参数 os ,数据 msg                   */
    public static void serverSendOut(OutputStream os , String msg) throws IOException
    {
    	os.write((msg + "\n").getBytes("utf-8"));
    }

    
    
}