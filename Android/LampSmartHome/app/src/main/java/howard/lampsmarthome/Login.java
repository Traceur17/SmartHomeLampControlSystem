package howard.lampsmarthome;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class Login extends Activity {

    public static String[] loginMSG = new String[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        final TextView addr = findViewById(R.id.addr);
        final EditText password = findViewById(R.id.password);
        final Button login = findViewById(R.id.login);


        if ( !MainActivity.status[1].equals("LOGIN_SUCCESS") )
        {
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loginMSG[0] = addr.getText().toString();
                    loginMSG[1] = password.getText().toString();
                    if (  !loginMSG[0].equals(" ") )//(( !loginMSG[0].equals(" ")) && ( !loginMSG[1].equals(" ") ))
                    {
                        MainActivity.clientThread = new ClientThread(MainActivity.handler);
                        new Thread(MainActivity.clientThread).start();

                        //Toast.makeText(getApplicationContext(), "正在连接", Toast.LENGTH_LONG).show();

                        synchronized (Thread.currentThread())
                        {
                            try
                            {
                                Thread.currentThread().wait(1000);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }

                        if ( (MainActivity.clientThread.socket != null) && (MainActivity.clientThread.socket.isConnected())) {
                            String data[] = new String[2];
                            data[0] = loginMSG[0];
                            data[1] = "LOGIN_SUCCESS";
                            Intent intent = new Intent();
                            intent.putExtra("status",data);
                            setResult(1,intent);

                            Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else
                        {
                            password.setText("");
                            Toast.makeText(getApplicationContext(), "连接失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(),"请输入IP和密码",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else if (MainActivity.status[1].equals("LOGIN_SUCCESS"))
        {
            addr.setText(MainActivity.status[0]);
            password.setText("······");
            login.setText("断开连接");
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try
                    {
                        if(MainActivity.clientThread.socket != null)
                        {
                            MainActivity.clientThread.socket.close();
                        }

                        String data[] = new String[2];
                        data[0] = loginMSG[0];
                        data[1] = "NO_LOGIN";
                        Intent intent = new Intent();
                        intent.putExtra("status",data);
                        setResult(1,intent);

                        Toast.makeText(getApplicationContext(),"已断开连接",Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }


    }
}
