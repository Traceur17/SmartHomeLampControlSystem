package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.TooManyListenersException;

import gnu.io.*;

public class Port implements SerialPortEventListener {
	
	public static SerialPort serialPort;
	public static InputStream in;
	public static OutputStream os;
	
	public static Message portMSG = new Message();
	
	/*��ʼ���˿�*/
	public SerialPort initPort(String port) 
	{
		try
		{
			//�򿪴���
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
			CommPort commPort = portIdentifier.open(port, 2000);    //2000�Ǵ򿪳�ʱʱ��
			serialPort = (SerialPort) commPort;
			
			
			//��������
			serialPort.addEventListener(new Port());
			serialPort.notifyOnDataAvailable(true);
			
			//���ò��������������ʣ�����/��������ƣ�����λ����ֹͣλ����żУ�飩
			serialPort.setSerialPortParams(115200,
											SerialPort.DATABITS_8, 
											SerialPort.STOPBITS_1,
											SerialPort.PARITY_NONE);
			
			
			in = serialPort.getInputStream();
			os = serialPort.getOutputStream();
			
			System.out.println("�ѳɹ��򿪶˿� " + port );
	    } catch (PortInUseException e) {
	        e.printStackTrace();
	    } catch (TooManyListenersException e) {
	        e.printStackTrace();
	    } catch (UnsupportedCommOperationException e) {
	        e.printStackTrace();
	    } catch (NoSuchPortException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return serialPort;
	}
	
	
	/*�رն˿�*/
	public static void closePort(SerialPort sp)
	{
		if(sp != null)
		{
			sp.close();
			sp = null;
		}
	}
	
	/*  д���� msg ������� os*/
	public static void WritetoPort(OutputStream os,String msg)
	{
		System.out.println("  ׼��д�����ڵ�����"  + msg);
		String[] command = msg.split(",",2);
		
		byte[] cmd = null;
		
		if( command[0].equals("Command"))
		{
			switch (command[1])
			{
			case "KEY1,on,":
				cmd = hex2byte("10");
				break;
			case "KEY1,off,":
				cmd = hex2byte("11");
				break;
			case "KEY2,on,":
				cmd = hex2byte("20");
				break;
			case "KEY2,off,":
				cmd = hex2byte("21");
				break;
			default :break;
			}
			
			try {
				os.write(cmd);
				os.flush();   
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	/*  ��ȡ������ in ��������ݣ������ظ�����     */
	public static String ReadPort()
	{
		
        //��¼�Ѿ����ﴮ��COM21��δ����ȡ�����ݵ��ֽڣ�Byte������
        int availableBytes = 0;

        try 
        {
            availableBytes = in.available();
          //�������ڻ���������ݵ�����
    		byte[] cache = new byte[availableBytes];
            while(availableBytes > 0)
            {
                in.read(cache);
                for(int i = 0; i < cache.length && i < availableBytes; i++)
                {
                    //���벢�������
                    System.out.print((char)cache[i]);
                }
                availableBytes = in.available();
            }
            String msg = new String(cache);
            return msg;
        }
        catch (IOException e) 
        {
        	return null;
        }

	}

	
	@Override
    public void serialEvent(SerialPortEvent event) 
	{
    	switch(event.getEventType())
    	{
    	case SerialPortEvent.DATA_AVAILABLE ://���ݿ���
			portMSG.newMSG(ReadPort());
    		System.out.println("���ڸ��ϱ�"  + portMSG.data);
    		
    		String[] data = new String[4];
    		data = portMSG.data.split(",",4);
    		switch(data[0])
    		{
    		case "Register":
    			String[] realData = portMSG.data.split("Register,",3);
    			int i = 1;
    			for( i = 1 ; i<realData.length ;i++)
    			{
        			Main.ZigBeeDevList.add("Register,"+realData[i]);
    			}
    			break;
    		case "Command":
    			//���յ�����Ϣ�㲥������Socket
    			for(Iterator<Socket> it = Main.SocketList.iterator();it.hasNext();)
    			{
    				Socket s = it.next();
    				try
    				{
    	    			OutputStream sos = s.getOutputStream();
    	    			sos.write((portMSG.data + "\n").getBytes("utf-8"));
    				} catch (SocketException e)
    				{
    					e.printStackTrace();
    					it.remove();
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
    			}
    			break;
    		}
    		
			portMSG.oldMSG();
			break;
    		
    	}
        
    }  
	
	//���յ����ַ�ת��16������
	public static byte[] hex2byte(String hex) {
        String digital = "0123456789ABCDEF";
        String hex1 = hex.replace(" ", "");
        char[] hex2char = hex1.toCharArray();
        byte[] bytes = new byte[hex1.length() / 2];
        byte temp;
        for (int p = 0; p < bytes.length; p++) {
            temp = (byte) (digital.indexOf(hex2char[2 * p]) * 16);
            temp += digital.indexOf(hex2char[2 * p + 1]);
            bytes[p] = (byte) (temp & 0xff);
        }
        return bytes;
    }

}
