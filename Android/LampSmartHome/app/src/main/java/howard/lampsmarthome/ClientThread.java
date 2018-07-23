package howard.lampsmarthome;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Created by Traceur on 2018/5/15.
 */

public class ClientThread implements Runnable {
    private Handler handler;
    public Socket socket;
    public Handler revHandler;
    BufferedReader br = null;
    OutputStream os = null;
    public String status = new String();

    public ClientThread(Handler handler)
    {
        this.handler = handler;
        this.socket = null;
    }

    @Override
    public void run()
    {
        try
        {
            this.socket = new Socket(Login.loginMSG[0],30000);
            if( this.socket.isConnected())
            {

                this.status = "CONNECT_SUCCESS";
            }
            else
            {
                status = "CONNECT_FAILURE";
            }
            os = this.socket.getOutputStream();
            br = new BufferedReader(
                    new InputStreamReader(this.socket.getInputStream())
            );

            new Thread()
            {
                @Override
                public void run()
                {

                    String line = null;
                    try
                    {
                        while ((line = br.readLine()) != null)
                        {
                            Message msg = new Message();
                            msg.what = 0x123;
                            msg.obj = line;
                            handler.sendMessage(msg);
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }.start();
            Looper.prepare();
            revHandler = new Handler()
            {
                @Override
                public void handleMessage(Message msg)
                {
                    if (msg.what == 0x345)
                    {
                        try
                        {
                            os.write((msg.obj.toString()+"\n").getBytes("utf-8"));
                        }
                        catch (Exception e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Looper.loop();
        }
        catch (SocketTimeoutException e)
        {
            System.out.println("网络连接超时！！");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }
}
