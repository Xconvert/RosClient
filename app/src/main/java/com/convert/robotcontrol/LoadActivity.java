package com.convert.robotcontrol;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.convert.robotcontrol.callback.LoadCallback;
import com.convert.robotcontrol.socket.WsManager;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoadActivity extends AppCompatActivity implements LoadCallback {

    private final String TAG = "LoadActivity";
    private final int MSG_PROGRESS = 0;
    private final int MSG_ADAPTER_CHANGE = 1;
    private static final int CORE_POOL_SIZE = 8;
    private String mDevAddress;// 本机IP地址-完整
    private String mLocAddress;// 局域网IP地址头,如：192.168.1.
    private Runtime mRun = Runtime.getRuntime();// 获取当前运行环境，来执行ping，相当于windows的cmd
    private Process mProcess = null;// 进程
    private final String PING = "ping -c 1 -w 1 ";// 其中 -c 1为发送的次数，-w 表示发送后等待响应的时间
    private List<String> mIpList = new ArrayList<>();// ping成功的IP地址
    private ExecutorService mExecutor;// 线程池对象
    private ProgressBar mProgressBar;
    private int mProgress = 2;
    private TextView mTips;

    private ArrayAdapter<String> mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
        initView();
        WsManager.getInstance().registerLoadCallBack(this);
        scan();

    }

    private void initView() {
        //全屏
        hideSystemUI();

        mTips = (TextView) findViewById(R.id.textView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        ListView listView = (ListView) findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "onItemClick: " + mIpList.get(position));
                WsManager.getInstance().init(mIpList.get(position));
//                Intent intent = new Intent(LoadActivity.this, MainActivity.class);
//                startActivity(intent);
            }
        });
        mAdapter = new ArrayAdapter<>(LoadActivity.this,android.R.layout.simple_list_item_1,mIpList);
        listView.setAdapter(mAdapter);
    }

    public void scan() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mDevAddress = getHostIP();// 获取本机IP地址
                mLocAddress = getLocAddrIndex(mDevAddress);// 获取本地ip前缀
                Log.d(TAG, "开始扫描设备,本机Ip为：" + mDevAddress);

                mExecutor = Executors.newFixedThreadPool(CORE_POOL_SIZE);

                // 新建线程池
                for (int i = 1; i < 255; i++) {// 创建256个线程分别去ping
                    final int lastAddress = i;// 存放ip最后一位地址 1-255

                    Runnable run = new Runnable() {

                        @Override
                        public void run() {
                            String ping = PING + mLocAddress + lastAddress;
                            String curNetIp = mLocAddress + lastAddress;
                            if (mDevAddress.equals(curNetIp)) // 如果与本机IP地址相同,跳过
                                return;

                            try {
                                mProcess = mRun.exec(ping);

                                int result = mProcess.waitFor();

                                mHandler.sendEmptyMessage(MSG_PROGRESS);

                                if (result == 0) {
                                    Log.d(TAG, "扫描成功,Ip地址为：" + curNetIp);
                                    Message msg = mHandler.obtainMessage(MSG_ADAPTER_CHANGE);
                                    msg.obj = curNetIp;
                                    mHandler.sendMessage(msg);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "扫描异常" + e.toString());
                            } finally {
                                if (mProcess != null)
                                    mProcess.destroy();
                            }
                        }
                    };

                    mExecutor.execute(run);
                }

                mExecutor.shutdown();

                while (!mExecutor.isTerminated()) { }

                Log.d(TAG, "扫描结束,总共成功扫描到" + mIpList.size() + "个设备.");
            }
        }).start();
    }

    private String getLocAddrIndex(String devAddress) {
        if (devAddress != null && !devAddress.equals("")) {
            return devAddress.substring(0, devAddress.lastIndexOf(".") + 1);
        }
        Toast.makeText(LoadActivity.this, getText(R.string.connect_error), Toast.LENGTH_SHORT).show();
        //没联网
        Log.w(TAG, "getLocAddrIndex: have not connected to wifi!");
        finish();
        return null;
    }

    //获取ip地址
    public String getHostIP() {

        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i(TAG, "SocketException");
            e.printStackTrace();
        }
        return hostIp;

    }

    private void hideSystemUI() {
        //全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        if (Build.VERSION.SDK_INT >= 19) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
            getWindow().setAttributes(params);
        }
    }

    private static byte[] ipv4Address2BinaryArray(String ipAdd){
        byte[] binIP = new byte[4];
        String[] strs = ipAdd.split("\\.");
        for(int i=0;i<strs.length;i++){
            binIP[i] = (byte) Integer.parseInt(strs[i]);
        }
        return binIP;
    }

    private Handler mHandler = new Handler() {
        private final int MAX_PROGRESS = 254;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS:
                    ++mProgress;
                    mProgressBar.setProgress(100 * mProgress / MAX_PROGRESS);
                    if (mProgress == MAX_PROGRESS){
                        mTips.setText(getText(R.string.tip2));
                        mProgressBar.setVisibility(View.GONE);
                    }
                    break;
                case MSG_ADAPTER_CHANGE:
                    String curNetIp = (String) msg.obj;
                    mIpList.add(curNetIp);
                    mAdapter.notifyDataSetChanged();
                    break;
                default:
            }
        }
    };

    @Override
    public void report(boolean isSucceed) {
        if (isSucceed){
            Intent intent = new Intent(LoadActivity.this, MainActivity.class);
            startActivity(intent);
        }
        else {
            Toast.makeText(LoadActivity.this, getText(R.string.connect_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        WsManager.getInstance().unRegisterLoadCallBack();
    }
}
