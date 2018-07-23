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
	
	/*初始化端口*/
	public SerialPort initPort(String port) 
	{
		try
		{
			//打开串口
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
			CommPort commPort = portIdentifier.open(port, 2000);    //2000是打开超时时间
			serialPort = (SerialPort) commPort;
			
			
			//监听串口
			serialPort.addEventListener(new Port());
			serialPort.notifyOnDataAvailable(true);
			
			//设置参数（包括波特率，输入/输出流控制，数据位数，停止位和齐偶校验）
			serialPort.setSerialPortParams(115200,
											SerialPort.DATABITS_8, 
											SerialPort.STOPBITS_1,
											SerialPort.PARITY_NONE);
			
			
			in = serialPort.getInputStream();
			os = serialPort.getOutputStream();
			
			System.out.println("已成功打开端口 " + port );
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
	
	
	/*关闭端口*/
	public static void closePort(SerialPort sp)
	{
		if(sp != null)
		{
			sp.close();
			sp = null;
		}
	}
	
	/*  写数据 msg 到输出流 os*/
	public static void WritetoPort(OutputStream os,String msg)
	{
		System.out.println("  准备写到串口的数据"  + msg);
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
	
	/*  读取输入流 in 里面的数据，并返回该数据     */
	public static String ReadPort()
	{
		
        //记录已经到达串口COM21且未被读取的数据的字节（Byte）数。
        int availableBytes = 0;

        try 
        {
            availableBytes = in.available();
          //定义用于缓存读入数据的数组
    		byte[] cache = new byte[availableBytes];
            while(availableBytes > 0)
            {
                in.read(cache);
                for(int i = 0; i < cache.length && i < availableBytes; i++)
                {
                    //解码并输出数据
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
    	case SerialPortEvent.DATA_AVAILABLE ://数据可用
			portMSG.newMSG(ReadPort());
    		System.out.println("串口刚上报"  + portMSG.data);
    		
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
    			//将收到的信息广播给其他Socket
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
	
	//将收到的字符转成16进制数
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
