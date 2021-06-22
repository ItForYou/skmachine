package kr.co.itforone.skmachine;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        context = this.getBaseContext();

        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23){ // 마시멜로(안드로이드 6.0) 이상 권한 체크
            TedPermission.with(context)
                    .setPermissionListener(permissionlistener)
                    //.setRationaleMessage("앱 사용을 위해 권한을 허용해 주세요.")
                    .setDeniedMessage("앱 사용을 위해 권한 설정이 필요합니다.\n [설정] > [권한] 에서 사용으로 변경해 주세요.")
                    .setGotoSettingButton(true)
                    .setPermissions(
                            //Manifest.permission.ACCESS_FINE_LOCATION,
                            //Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, // 기기, 사진, 미디어, 파일 엑세스 권한
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA
                    )
                    .check();
        } else {
            initView();
        }
    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            initView(); // 권한이 승인되었을 때 실행할 함수
        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            Toast.makeText(context, "권한 요청에 동의 해주셔야 이용 가능합니다. [설정] > [권한] 에서 사용으로 변경해 주세요.", Toast.LENGTH_SHORT).show();
            // 앱종료
            moveTaskToBack(true);
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    };

    private void initView() {
        // 핸들러로 이용해서 1초간 머물고 이동이 됨
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1000);
    }

}