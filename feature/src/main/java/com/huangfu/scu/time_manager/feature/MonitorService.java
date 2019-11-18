package com.personal.timealarm;

import android.app.KeyguardManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;

import java.util.List;

public class MonitorService extends Service {

    private SharedPreferences data;
    private boolean isMonitor = false;
    private boolean isOnAlarm;
    private long curLastTime = 0;
    private long curStopTime = 0;
    private int type;
    private String lastApp = "";

    private String[] items;

    Vibrator vibrator;
    String url;
    //自定义文件播放器
    MediaPlayer mMediaPlayer;
    Boolean noSongFile;
    //系统文件播放
    RingtoneManager manager;
    int  ringToneCount;

    public IBinder onBind(Intent intent) {
        return new MyMsg();
    }

    public void onCreate() {
        super.onCreate();
        data = getSharedPreferences("data", MODE_PRIVATE);
        items = data.getString("packageNames", " ").split(" ");

        type = data.getInt("type", 0);
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);

        url = data.getString("songFileUrl",null);
        mMediaPlayer = new MediaPlayer();

        manager = new RingtoneManager(MonitorService.this);
        Cursor cursor = manager.getCursor();
        ringToneCount = cursor.getCount();
        int position = (int) (Math.random() * ringToneCount);
        Uri uri = manager.getRingtoneUri(position);

        if(url!=null)
        {
            try {
                mMediaPlayer.setDataSource(url);
                mMediaPlayer.prepare() ;
                noSongFile = false;
//                Log.i("song","设置音乐路径成功");
            }catch (Exception e)
            {
                e.printStackTrace();
//                Log.i("song","设置出错");
            }
        }
        else
        {
            try {
                noSongFile = true;
                mMediaPlayer.setDataSource(MonitorService.this,uri);
//                Log.i("song","没有找到歌曲路径");
                mMediaPlayer.prepare();
                mMediaPlayer.setLooping(true);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    public void onDestroy() {
        super.onDestroy();
        isMonitor = false;
        vibrator.cancel();
        if(mMediaPlayer.isPlaying())
            mMediaPlayer.stop();
        mMediaPlayer.release();

    }

    /**
     * 获取顶端app包名
     */
    private String getTopApp(Context context) {
        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
            UsageStatsManager m = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (m != null) {
                long now = System.currentTimeMillis();
                //获取?秒之内的应用数据
                List<UsageStats> stats = m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 60000, now);
                String topActivity = "";
                //取得最近运行的一个app，即当前运行的app
                if ((stats != null) && (!stats.isEmpty())) {
                    int j = 0;
                    for (int i = 0; i < stats.size(); i++) {
                        if (stats.get(i).getLastTimeUsed() > stats.get(j).getLastTimeUsed()) {
                            j = i;
                        }
                    }
                    topActivity = stats.get(j).getPackageName();
                }
                return topActivity;
            }
        }
        return "";
    }

    /**
     * 判断是否是被监控的app
     */
    public boolean isOnList(String item) {
        for (String item1 : items) {
            if (item.equals(item1)) {
                return true;
            }
        }
        return false;
    }

    protected long getCurLastTime() {
        return curLastTime;
    }

    public long getCurStopTime() {
        return curStopTime;
    }

    public class MyMsg extends Binder{
        public long getCurLastTime() {
            return MonitorService.this.getCurLastTime();
        }

        public long getCurStopTime() {
            return MonitorService.this.getCurStopTime();
        }
    }
