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

	
	public static Message getPortString = new Message();//��ȡ���ڶ�����
	
	public static Message socketString = new Message();//��¼socket����������
	
	public Server(Socket s) throws IOException
	{
		this.s = s;
		br = new BufferedReader(new InputStreamReader(s.getInputStream(),"utf-8"));
		os = s.getOutputStream();
	}
	
	@Override
	//�߳�ִ����
	public void run() 
	{
		while (true)
		{
			String msg = null;

			//����Socket���յ�����Ϣ
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
							System.out.println("���ظ��ֻ�" +regMSG);
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				else
				{
					//���յ�����Ϣ�㲥������Socket
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
					
					//���յ�����Ϣ���͸�����
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
	
	/*     ��ʼ������                    */
	/*  ��ʼ������������             */
    public static void serverInit()
    {
    	
    }


    
    /*   ��ȡ�յ�������                                                                                */
    /*   �������������� br ,����socket�յ�������                 */
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
    
    
    /*   ����ָ�������� msg                           */
    /*   ������������� os ,���� msg                   */
    public static void serverSendOut(OutputStream os , String msg) throws IOException
    {
    	os.write((msg + "\n").getBytes("utf-8"));
    }

    
    
}