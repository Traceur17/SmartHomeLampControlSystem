package howard.lampsmarthome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;

public class AddDevices extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_devices);
        //ToDo
        //从服务器端接收 在线设备 数据，处理后显示出来
        //选择要添加的设备，返回后添加到main页面上

        Toast.makeText(getApplicationContext(),"加载中",Toast.LENGTH_SHORT).show();

        int count = MainActivity.ZigBeeDevList.size();
        if(count == 0)
        {
            Toast.makeText(getApplicationContext(),"没有添加设备",Toast.LENGTH_SHORT).show();
        }
        else
        {
            RelativeLayout layout = new RelativeLayout(this);

            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int width = dm.widthPixels;
            int height = dm.heightPixels;

            Button confirm = new Button(this);
            confirm.setId(999);
            confirm.setText("添加");
            RelativeLayout.LayoutParams confirmParams = new RelativeLayout.LayoutParams (200,150);
            confirmParams.leftMargin = (width-200)/2;
            confirmParams.rightMargin = (width-200)/2;
            confirmParams.topMargin = 10;
            layout.addView(confirm,confirmParams);

            final Button btn[] = new Button[count];
            int i = 0;
            int l = 0;
            ArrayList<String[]> tmp = new ArrayList<String[]>();
            for(Iterator<String[]> it = MainActivity.ZigBeeDevList.iterator() ; it.hasNext();)
            {
                String[] dev = it.next();
                btn[i] = new Button(this);
                btn[i].setId(i);
                btn[i].setText(dev[1]);
                btn[i].setHint(dev[2]);
                RelativeLayout.LayoutParams btParams = new RelativeLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT,150);  //设置按钮的宽度和高度

                btParams.topMargin = 200 + 150*i;   //纵坐标定位

                layout.addView(btn[i],btParams);   //将按钮放入layout组件
                i++;

            }

            this.setContentView(layout);
            confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    //intent.putExtra("list",MainActivity.beSelected);
                    setResult(2,intent);
                    finish();
                }
            });

            for(i = 0 ;i<count ; i++)
            {
                btn[i].setTag(i);
                btn[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int tag = (Integer)v.getTag();
                        String dev[] = new String[3];
                        dev[0] = btn[tag].getText().toString();
                        dev[1] = btn[tag].getHint().toString();
                        dev[2] = Integer.toString(btn[tag].getId());

                        MainActivity.beSelected.add(dev);
                        btn[tag].setBackgroundColor(Color.rgb(52, 52, 52));
                    }
                });
            }

        }

    }
}
