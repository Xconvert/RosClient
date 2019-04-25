package com.convert.robotcontrol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.convert.robotcontrol.callback.Callback;
import com.convert.robotcontrol.view.OrientationView;
import com.convert.robotcontrol.view.RockerView;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements Callback {

    private final String TAG = "MainActivity";
    private final int IDLE = 0;//空闲模式
    private final int BUILD = 1;//建图模式
    private final int NVG = 2;//导航模式
    private final int SHUTDOWN = 3;//关闭机器人
    private final int EXIT = 4;//退出
    //操作模式
    private final int MODE_UNKNOWN = -1;
    private final int MODE_DRAG = 0;
    private final int MODE_ZOOM = 1;
    private final int MSG_UPDATE_THUMB = 0;
    private final int MSG_UPDATE_MAP = 1;
    private final int MSG_VIDEO = 2;
    private final String KEY_MAP = "map";
    private final String KEY_VIDEO = "video";
    private int mMode = MODE_UNKNOWN;
    private int mRobotMode = IDLE;
    private int mRobotState = IDLE;
    private ImageView mVideo;
    private ImageView mOpenMapBtn;
    private Button mCloseMapBtn;
    private ImageView mMap;
    private RockerView mRockerView;
    private LinearLayout mFuncLayout;
    private Button mFuncNvg;
    private Button mFuncClearPoint;
    private LinearLayout mPoseLayout;
    private TextView mTipSetPose;
    private Button mSetPoseBtn;
    private OrientationView mOrientationView;
    private static boolean sIsOpenMap = false;
    private Spinner mSpinner;//功能的下拉列表
    //用于判断是否进行姿态设置
    private static boolean sIsEstimatePose = false;

    private RobotManager mRobotManager;
    //手势参数
    private PointF mStartPoint;
    private float mStartDis;
    //用于判断是否点击事件
    boolean mIsClick = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");
        checkPermission();
        initView();

        mRobotManager = RobotManager.getInstance();
        mRobotManager.init(this);
        mRobotManager.registerCallBack(this);
        mStartPoint = new PointF();

        updateMap(mRobotManager.getDesMap());

    }

    private void initView() {
        //全屏沉浸模式
        hideSystemUI();

        //============================
        //========= 手动界面 ==========
        //============================
        mVideo = (ImageView) findViewById(R.id.video);
        mOpenMapBtn = (ImageView) findViewById(R.id.open_map_button);
        mOpenMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMap();
            }
        });

        mRockerView = (RockerView) findViewById(R.id.rocker_view);
        if (mRockerView != null) {
            mRockerView.setOnAngleChangeListener(new RockerView.OnAngleChangeListener() {
                @Override
                public void onStart() {
                }

                @Override
                public void angle(double lineSpeed, double angularVelocity) {
                    //Log.d(TAG, "lineSpeed: " + lineSpeed + ", angular: " + angularVelocity);
                    mRobotManager.doControl(lineSpeed, angularVelocity);
                }

                @Override
                public void onFinish() {
                    mRobotManager.doControl(0, 0);
                }
            });
        }
        mSpinner = (Spinner) findViewById(R.id.spinner);
        //数据
        ArrayList<String> data_list = new ArrayList<>();
        data_list.add(getString(R.string.mode_idle));
        data_list.add(getString(R.string.mode_build));
        data_list.add(getString(R.string.mode_nvg));
        data_list.add(getString(R.string.mode_shutdown));
        data_list.add(getString(R.string.mode_exit));
        //适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data_list);
        //设置样式
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //加载适配器
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    //导航可能会结束...
                    case IDLE:
                        mRobotMode = IDLE;
                        if (mRobotState == NVG) {
                            mRobotState = IDLE;
                            mFuncNvg.setText(getString(R.string.fun_nvg));
                            mFuncNvg.setTextColor(Color.BLACK);
                        }
                        break;
                    case BUILD:
                        mRobotMode = BUILD;
                        Toast.makeText(MainActivity.this, getString(R.string.start_build_map), Toast.LENGTH_SHORT).show();
                        if (mRobotState == NVG) {
                            mRobotState = BUILD;
                            mFuncNvg.setText(getString(R.string.fun_nvg));
                            mFuncNvg.setTextColor(Color.BLACK);
                        }
                        break;
                    case NVG:
                        mRobotMode = NVG;
                        break;
                    case SHUTDOWN:
                        mRobotMode = SHUTDOWN;
                        if (mRobotState == NVG) {
                            mRobotState = SHUTDOWN;
                            mFuncNvg.setText(getString(R.string.fun_nvg));
                            mFuncNvg.setTextColor(Color.BLACK);
                        }
                        //...
                        break;
                    case EXIT:
                        finish();
                        break;
                    default:
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //============================
        //========= 地图界面 ==========
        //============================
        mMap = (ImageView) findViewById(R.id.map);
        mCloseMapBtn = (Button) findViewById(R.id.close_map_button);
        mCloseMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeMap();
                mOpenMapBtn.setImageBitmap(mRobotManager.getThumbnails());
            }
        });
        mFuncLayout = (LinearLayout) findViewById(R.id.map_func_layout);
        mFuncNvg = (Button) findViewById(R.id.func_ngv);
        mFuncNvg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRobotState != NVG) {
                    //开始导航
                    mRobotState = NVG;
                    mSpinner.setSelection(NVG);
                    mRobotMode = NVG;
                    mFuncNvg.setText(getString(R.string.nvg));
                    mFuncNvg.setTextColor(Color.GREEN);
                    Toast.makeText(MainActivity.this, getString(R.string.tip_nvg), Toast.LENGTH_SHORT).show();
                    //...
                    mRobotManager.doNavigationGoal();
                    //...
                    //mMapManager.saveMapToFile();
                }
            }
        });
        mFuncClearPoint = (Button) findViewById(R.id.func_clear_point);
        mFuncClearPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFuncLayout.setVisibility(View.GONE);
                mRobotManager.clearDesPos();
                if (mRobotState == NVG) {
                    //导航结束
                    mRobotState = IDLE;
                    mSpinner.setSelection(IDLE);
                    mRobotMode = IDLE;
                    mFuncNvg.setText(getString(R.string.fun_nvg));
                    mFuncNvg.setTextColor(Color.BLACK);
                    //...
                }
            }
        });

        mPoseLayout = (LinearLayout) findViewById(R.id.map_pose_layout);
        mTipSetPose = (TextView) findViewById(R.id.tip_set_pose);
        mSetPoseBtn = (Button) findViewById(R.id.func_set_pose);
        mSetPoseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sIsEstimatePose) {
                    //sure the pose, and submit it
                    if (mRobotManager.isSetRobotPoint()) {
                        //view operation
                        sIsEstimatePose = false;
                        mTipSetPose.setVisibility(View.GONE);
                        mOrientationView.setVisibility(View.GONE);
                        mSetPoseBtn.setText(getText(R.string.set_position));

                        mRobotManager.doPoseEstimate();
                        Toast.makeText(MainActivity.this, getText(R.string.succeed_set_pose), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, getText(R.string.tip_sure_pose), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    //enter the pose estimation mode, you can modify the pose and position.
                    sIsEstimatePose = true;
                    //view operation
                    mTipSetPose.setVisibility(View.VISIBLE);
                    mOrientationView.setVisibility(View.VISIBLE);
                    mSetPoseBtn.setText(getText(R.string.sure_pose));

                    mOrientationView.setAngle(mRobotManager.getOrientationAngle());
                }
            }
        });
        mOrientationView = (OrientationView) findViewById(R.id.orientation_view);
        mOrientationView.setOnAngleChangeListener(new OrientationView.OnAngleChangeListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void angle(double stdAngle) {
                mRobotManager.setOrientationAngle(stdAngle);
            }

            @Override
            public void onFinish() {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //全屏沉浸模式
        hideSystemUI();
    }

    private void checkPermission() {
        final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, getText(R.string.tip3), Toast.LENGTH_SHORT).show();
            }
            //申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);

        }

    }

    //全屏沉浸模式
    private void hideSystemUI() {
        //全屏第一种方法
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        //全屏第二种方式，因为上一种不彻底
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        if (Build.VERSION.SDK_INT >= 19) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
            getWindow().setAttributes(params);
        }
    }

    //打开地图
    private void openMap() {
        mMap.setVisibility(View.VISIBLE);
        mCloseMapBtn.setVisibility(View.VISIBLE);
        mPoseLayout.setVisibility(View.VISIBLE);

        mVideo.setVisibility(View.GONE);
        mRockerView.setVisibility(View.GONE);
        mOpenMapBtn.setVisibility(View.GONE);
        mSpinner.setVisibility(View.GONE);

        sIsOpenMap = true;
    }

    //关闭地图
    private void closeMap() {
        mMap.setVisibility(View.GONE);
        mCloseMapBtn.setVisibility(View.GONE);
        mPoseLayout.setVisibility(View.GONE);

        mVideo.setVisibility(View.VISIBLE);
        mRockerView.setVisibility(View.VISIBLE);
        mOpenMapBtn.setVisibility(View.VISIBLE);
        mSpinner.setVisibility(View.VISIBLE);

        sIsOpenMap = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (sIsOpenMap) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:// 按下
                    mStartPoint.set(event.getX(), event.getY());
                    mMode = MODE_DRAG;
                    break;
                case MotionEvent.ACTION_MOVE:// 移动
                    if (moveDistance(event) > 5f) {
                        mIsClick = false;
                    }
                    if (mMode == MODE_ZOOM) {
                        zoom(event);
                    } else if (mMode == MODE_DRAG) {
                        drag(event);
                    }
                    break;
                case MotionEvent.ACTION_UP:// 抬起
                    mRobotManager.logMapArr();
                    if (mIsClick) {
                        //触发点击事件
                        if (sIsEstimatePose) {
                            //to sure the robot pose
                            mRobotManager.setRobotPos(new PointF(event.getX(), event.getY()));
                        }
                        else {
                            // to set destination point
                            mRobotManager.setDesPos(new PointF(event.getX(), event.getY()));
                            //开启地图功能按钮
                            mFuncLayout.setVisibility(View.VISIBLE);
                            //动画...
                            if (mRobotState == NVG) {
                                //导航结束...
                                mRobotState = IDLE;
                                mSpinner.setSelection(IDLE);
                                mRobotMode = IDLE;
                                mFuncNvg.setText(getString(R.string.fun_nvg));
                                mFuncNvg.setTextColor(Color.BLACK);
                            }
                        }
                    }
                    mIsClick = true;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN: //两点同时触控
                    mMode = MODE_ZOOM;
                    mStartDis = distance(event);//两指起始距离
                    break;
                default:
            }
        }
        return true;
    }

    //地图操作
    private float moveDistance(MotionEvent event) {

        float dx = event.getX() - mStartPoint.x;
        float dy = event.getY() - mStartPoint.y;
        // 使用勾股定理返回两点之间的距离
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float distance(MotionEvent event) {

        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        // 使用勾股定理返回两点之间的距离
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private PointF getCenter(MotionEvent event) {

        float x = (event.getX(1) + event.getX(0)) / 2;
        float y = (event.getY(1) + event.getY(0)) / 2;
        return new PointF(x, y);
    }

    private void zoom(MotionEvent event) {
        //只有同时触屏两个点的时候才执行
        if (event.getPointerCount() < 2) return;
        float endDis = distance(event);// 结束距离
        PointF center = getCenter(event);
        //new PointF(event.getX(), event.getY());
        if (Math.abs(endDis - mStartDis) > 10f) { // 两个手指像素大于10
            float scale = endDis / mStartDis;// 得到缩放倍数
            mStartDis = endDis;//重置距离
            mRobotManager.zoom(center, scale);
        }
    }

    public void drag(MotionEvent event) {
        float dx = event.getX() - mStartPoint.x; // 得到x轴的移动距离
        float dy = event.getY() - mStartPoint.y; // 得到x轴的移动距离

        mStartPoint.set(event.getX(), event.getY());
        //在当前基础上移动
        mRobotManager.moveMap(dx, dy);
    }

    @Override
    public void updateMap(Bitmap bitmap) {
        Message message = mHandler.obtainMessage(MSG_UPDATE_MAP);
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_MAP, bitmap);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    @Override
    public void updateThumb(Bitmap bitmap) {
        Message message = mHandler.obtainMessage(MSG_UPDATE_THUMB);
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_MAP, bitmap);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    @Override
    public void updateVideo(Bitmap bitmap) {
        Message message = mHandler.obtainMessage(MSG_VIDEO);
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_VIDEO, bitmap);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRobotManager.close();
        mHandler = null;
        Log.i(TAG, "onDestroy:");
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_THUMB:
                    Bundle data = msg.getData();
                    if (data != null) {
                        Bitmap map = data.getParcelable(KEY_MAP);
                        if (map != null) {
                            mOpenMapBtn.setImageBitmap(map);
                            Log.i(TAG, "handleMessage: mOpenMapBtn.setImageBitmap(map);");
                        }
                    }
                    break;
                case MSG_UPDATE_MAP:
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        Bitmap map = bundle.getParcelable(KEY_MAP);
                        if (map != null) {
                            mMap.setImageBitmap(map);
                        }
                    }
                    break;
                case MSG_VIDEO:
                    //mVideo
                    Bundle videoData = msg.getData();
                    if (videoData != null) {
                        Bitmap video = videoData.getParcelable(KEY_VIDEO);
                        if (video != null) {
                            mVideo.setImageBitmap(video);
                            //Log.d(TAG, "handleMessage: mVideo.setImageBitmap(video);");
                        }
                    }
                    break;
                default:
            }
        }
    };

}
