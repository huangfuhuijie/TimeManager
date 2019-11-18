package com.personal.timealarm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class MySettings extends AppCompatActivity{

    private SharedPreferences data;
    private SharedPreferences.Editor editor;
    private SimpleRoundProgress view_lastTimeBar;
    private SimpleRoundProgress view_stopTimeBar;
    private TextView view_curLastTimeValue;
    private TextView view_curStopTimeValue;
    private long curLastTime = 0, curStopTime = 0;

    ServiceConnection connection=new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MonitorService.MyMsg msg = (MonitorService.MyMsg) iBinder;
            curLastTime = msg.getCurLastTime();
            curStopTime = msg.getCurStopTime();
            myHandler.sendMessage(new Message());
        }
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @SuppressLint("HandlerLeak")
    Handler myHandler = new Handler(){
        public void handleMessage(Message msg) {
            initContent();
        }
    };

    @SuppressLint("CommitPrefEdits")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("设置");

        view_lastTimeBar = findViewById(R.id.widget_lastTimeBar);
        view_stopTimeBar = findViewById(R.id.widget_stopTimeBar);
        view_curLastTimeValue = findViewById(R.id.widget_curLastTimeValue);
        view_curStopTimeValue = findViewById(R.id.widget_curStopTimeValue);

        data = getSharedPreferences("data", MODE_PRIVATE);
        editor = data.edit();

        Intent service = new Intent(this, MonitorService.class);
        bindService(service, connection, Context.BIND_AUTO_CREATE);
    }

    protected void onStart() {
        super.onStart();
        ActionBar actionBar = this.getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.widget_lastTime:
                getAndSetData(1);
                break;
            case R.id.widget_stopTime:
                getAndSetData(2);
                break;
            case R.id.widget_type:
                getAndSetData(3);
                break;
            case R.id.widget_ring:
                int hasPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
                if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 11);
                    return;
                }
                if (data.getBoolean("isOnAlarm", false)) {
                    Toast.makeText(MySettings.this, "请先关闭提醒", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(MySettings.this,SongList.class);
                    startActivityForResult(intent,1);
                }
                break;
            case R.id.widget_sleepTime:
                getSleepTime();
                break;
            case R.id.widget_how_to_use:
                Intent intent = new Intent(MySettings.this,HowToUse.class);
                startActivityForResult(intent,1);
        }
    }

    public void getAndSetData(final int TYPE) {
        if (data.getBoolean("isOnAlarm", false)) {
            Toast.makeText(MySettings.this, "请先关闭提醒", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (TYPE == 1 || TYPE == 2) {
            final View VIEW_EDIT = View.inflate(this, R.layout.activity_edit, null);
            if(TYPE == 1){
                builder.setTitle("设置"+getString(R.string.lastTime)).setIcon(R.drawable.new_clock);
            }else{
                builder.setTitle("设置"+getString(R.string.stopTime)).setIcon(R.drawable.new_clock);
            }
            builder.setView(VIEW_EDIT);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @SuppressLint("SetTextI18n")
                public void onClick(DialogInterface dialogInterface, int i) {
                    EditText view_editText = VIEW_EDIT.findViewById(R.id.widget_minute);
                    String res = view_editText.getText().toString();
                    if ("".equals(res) || res.charAt(0)=='0') {
                        Toast.makeText(MySettings.this, "请输入正确的数字！", Toast.LENGTH_SHORT).show();
                    }else{
                        if(TYPE == 1){
                            editor.putLong("lastTime",Long.parseLong(res));
                            TextView tv = findViewById(R.id.widget_lastTime);
                            tv.setText(getContentItemString(tv, "玩耍时间", res+" min"));
                        }else{
                            editor.putLong("stopTime",Long.parseLong(res));
                            TextView tv = findViewById(R.id.widget_stopTime);
                            tv.setText(getContentItemString(tv, "学习时间", res+" min"));
                        }
                        editor.apply();
                    }
                    dialogInterface.dismiss();
                }
            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        } else if (TYPE == 3) {
            if (data.getBoolean("isOnSleepAlarm", false)) {
                Toast.makeText(MySettings.this, "请先关闭提醒", Toast.LENGTH_SHORT).show();
                return;
            }
            int hasPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 11);
                return;
            }
            final String[] arrayOfString = {"震动", "响铃", "弹窗"};
            builder.setTitle(getString(R.string.type)).setIcon(R.drawable.new_clock);
            builder.setSingleChoiceItems(arrayOfString, data.getInt("type",0), new DialogInterface.OnClickListener()    {
                public void onClick(DialogInterface dialogInterface, int i){
                    editor.putInt("type", i);
                    editor.apply();
                    TextView tv = findViewById(R.id.widget_type);
                    tv.setText(getContentItemString(tv, "提醒方式", arrayOfString[i]));
                    dialogInterface.dismiss();
                }
            }).setCancelable(true).create().show();
        }
    }

    public void getSleepTime()
    {
        if (data.getBoolean("isOnSleepAlarm", false)) {
            Toast.makeText(MySettings.this, "请先关闭提醒", Toast.LENGTH_SHORT).show();
            return;
        }
        String sleepTime = data.getString("sleepTime","23:00");
        int hour = Integer.valueOf(sleepTime.substring(0,sleepTime.indexOf(':')));
        int minute = Integer.valueOf(sleepTime.substring(sleepTime.indexOf(':')+1));
        new TimePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT,new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int i, int i1) {
                TextView tv = findViewById(R.id.widget_sleepTime);
                if(i1>=10){
                    editor.putString("sleepTime",i+":"+i1);
                    tv.setText(getContentItemString(tv, "睡觉时间", i+":"+i1));
                }else{
                    editor.putString("sleepTime",i+":0"+i1);
                    tv.setText(getContentItemString(tv, "睡觉时间", i+":0"+i1));
                }
//                Log.i("setTime","设置时间为: "+i+":"+i1);
                editor.apply();
            }
        },hour,minute,true).show();
    }

    /**
     * 初始化配置
     */
    @SuppressLint("SetTextI18n")
    public void initContent() {
        String type = "";
        int temp = data.getInt("type", 0);
        switch (temp) {
            case 0:
                type = "震动";
                break;
            case 1:
                type = "响铃";
                break;
            case 2:
                type = "弹窗";
                break;
        }
        String keys[] = {getString(R.string.lastTime),
                         getString(R.string.stopTime),
                         getString(R.string.sleepTime),
                         getString(R.string.type),
                         getString(R.string.ring)};

        String values[] = { data.getLong("lastTime", 40L)+" min",
                            data.getLong("stopTime", 60L)+" min",
                            data.getString("sleepTime","23:00"),
                            type,
                            data.getString("songName","随机")};

        TextView tvs[] = {findViewById(R.id.widget_lastTime),
                         findViewById(R.id.widget_stopTime),
                         findViewById(R.id.widget_sleepTime),
                         findViewById(R.id.widget_type),
                         findViewById(R.id.widget_ring)};

        for(int i=0;i<tvs.length;i++){
            tvs[i].setText(getContentItemString(tvs[i], keys[i], values[i]));
        }

        view_curLastTimeValue.setText(getProValue(curLastTime));
        view_curStopTimeValue.setText(getProValue(curStopTime));
        int lastValue = (int) (curLastTime*100/data.getLong("lastTime", 30L)/60000);
        int stopValue = (int) (curStopTime*100/data.getLong("stopTime", 5L)/60000);
        view_lastTimeBar.setProgress(lastValue);
        view_stopTimeBar.setProgress(stopValue);
    }

    /**
     * 转化为 分：秒
     */
    private String getProValue(long pro){
        long m = pro/1000/60;
        long s = pro/1000 - m*60;
        return m+":"+(s>9?"":"0")+s;
    }

    /**
     * 获取更新后Item的显示内容，空格多少什么的
     */
    public SpannableStringBuilder getContentItemString(TextView tv, String key, String value){
        TextPaint paint = tv.getPaint();
        int width = tv.getMeasuredWidth() - tv.getPaddingLeft() - tv.getPaddingRight();
        float width1 = paint.measureText(key);

        SpannableStringBuilder v = new SpannableStringBuilder(value);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(91, 88, 88));
        v.setSpan(colorSpan, 0, v.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        v.setSpan(new RelativeSizeSpan(0.75f), 0, v.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        float width2 = paint.measureText(value)*0.75f;
        float spaceWidth = paint.measureText(" ");
        int spaceCount = (int)((width - width1 - width2) / spaceWidth);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(key);
        for (int i = 0; i < spaceCount; i++) sb.append(" ");
            sb.append(v);
            return sb;
        }

        protected void onDestroy() {
            super.onDestroy();
            unbindService(connection);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent _data) {
            if(resultCode==1)
            {
            TextView tv = findViewById(R.id.widget_ring);
            tv.setText(getContentItemString(tv, "铃声",data.getString("songName","随机")));
        }
    }
}
