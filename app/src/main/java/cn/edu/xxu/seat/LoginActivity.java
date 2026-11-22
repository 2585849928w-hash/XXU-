package cn.edu.xxu.seat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** First screen: verifies CAS credentials, then stores them encrypted on this device. */
public final class LoginActivity extends Activity {
  private EditText username, password;
  private Button login;
  private TextView hint;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    ScrollView scroll=new ScrollView(this); LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(52,80,52,48);root.setBackgroundColor(Color.rgb(245,248,246));scroll.addView(root);setContentView(scroll);
    ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.wlt_icon);root.addView(icon,new LinearLayout.LayoutParams(176,176));
    TextView title=new TextView(this);title.setText("XXU 图书馆");title.setTextSize(28);title.setTextColor(Color.rgb(7,91,69));title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,-2));
    TextView sub=new TextView(this);sub.setText("登录后开始预约座位");sub.setTextSize(16);sub.setGravity(Gravity.CENTER);root.addView(sub,new LinearLayout.LayoutParams(-1,-2));
    Space gap=new Space(this);root.addView(gap,new LinearLayout.LayoutParams(1,44));
    username=field(root,"学号");password=field(root,"统一认证密码");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
    username.setText(MainActivity.savedUser(this));password.setText(MainActivity.savedPassword(this));
    login=new Button(this);login.setText("登录并进入");root.addView(login,new LinearLayout.LayoutParams(-1,-2));
    hint=new TextView(this);hint.setGravity(Gravity.CENTER);hint.setPadding(0,24,0,0);root.addView(hint,new LinearLayout.LayoutParams(-1,-2));
    TextView privacy=new TextView(this);privacy.setText("账号和密码仅保存于本机，并使用 Android Keystore 加密。");privacy.setTextSize(12);privacy.setGravity(Gravity.CENTER);privacy.setPadding(0,48,0,0);root.addView(privacy,new LinearLayout.LayoutParams(-1,-2));
    login.setOnClickListener(v -> signIn());
  }
  private EditText field(LinearLayout root,String title){EditText e=new EditText(this);e.setHint(title);e.setSingleLine(true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=20;root.addView(e,p);return e;}
  private void signIn(){String u=username.getText().toString().trim(),p=password.getText().toString();if(u.isEmpty()||p.isEmpty()){hint.setText("请输入学号和密码");return;}login.setEnabled(false);hint.setText("正在验证登录…");new Thread(()->{try{new MainActivity.Client().login(u,p);if(!MainActivity.saveLogin(this,u,p))throw new Exception("无法加密保存密码");runOnUiThread(()->{startActivity(new Intent(this,MainActivity.class));finish();});}catch(Exception e){runOnUiThread(()->{hint.setText("登录失败："+e.getMessage());login.setEnabled(true);});}}).start();}
}
