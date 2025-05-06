package com.silmedy.videocall;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.messaging.FirebaseMessaging;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

/**
 * 로그인 화면
 * 1) 이미 로그인된 상태이면 HomeActivity 또는 ReceiveActivity로 즉시 이동
 * 2) 이메일/비밀번호 인증, 토큰·이름·fcm_token 저장
 * 3) fcm_token 신규 발급 시 서버 등록
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG   = "LoginActivity";
    private static final String PREFS = "SilmedyPrefs";

    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ─── 0) 자동 로그인 체크 ───────────────────────────────────
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String token = prefs.getString("access_token", null);
        if (token != null) {
            // 이미 로그인됨 → 딜레이 없이 Home 또는 Receive로 이동
            String afterRoom = getIntent().getStringExtra("after_login_roomId");
            if (afterRoom != null) {
                startActivity(new Intent(this, ReceiveActivity.class)
                        .putExtra("roomId", afterRoom));
            } else {
                startActivity(new Intent(this, HomeActivity.class));
            }
            finish();
            return;
        }

        // ─── 1) 뷰 바인딩 ─────────────────────────────────────────
        setContentView(R.layout.activity_login);
        inputEmail    = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin      = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = inputEmail.getText().toString().trim();
        String pw    = inputPassword.getText().toString().trim();
        if (email.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ─── 2) 로그인 API 호출 ───────────────────────────────────
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", pw);
        } catch (JSONException e) {
            Log.e(TAG, "JSON 생성 실패", e);
            return;
        }

        Request req = new Request.Builder()
                .url("http://3.36.62.211:5000/api/v1/patients/login")
                .post(RequestBody.create(
                        body.toString(),
                        MediaType.get("application/json; charset=utf-8")
                ))
                .build();

        new OkHttpClient().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show()
                );
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this,
                                    "로그인 실패: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                try {
                    JSONObject j = new JSONObject(res);
                    String access   = j.getString("access_token");
                    String refresh  = j.getString("refresh_token");
                    String name     = j.getString("name");
                    String fcmToken = j.optString("fcm_token", "");

                    // ─── 3) SharedPreferences에 저장 ────────────────────
                    prefs.edit()
                            .putString("access_token", access)
                            .putString("refresh_token", refresh)
                            .putString("patient_name", name)
                            .putString("fcm_token", fcmToken)
                            .apply();

                    // ─── 4) fcm_token 신규 발급 시 서버 등록 ─────────────
                    if (fcmToken.isEmpty()) {
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        postFcmToken(email, access, task.getResult());
                                    }
                                });
                    }

                    // ─── 5) 로그인 후 이동 결정 ─────────────────────────
                    runOnUiThread(() -> {
                        String afterRoom = getIntent().getStringExtra("after_login_roomId");
                        if (afterRoom != null) {
                            startActivity(new Intent(LoginActivity.this, ReceiveActivity.class)
                                    .putExtra("roomId", afterRoom));
                        } else {
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        }
                        finish();
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "응답 파싱 실패", e);
                }
            }
        });
    }

    /** fcm_token을 서버에 POST */
    private void postFcmToken(String email, String jwt, String fcmToken) {
        JSONObject b = new JSONObject();
        try { b.put("fcm_token", fcmToken); } catch (JSONException ignored) {}
        Request r = new Request.Builder()
                .url("http://3.36.62.211:5000/api/v1/patients/fcm-token")
                .addHeader("Authorization", "Bearer " + jwt)
                .post(RequestBody.create(
                        b.toString(),
                        MediaType.get("application/json; charset=utf-8")
                ))
                .build();
        new OkHttpClient().newCall(r).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "FCM 등록 실패", e);
            }
            @Override public void onResponse(Call call, Response response) {
                Log.i(TAG, "FCM 등록 결과: " + response.code());
            }
        });
    }
}