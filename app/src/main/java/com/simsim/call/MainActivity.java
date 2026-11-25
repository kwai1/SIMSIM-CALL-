package com.simsim.call;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.view.Gravity;
import android.view.View;
import android.text.InputType;
import android.widget.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(5, 9, 18);
    private final int PANEL = Color.rgb(13, 22, 38);
    private final int BLUE = Color.rgb(0, 190, 255);
    private final int WHITE = Color.WHITE;
    private final int GRAY = Color.rgb(145, 165, 185);
    private LinearLayout root;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private final String ADMIN_KEY = "SIMSIM-ADMIN-2026";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() != null) showDashboard();
        else showLogin();
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(16)); d.setStroke(dp(1), stroke); return d;
    }
    private void setup() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL); root.setPadding(dp(24),dp(30),dp(24),dp(30));
        root.setBackgroundColor(BG); scroll.addView(root); setContentView(scroll);
    }
    private void text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER);
        if(bold)t.setTypeface(null,Typeface.BOLD);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(7),0,dp(7)); root.addView(t,p);
    }
    private Button button(String value) {
        Button b=new Button(this); b.setText(value); b.setTextColor(WHITE); b.setTextSize(15); b.setAllCaps(false); b.setBackground(shape(PANEL,BLUE));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58)); p.setMargins(0,dp(9),0,dp(9)); root.addView(b,p); return b;
    }
    private EditText input(String hint, int type) {
        EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(GRAY); e.setTextColor(WHITE); e.setTextSize(16); e.setSingleLine(true);
        e.setInputType(type); e.setPadding(dp(15),0,dp(15),0); e.setBackground(shape(PANEL,BLUE));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58)); p.setMargins(0,dp(8),0,dp(8)); root.addView(e,p); return e;
    }
    private void header() {
        text("◈",55,BLUE,true); text("SIMSIM-CALL",32,WHITE,true);
        text("PRIVATE COMMUNICATION NETWORK",11,BLUE,true);
        text("SYSTEM STATUS  •  PROTECTED ACCESS",11,GRAY,false);
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    private void showLogin() {
        setup(); header(); text("SECURITY GATE",20,WHITE,true);
        text("تسجيل الدخول",17,BLUE,true);
        final EditText email=input("البريد الإلكتروني",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText password=input("كلمة المرور",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button login=button("دخول إلى سمسم");
        login.setOnClickListener(v -> {
            String e=email.getText().toString().trim(), p=password.getText().toString();
            if(e.isEmpty()||p.isEmpty()){toast("أدخل البريد الإلكتروني وكلمة المرور");return;}
            login.setEnabled(false);
            auth.signInWithEmailAndPassword(e,p).addOnCompleteListener(task -> {
                login.setEnabled(true);
                if(task.isSuccessful()) showDashboard();
                else toast("تعذر تسجيل الدخول: تحقق من البيانات أو اتصال الإنترنت");
            });
        });
        Button register=button("إنشاء حساب باستخدام رمز دعوة");
        register.setOnClickListener(v -> showRegister());
        Button admin=button("دخول المدير");
        admin.setOnClickListener(v -> showAdminGate());
        text("AUTHORIZED USERS ONLY",11,BLUE,true);
    }

    private void showRegister() {
        setup(); header(); text("إنشاء حساب جديد",20,WHITE,true);
        final EditText code=input("رمز الدعوة",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        final EditText email=input("البريد الإلكتروني",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText password=input("كلمة المرور (6 أحرف على الأقل)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button create=button("التحقق وإنشاء الحساب");
        create.setOnClickListener(v -> {
            String c=code.getText().toString().trim().toUpperCase();
            String e=email.getText().toString().trim();
            String p=password.getText().toString();
            if(c.isEmpty()||e.isEmpty()||p.length()<6){toast("أكمل البيانات وتأكد أن كلمة المرور 6 أحرف على الأقل");return;}
            create.setEnabled(false);
            auth.createUserWithEmailAndPassword(e,p).addOnCompleteListener(task -> {
                if(!task.isSuccessful()){create.setEnabled(true);toast("لم يتم إنشاء الحساب. تحقق من البريد أو كلمة المرور");return;}
                FirebaseUser user=auth.getCurrentUser();
                if(user==null){create.setEnabled(true);toast("تعذر تحديد الحساب");return;}
                consumeInvite(c,user,create);
            });
        });
        Button back=button("رجوع"); back.setOnClickListener(v -> showLogin());
    }

    private void consumeInvite(String code,FirebaseUser user,Button button) {
        DocumentReference invite=db.collection("invitations").document(code);
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snap=transaction.get(invite);
            if(!snap.exists()) throw new FirebaseFirestoreException("رمز غير موجود",FirebaseFirestoreException.Code.NOT_FOUND);
            String status=snap.getString("status");
            if(!"نشط".equals(status)&&!"active".equalsIgnoreCase(status))
                throw new FirebaseFirestoreException("رمز مستخدم أو غير نشط",FirebaseFirestoreException.Code.PERMISSION_DENIED);
            transaction.update(invite,"status","مستخدم","usedBy",user.getUid(),"usedAt",FieldValue.serverTimestamp());
            return null;
        }).addOnCompleteListener(task -> {
            if(!task.isSuccessful()){
                user.delete();
                button.setEnabled(true);
                toast("رمز الدعوة غير صالح أو سبق استخدامه");
                return;
            }
            Map<String,Object> profile=new HashMap<>();
            profile.put("email",user.getEmail());
            profile.put("createdAt",FieldValue.serverTimestamp());
            profile.put("status","active");
            db.collection("users").document(user.getUid()).set(profile)
                .addOnSuccessListener(x -> showDashboard())
                .addOnFailureListener(err -> toast("تم إنشاء الحساب لكن تعذر حفظ الملف الشخصي"));
        });
    }

    private void showAdminGate() {
        setup(); header(); text("ADMIN CONTROL",22,WHITE,true);
        final EditText key=input("رمز المدير",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button enter=button("متابعة");
        enter.setOnClickListener(v -> {
            if(ADMIN_KEY.equals(key.getText().toString())) showAdmin();
            else toast("رمز المدير غير صحيح");
        });
        Button back=button("رجوع"); back.setOnClickListener(v -> showLogin());
    }

    private void showAdmin() {
        setup(); header(); text("لوحة تحكم المدير",22,WHITE,true);
        Button generate=button("＋ إنشاء رمز دعوة جديد");
        generate.setOnClickListener(v -> {
            String code=createCode();
            Map<String,Object> data=new HashMap<>();
            data.put("status","نشط"); data.put("createdAt",FieldValue.serverTimestamp());
            db.collection("invitations").document(code).set(data)
                .addOnSuccessListener(x -> showGeneratedCode(code))
                .addOnFailureListener(err -> toast("تعذر إنشاء الدعوة. تحقق من قواعد Firestore"));
        });
        Button logout=button("تسجيل الخروج"); logout.setOnClickListener(v -> showLogin());
    }

    private String createCode() {
        String chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; SecureRandom random=new SecureRandom();
        StringBuilder b=new StringBuilder("SIM-"); for(int i=0;i<8;i++) b.append(chars.charAt(random.nextInt(chars.length())));
        return b.toString();
    }
    private void showGeneratedCode(String code) {
        setup(); header(); text("تم إنشاء رمز الدعوة",19,WHITE,true);
        TextView result=new TextView(this); result.setText(code); result.setTextSize(23); result.setTextColor(BLUE);
        result.setGravity(Gravity.CENTER); result.setTypeface(null,Typeface.BOLD); result.setPadding(dp(10),dp(20),dp(10),dp(20));
        result.setBackground(shape(PANEL,BLUE)); root.addView(result,new LinearLayout.LayoutParams(-1,dp(75)));
        Button copy=button("نسخ رمز الدعوة");
        copy.setOnClickListener(v -> {
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("SIMSIM CODE",code)); toast("تم نسخ الرمز");
        });
        Button back=button("العودة إلى لوحة المدير"); back.setOnClickListener(v -> showAdmin());
    }

    private void showDashboard() {
        setup(); header(); text("ACCESS GRANTED",20,BLUE,true);
        text("مرحباً بك في الشبكة الخاصة",19,WHITE,true);
        FirebaseUser u=auth.getCurrentUser();
        if(u!=null) text(u.getEmail()==null?"مستخدم سمسم":u.getEmail(),13,GRAY,false);
        Button calls=button("◉  الاتصالات");
        calls.setOnClickListener(v -> toast("واجهة المكالمات قيد الربط"));
        Button messages=button("▤  المحادثات");
        messages.setOnClickListener(v -> toast("واجهة المحادثات قيد الربط"));
        Button logout=button("تسجيل الخروج");
        logout.setOnClickListener(v -> auth.signOut());
        logout.setOnClickListener(v -> { auth.signOut(); showLogin(); });
    }
}
