package howard.lampsmarthome;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends Activity {

    final int CONN = 1000;
    final int ADD = 1001;
    public static ClientThread clientThread;
    public static ArrayList<String[]> ZigBeeDevList = new ArrayList<String[]>();
    public static ArrayList<String[]> beSelected = new ArrayList<String[]>();
    public static Handler handler;
    public static String[] status = new String[2];//0,ip,  1,success,

    public RelativeLayout mainlayout;
    public Button conn , add ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainlayout = new RelativeLayout(this);
        setView(mainlayout);

        add = findViewById(ADD);
        conn = findViewById(CONN);

        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if(msg.what == 0x123)
                {
                    String[] dev = msg.obj.toString().split(",",4);
                    switch (dev[0])
                    {
                        case "Register":
                            ZigBeeDevList.add(dev);
                            System.out.println(msg.obj.toString());
                            break;
                        case "Command":
                            ArrayList<String[]> tmp = new ArrayList<String[]>();
                            Button bt;
                            int id = 0;
                            for (Iterator<String[]> it = beSelected.iterator();it.hasNext();)
                            {
                                System.out.println("第一层");
                                String[] device = it.next();
                                System.out.println( "收到的数据   "+  dev[0]   +  "    存储的数据   " +  device[0]);
                                if (device[0].equals(dev[1]))
                                {
                                    System.out.println("第二层");
                                    if (device[1].equals(dev[2]))
                                    {
                                        //do nothing
                                    }
                                    else
                                    {
                                        System.out.println("");
                                        id = Integer.parseInt(device[2]);
                                        bt = findViewById(id);
                                        if (dev[2].equals("on"))
                                        {
                                            System.out.println("第三层");
                                            bt.setBackgroundColor(Color.rgb(224, 255, 64));
                                            bt.setHint("off");
                                        }
                                        else
                                        {
                                            System.out.println("第四层");
                                            bt.setBackgroundColor(Color.rgb(184, 184, 184));
                                            bt.setHint("on");
                                        }
                                        device[1] = dev[2];
                                    }
                                }
                                tmp.add(device);
                            }
                            beSelected = tmp;
                    }
                }
            }

        };

        if( (status[1] != null ) && status[1].equals("LOGIN_SUCCESS") )
        {
            conn.setText(status[0]);
        }
        else
        {
            status[1] = "NO_LOGIN";
        }

        new Thread()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    Message msg = new Message();
                    msg.what = 0x345;
                    msg.obj = "GET_ZIGBEEDEVLIST";
                    if( (clientThread != null) && (clientThread.socket != null) && (clientThread.socket.isConnected()))
                    {
                        clientThread.revHandler.sendMessage(msg);
                    }
                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        conn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,Login.class);
                startActivityForResult(intent,1);
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,AddDevices.class);
                startActivityForResult(intent,1);
            }
        });

        int count = beSelected.size();
        if (count != 0)
        {
            mainlayout.removeView(add);
            reDraw(mainlayout);
        }//endif


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == 1) {
                String[] state = data.getStringArrayExtra("status");
                if (state[1].equals("LOGIN_SUCCESS"))
                {
                    status = state;
                    conn.setText(status[0]);
                }
                else
                {
                    conn.setText("连接");
                    status = state;
                }
            }
            else if (resultCode == 2)
            {
                reDraw(mainlayout);
            }
        }
    }


    private void setView(RelativeLayout mainlayout)
    {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        Button connect = new Button(this);
        connect.setId(CONN);
        connect.setText("连接");
        RelativeLayout.LayoutParams connParams = new RelativeLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        mainlayout.addView(connect,connParams);

        Button add = new Button(this);
        add.setId(ADD);
        add.setText("添加");
        RelativeLayout.LayoutParams addParams = new RelativeLayout.LayoutParams ( (width-50)/4,200);
        addParams.leftMargin = 10;
        addParams.topMargin = 20;
        addParams.addRule(RelativeLayout.BELOW,CONN);
        mainlayout.addView(add,addParams);

        setContentView(mainlayout);

    }

    private void reDraw(RelativeLayout mainlayout)
    {
        mainlayout.removeView(add);
        int count = beSelected.size();
        System.out.println("redraw   "  + count );
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        mainlayout.removeView(add);
        final Button btn[] = new Button[count];
        int i = 0;
        int j = -1;
        for(Iterator<String[]> dev = beSelected.iterator(); dev.hasNext();)
        {
            String text[] = dev.next();
            btn[i] = new Button(this);
            btn[i].setId(i);
            btn[i].setText(text[0]);
            System.out.println("手机在判断  "  +  text[1]);
            if (text[1].equals("on"))
            {
                btn[i].setBackgroundColor(Color.rgb(224, 255, 64));//表示亮
                btn[i].setHint("off");//表示可执行指令
            }
            else//off
            {
                btn[i].setBackgroundColor(Color.rgb(184, 184, 184));//表示灭
                btn[i].setHint("on");//表示可执行指令
            }
            RelativeLayout.LayoutParams btParams = new RelativeLayout.LayoutParams ((width-50)/4,200);  //设置按钮的宽度和高度
            if (i%4 == 0) {
                j++;
            }
            btParams.leftMargin = 10+ ((width-50)/4+10)*(i%4);   //横坐标定位
            btParams.topMargin = 230 + 250*j;   //纵坐标定位


            mainlayout.addView(btn[i],btParams);   //将按钮放入layout组件
            i++;

        }
        RelativeLayout.LayoutParams btParams = new RelativeLayout.LayoutParams ((width-50)/4,200);
        if (count%4 == 0) {
            j++;
        }
        btParams.leftMargin = 10+ ((width-50)/4+10)*(count%4);   //横坐标定位
        btParams.topMargin = 230 + 250*j;   //纵坐标定位
        mainlayout.addView(add,btParams);

        setContentView(mainlayout);


        if( status[1].equals("LOGIN_SUCCESS") )
        {
            conn.setText(status[0]);
        }
        for(i = 0 ;i<count ; i++)
        {
            btn[i].setTag(i);
            btn[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int tag = (Integer)v.getTag();
                    String command = "Command," + btn[tag].getText().toString() + "," + btn[tag].getHint().toString() + ",";
                    System.out.println("手机按键发出指令 " + command);
                    if( (clientThread != null) && (clientThread.socket != null) && (clientThread.socket.isConnected()))
                    {
                        Message msg = new Message();
                        msg.what = 0x345;
                        msg.obj = command;
                        clientThread.revHandler.sendMessage(msg);
                        if (btn[tag].getHint().toString().equals("on"))
                        {
                            btn[tag].setBackgroundColor(Color.rgb(224, 255, 64));//原先是灭，现在点亮
                            btn[tag].setHint("off");
                        }
                        else
                        {
                            btn[tag].setBackgroundColor(Color.rgb(184, 184, 184));//原先是亮，现在灭掉
                            btn[tag].setHint("on");
                        }
                    }
                }
            });
        }
    }


}