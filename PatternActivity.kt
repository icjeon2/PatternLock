
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.ssenstone.stonepass.libstonepass_sdk.SSUserManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import kr.co.hi.hiup.R;
import timber.log.Timber;


public class PatternActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = getClass().getSimpleName();
    private Context mContext;

    private final int REG_FIRST = 1;    // 등록
    private final int REG_SECOND = 2;   // 등록 확인
    private int MODE = AUTH;            // 프로세스 모드

    private String pattern1;            // 등록시 저장할 패턴
    private String pattern2;            // 등록시 저장할 패턴
    private String op;                  // 프로세스 구분(Reg = 등록, Auth = 인증, Dereg = 해지)

    private long[] vibratorPattern = {100, 50, 100, 50};

    private ImageView imageClose;
    private PatternLockView patternLockView;  // 패턴뷰

    private TextView title;
    private TextView desc;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pattern);
        mContext = this;

        op = getIntent().getStringExtra("op");        
        initView();

        // 패턴 리스너 등록
        patternLockView.addPatternLockListener(patternLockViewListener);
    }

    public void initView() {
        imageClose = findViewById(R.id.image_close);
        imageClose.setOnClickListener(this);

        title = findViewById(R.id.text_title);
        desc = findViewById(R.id.text_descipte);

        if (op == reg) {   // 등록일 경우
            MODE = REG_FIRST;       // 등록

            // 첫번째 패턴 입력
            title.setText("새로운 패턴 등록");
            title.setVisibility(View.VISIBLE);

            // 패턴은 4자리 이상
            desc.setText("4개 이상의 점을 연결");
            desc.setVisibility(View.VISIBLE);
        } else if(op == auth) {
            MODE = AUTH;            // 인증

            // 패턴 입력
            title.setText("패턴 인증");
            title.setVisibility(View.VISIBLE);
            desc.setText("");
            desc.setVisibility(View.INVISIBLE);
        }else if(op==dereg){
            // 패턴해지
            // 패턴 입력
            title.setText("패턴 해지");
            title.setVisibility(View.VISIBLE);
            desc.setText("확인을 위해 기존패턴을 그려주세요");
            desc.setVisibility(View.VISIBLE);
        }

        // init
        patternLockView = findViewById(R.id.layout_pattern);
        //patternLockView.setCorrectStateColor(getResources().getColor(R.color.color_primary));
        //patternLockView.setNormalStateColor(Color.WHITE);
        //patternLockView.setWrongStateColor(Color.RED);
    }

    // 패턴 이벤트 리스너
    PatternLockViewListener patternLockViewListener = new PatternLockViewListener() {
        @Override
        public void onStarted() {
            Timber.d("Pattern drawing started");
        }

        @Override
        public void onProgress(List<PatternLockView.Dot> progressPattern) {
            Timber.d("Pattern progress: %s", PatternLockUtils.patternToString(patternLockView, progressPattern));
        }

        @Override
        public void onComplete(List<PatternLockView.Dot> pattern) {
            Timber.d("Pattern complete: " +
                    PatternLockUtils.patternToString(patternLockView, pattern) + ", size : " + PatternLockUtils.patternToString(patternLockView, pattern).length());

            // 패턴이 4자리 미만일 경우
            if (PatternLockUtils.patternToString(patternLockView, pattern).length() < 4) {
                Timber.e(TAG, "패턴이 4개 이하");

                // 진동 울림
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(vibratorPattern, -1); // 0.5초간 진동

                // 텍스트 설정
                desc.setText("4개 이상의 점을 연결");
                desc.setTextColor(Color.RED);   // 색상을 빨간색으로 변경
                // 1초후 실행 함
                new Handler().postDelayed(() -> {
                    desc.setTextColor(getResources().getColor(R.color.color_primary)); // 색상을 원래색으로 돌림(노랑색)
                }, 1000);
                desc.setVisibility(View.VISIBLE);
                patternLockView.clearPattern();     // 설정한 패턴을 지움
            } else {
                if (op == reg) {
                    if (MODE == REG_FIRST) { // 첫번째 패턴 입력
                        pattern1 = PatternLockUtils.patternToString(patternLockView, pattern);  // 입력한 패턴을 가져와서 저장
                        patternLockView.clearPattern();                                         // 입력한 패턴을 지움

                        // 두번째 입력 텍스트로 변경
                        title.setText("패턴을 다시 그려주세요");
                        title.setVisibility(View.VISIBLE);

                        MODE = REG_SECOND;      // 두번째 입력
                    } else if (MODE == REG_SECOND) {
                        pattern2 = PatternLockUtils.patternToString(patternLockView, pattern); // 입력한 패턴을 가져와서 저장

                        // 첫번째 입력한 패턴과 두번째 입력한 패턴을 비교
                        if (!pattern2.equalsIgnoreCase(pattern1)) { // 첫번째 입력한 패턴과 두번째 입력한 패턴이 다를 경우
                            MODE = REG_FIRST;   // 첫번째 입력

                            // 저장한 패턴 초기화
                            pattern1 = "";
                            pattern2 = "";

                            // 진동을 울림
                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(vibratorPattern, -1); // 0.5초간 진동

                            title.setVisibility(View.VISIBLE);
                            title.setText("불일치");    // 패턴 오류 텍스트 설정
                            title.setTextColor(Color.RED);                                      // 텍스트 색상 변경
                            // 1초 뒤 실행
                            new Handler().postDelayed(() -> {
                                title.setText("새로운 패턴등록");  // 첫번째 패턴 입력 텍스트로 변경
                                title.setTextColor(Color.parseColor("#00184b"));                                // 텍스트 색상 변경
                            }, 1000);

                            patternLockView.clearPattern();     // 입력한 패턴 제거
                        } else {        // 첫번째 입력한 패턴과 두번째 입력한 패턴이 일치할 경우
                          dosomething(PatternLockUtils.patternToString(patternLockView, pattern));
                        }
                    }
                }else if(op == auth || op == dereg) {
                    dosomething(PatternLockUtils.patternToString(patternLockView, pattern));
                }
            }
        }

        @Override
        public void onCleared() {
            Timber.d("Pattern has been cleared");
        }
    };

    FidoListener fidoListener = new FidoListener() {
        @Override
        public void authfailed() {

        }

        @Override
        public void authsuccess() {

        }

    };

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.image_close) {
            onBackPressed();
        }
    }
}
