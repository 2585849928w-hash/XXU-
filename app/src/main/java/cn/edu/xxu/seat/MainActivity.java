package cn.edu.xxu.seat;

import android.app.Activity;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/** A small native Android client. It does not contain credentials or a preconfigured seat. */
public final class MainActivity extends Activity {
  private EditText user, pass, floor, seats, time, day;
  private TextView log;
  private Button run;
  private PowerManager.WakeLock wakeLock;
  private static final String BASE = "http://211.84.163.219:85";
  private static final String CAS = "https://cas.xxu.edu.cn/cas";
  private static final String PREFS = "settings", PASSWORD_KEY = "xxu_seat_password";

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(36, 36, 36, 36);
    ScrollView scroll = new ScrollView(this); scroll.addView(box); setContentView(scroll);
    TextView title = new TextView(this); title.setText("XXU 图书馆抢座"); title.setTextSize(24); box.addView(title);
    TextView note = new TextView(this); note.setText("仅供本人按学校规则使用。定时模式请保持 App 打开、联网且关闭电池优化。密码会通过本机 Android Keystore 加密保存。\n"); box.addView(note);
    user = new EditText(this); pass = new EditText(this); pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    floor = field(box, "楼层（例如：七楼）"); seats = field(box, "座位号（优先顺序，用英文逗号分隔）");
    day = field(box, "预约日期 YYYY-M-D（留空＝明天）"); time = field(box, "开抢时间 HH:MM:SS（留空＝立即）");
    Button account = new Button(this); account.setText("切换账号"); box.addView(account);
    Button map = new Button(this); map.setText("查看七楼北座位图"); box.addView(map);
    run = new Button(this); run.setText("开始"); box.addView(run);
    log = new TextView(this); log.setTextIsSelectable(true); log.setTextSize(13); box.addView(log);
    load(); account.setOnClickListener(v -> startActivity(new android.content.Intent(this, LoginActivity.class))); map.setOnClickListener(v -> startActivity(new android.content.Intent(this, SeatMapActivity.class))); run.setOnClickListener(v -> begin());
  }
  private EditText field(LinearLayout b, String hint) { EditText x = new EditText(this); x.setHint(hint); x.setSingleLine(true); b.addView(x); return x; }
  private void load() { android.content.SharedPreferences p=getSharedPreferences(PREFS,0); if(user.getText().length()==0)user.setText(p.getString("u", "")); if(pass.getText().length()==0)pass.setText(savedPassword(this)); floor.setText(p.getString("f", "七楼")); seats.setText(p.getString("s", "")); time.setText(p.getString("t", "")); }
  private void save() { if(!saveLogin(this,user.getText().toString().trim(),pass.getText().toString()))Toast.makeText(this,"无法加密保存密码",Toast.LENGTH_LONG).show(); getSharedPreferences(PREFS,0).edit().putString("f",floor.getText().toString().trim()).putString("s",seats.getText().toString().trim()).putString("t",time.getText().toString().trim()).apply(); }
  static String savedUser(android.content.Context c){return c.getSharedPreferences(PREFS,0).getString("u","");}
  static String savedPassword(android.content.Context c){try{String value=c.getSharedPreferences(PREFS,0).getString("p","");return value.isEmpty()?"":decryptPassword(value);}catch(Exception e){return "";}}
  static boolean saveLogin(android.content.Context c,String username,String password){try{c.getSharedPreferences(PREFS,0).edit().putString("u",username).putString("p",encryptPassword(password)).apply();return true;}catch(Exception first){try{passwordKey(true);c.getSharedPreferences(PREFS,0).edit().putString("u",username).putString("p",encryptPassword(password)).apply();return true;}catch(Exception ignored){return false;}}}
  private static SecretKey passwordKey(boolean renew) throws Exception { KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(!renew){Key existing=store.getKey(PASSWORD_KEY,null);if(existing instanceof SecretKey)return (SecretKey)existing;}if(store.containsAlias(PASSWORD_KEY))store.deleteEntry(PASSWORD_KEY);javax.crypto.KeyGenerator generator=javax.crypto.KeyGenerator.getInstance(android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new android.security.keystore.KeyGenParameterSpec.Builder(PASSWORD_KEY,android.security.keystore.KeyProperties.PURPOSE_ENCRYPT|android.security.keystore.KeyProperties.PURPOSE_DECRYPT).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).build());return generator.generateKey(); }
  private static String encryptPassword(String plain) throws Exception { byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,passwordKey(false),new javax.crypto.spec.GCMParameterSpec(128,iv));return android.util.Base64.encodeToString(iv,android.util.Base64.NO_WRAP)+":"+android.util.Base64.encodeToString(cipher.doFinal(plain.getBytes("UTF-8")),android.util.Base64.NO_WRAP); }
  private static String decryptPassword(String value) throws Exception { String[] parts=value.split(":",2);if(parts.length!=2)throw new Exception("invalid encrypted password");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,passwordKey(false),new javax.crypto.spec.GCMParameterSpec(128,android.util.Base64.decode(parts[0],android.util.Base64.NO_WRAP)));return new String(cipher.doFinal(android.util.Base64.decode(parts[1],android.util.Base64.NO_WRAP)),"UTF-8"); }
  private void say(String s) { runOnUiThread(() -> { log.append(s+"\n"); ((ScrollView)log.getParent().getParent()).fullScroll(View.FOCUS_DOWN); }); }
  private void begin() {
    String u=user.getText().toString().trim(), p=pass.getText().toString(), f=floor.getText().toString().trim(), ss=seats.getText().toString().trim();
    if(u.isEmpty()||p.isEmpty()||f.isEmpty()||ss.isEmpty()){ Toast.makeText(this,"请填写学号、密码、楼层和座位号",Toast.LENGTH_LONG).show(); return; }
    save(); log.setText(""); run.setEnabled(false);
    new Thread(() -> { try { runGrab(u,p,f,ss); } catch(Exception e) { say("失败："+e.getMessage()); } finally { runOnUiThread(()->run.setEnabled(true)); releaseWakeLock(); } }).start();
  }
  private void holdWakeLock() { PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE); wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"xxu:grab"); wakeLock.acquire(10*60*1000L); }
  private void releaseWakeLock() { if(wakeLock!=null&&wakeLock.isHeld()) wakeLock.release(); }
  private String bookingDay() throws Exception { String v=day.getText().toString().trim(); if(!v.isEmpty()) return v; Calendar c=Calendar.getInstance();c.add(Calendar.DATE,1);return c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH)+1)+"-"+c.get(Calendar.DATE); }
  private Calendar targetTime() throws Exception { String v=time.getText().toString().trim(); if(v.isEmpty())return null; String[] a=v.split(":"); if(a.length<2||a.length>3) throw new Exception("时间格式应为 HH:MM 或 HH:MM:SS"); Calendar target=Calendar.getInstance();target.set(Calendar.HOUR_OF_DAY,Integer.parseInt(a[0]));target.set(Calendar.MINUTE,Integer.parseInt(a[1]));target.set(Calendar.SECOND,a.length==3?Integer.parseInt(a[2]):0);target.set(Calendar.MILLISECOND,0);if(target.before(Calendar.getInstance()))target.add(Calendar.DATE,1);return target; }
  private void waitUntil(Calendar target, String label) throws Exception { while(target.getTimeInMillis()>System.currentTimeMillis()) Thread.sleep(Math.min(1000,target.getTimeInMillis()-System.currentTimeMillis())); say(label); }
  private void runGrab(String u,String p,String floorName,String wanted) throws Exception {
    String day=bookingDay(); Calendar target=targetTime(); if(target!=null){Calendar loginAt=(Calendar)target.clone();loginAt.add(Calendar.MINUTE,-2);holdWakeLock();if(loginAt.getTimeInMillis()>System.currentTimeMillis()){say("将在 "+new SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(loginAt.getTime())+" 登录，并在 "+new SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(target.getTime())+" 提交；请保持 App 打开。");waitUntil(loginAt,"开始提前登录…");}} say("正在登录…"); Client c=new Client(); c.login(u,p); say("登录成功，正在读取座位…");
    JSONArray floors=c.getArray(BASE+"/api.php/v3areas/1", "data.list.childArea"); JSONObject fl=null; for(int i=0;i<floors.length();i++){JSONObject x=floors.getJSONObject(i);if(floorName.equals(x.optString("name"))){fl=x;break;}} if(fl==null)throw new Exception("未找到楼层："+floorName);
    int floorId=fl.getInt("id"); String segment=c.segment(floorId,day); JSONArray areas=c.getArray(BASE+"/api.php/v3areas/"+floorId,"data.list.childArea"); if(areas.length()==0){areas=new JSONArray();areas.put(fl);}
    String[] choices=wanted.split(","); ArrayList<Target> targets=new ArrayList<>();
    for(int ai=0;ai<areas.length();ai++){JSONObject ar=areas.getJSONObject(ai);int areaId=ar.getInt("id");JSONArray list=c.getArray(BASE+"/api.php/spaces_old?area="+areaId+"&segment="+URLEncoder.encode(segment,"UTF-8")+"&day="+URLEncoder.encode(day,"UTF-8")+"&startTime=08%3A00&endTime=21%3A45","data.list");for(String choose:choices)for(int i=0;i<list.length();i++){JSONObject s=list.getJSONObject(i);String no=s.optString("no",s.optString("name"));if(choose.trim().equals(no)&&!contains(targets,s.getInt("id")))targets.add(new Target(s.getInt("id"),areaId,no));}}
    if(targets.isEmpty())throw new Exception("指定座位均未找到或不可预约"); if(target!=null&&target.getTimeInMillis()>System.currentTimeMillis())waitUntil(target,"开抢时间已到，开始提交…"); say("开始尝试："+targets.size()+" 个座位"); long end=System.currentTimeMillis()+180000;
    while(System.currentTimeMillis()<end) for(Target t:targets){JSONObject result=c.book(t,segment);if(result.optInt("status")==1){say("成功："+t.no+"，"+result.optString("msg"));return;}say("未成功："+t.no+" - "+result.optString("msg"));Thread.sleep(500);}
    say("3 分钟内未预约成功。");
  }
  private boolean contains(ArrayList<Target> a,int id){for(Target t:a)if(t.id==id)return true;return false;}
  static final class Target{int id,area;String no;Target(int i,int a,String n){id=i;area=a;no=n;}}

  static final class Client {
    CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL); String userid,token;
    Client(){CookieHandler.setDefault(cookies);}
    static final class Reply { final String url, body, location; Reply(String u,String b,String l){url=u;body=b;location=l;} }
    Reply request(String url,String method,String body,boolean follow) throws Exception { HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();h.setInstanceFollowRedirects(follow);h.setConnectTimeout(15000);h.setReadTimeout(15000);h.setRequestProperty("User-Agent","Mozilla/5.0 (Android) AppleWebKit/537.36 Chrome/120");h.setRequestProperty("X-Requested-With","XMLHttpRequest");h.setRequestProperty("Referer",url.startsWith(BASE)?BASE+"/home/web/seat/area/1":CAS+"/login");h.setRequestMethod(method);if(body!=null){h.setDoOutput(true);h.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");try(OutputStream o=h.getOutputStream()){o.write(body.getBytes("UTF-8"));}}InputStream in=h.getResponseCode()>=400?h.getErrorStream():h.getInputStream();StringBuilder b=new StringBuilder();if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"))){String l;while((l=r.readLine())!=null)b.append(l);}return new Reply(h.getURL().toString(),b.toString(),h.getHeaderField("Location")); }
    String text(String url,String method,String body) throws Exception { return request(url,method,body,true).body; }
    void login(String u,String p) throws Exception {
      String service=URLEncoder.encode(BASE+"/cas/index.php?","UTF-8"), login=CAS+"/login?service="+service;
      Reply page=request(login,"GET",null,false); String execution=match(page.body,"name=[\"']execution[\"']\\s+value=[\"']([^\"']+)");if(execution==null)execution=match(page.body,"value=[\"']([^\"']+)[\"']\\s+name=[\"']execution");if(execution==null)throw new Exception("无法取得学校登录令牌");
      String key=text(CAS+"/jwt/publicKey","GET",null).trim();String body="username="+enc(u)+"&password="+enc(rsa(p,key))+"&captcha=&mfaState=&currentMenu=1&failN=0&execution="+enc(execution)+"&_eventId=submit&geolocation=&fpVisitorId=&submit=Login1";
      Reply step=request(login,"POST",body,false); boolean reached=false;
      for(int i=0;i<20;i++){ if(step.location==null)break; String next=new URL(new URL(step.url),step.location).toString(); if(next.contains("/Api/auto_user_check")){request(next,"GET",null,true);reached=true;break;} step=request(next,"GET",null,false); }
      if(!reached)throw new Exception("未完成座位系统授权跳转");
      for(HttpCookie c:cookies.getCookieStore().getCookies()){if("userid".equals(c.getName()))userid=c.getValue();if("access_token".equals(c.getName()))token=c.getValue();}if(userid==null||token==null)throw new Exception("座位系统登录未完成");
    }
    JSONArray getArray(String url,String path)throws Exception{JSONObject root=new JSONObject(text(url,"GET",null));if(root.optInt("status")!=1)throw new Exception(root.optString("msg","接口错误"));Object x=root;for(String k:path.split("\\."))x=((JSONObject)x).get(k);return (JSONArray)x;}
    String segment(int area,String day)throws Exception{JSONObject r=new JSONObject(text(BASE+"/api.php/space_time_buckets?area="+area+"&day="+enc(day),"GET",null));String s=findSegment(r);if(s==null)throw new Exception("未找到 08:00-21:45 时段");return s;}
    String findSegment(Object o)throws Exception{if(o instanceof JSONObject){JSONObject x=(JSONObject)o;if("08:00".equals(x.optString("startTime").substring(0,Math.min(5,x.optString("startTime").length())))&&"21:45".equals(x.optString("endTime").substring(0,Math.min(5,x.optString("endTime").length())))){String v=x.optString("segment",x.optString("segmentId",x.optString("id")));if(!v.isEmpty())return v;}Iterator<String> it=x.keys();while(it.hasNext()){String v=findSegment(x.get(it.next()));if(v!=null)return v;}}else if(o instanceof JSONArray){JSONArray a=(JSONArray)o;for(int i=0;i<a.length();i++){String v=findSegment(a.get(i));if(v!=null)return v;}}return null;}
    JSONObject book(Target t,String segment)throws Exception{String body="access_token="+enc(token)+"&userid="+enc(userid)+"&segment="+enc(segment)+"&type=1&operateChannel=2";return new JSONObject(text(BASE+"/api.php/spaces/"+t.id+"/book","POST",body));}
    static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    static String match(String s,String p){java.util.regex.Matcher m=java.util.regex.Pattern.compile(p).matcher(s);return m.find()?m.group(1):null;}
    static String rsa(String p,String pem)throws Exception{pem=pem.replaceAll("-----BEGIN PUBLIC KEY-----|-----END PUBLIC KEY-----|\\s","");PublicKey k=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(android.util.Base64.decode(pem,android.util.Base64.DEFAULT)));Cipher c=Cipher.getInstance("RSA/ECB/PKCS1Padding");c.init(Cipher.ENCRYPT_MODE,k);return "__RSA__"+android.util.Base64.encodeToString(c.doFinal(p.getBytes("UTF-8")),android.util.Base64.NO_WRAP);}
  }
}
