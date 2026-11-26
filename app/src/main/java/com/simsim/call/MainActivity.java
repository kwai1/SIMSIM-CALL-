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
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import android.view.inputmethod.InputMethodManager;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(5, 3, 18);
    private final int PANEL = Color.rgb(18, 10, 38);
    private final int BLUE = Color.rgb(153, 48, 255);
    private final int WHITE = Color.WHITE;
    private final int GRAY = Color.rgb(145, 165, 185);
    private LinearLayout root;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private final String ADMIN_KEY = "SIMSIM-ADMIN-2026";
    private final Handler presenceHandler = new Handler(Looper.getMainLooper());
    private ListenerRegistration contactsListener;
    private LinearLayout contactsList;
    private EditText contactsSearch;
    private com.google.firebase.firestore.QuerySnapshot lastContacts;
    private String myDisplayName = "عضو سمسم";
    private boolean profileReady = false;
    private ListenerRegistration messagesListener;
    private LinearLayout messagesContainer;
    private String activeChatId;
    private final Runnable presenceBeat = new Runnable() {
        @Override public void run() {
            updatePresence(true);
            presenceHandler.postDelayed(this, 30000);
        }
    };

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
    private GradientDrawable purpleGradient() {
        GradientDrawable d = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.rgb(184,55,255), Color.rgb(76,24,220), Color.rgb(120,35,255)});
        d.setCornerRadius(dp(28));
        d.setStroke(dp(1), Color.rgb(205,145,255));
        return d;
    }

    private void header() {
        TextView mark = new TextView(this);
        mark.setText("☎");
        mark.setTextSize(54);
        mark.setTextColor(WHITE);
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(null, Typeface.BOLD);
        GradientDrawable emblem = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(194,72,255), Color.rgb(75,25,220)});
        emblem.setShape(GradientDrawable.OVAL);
        emblem.setStroke(dp(3), Color.rgb(225,190,255));
        mark.setBackground(emblem);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(116),dp(116));
        mp.gravity = Gravity.CENTER;
        mp.setMargins(0,dp(12),0,dp(12));
        root.addView(mark,mp);
        text("SIMSIM",34,WHITE,true);
        text("CALL",34,BLUE,true);
        text("اتصال آمن .. خصوصية كاملة",18,WHITE,true);
        text("شبكة تواصل خاصة للأشخاص المصرّح لهم",13,GRAY,false);
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    private String authErrorMessage(Exception error) {
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code) || "ERROR_EMAIL_ALREADY_EXISTS".equals(code))
                return "هذا البريد مسجل مسبقاً. ارجع وسجّل الدخول بدلاً من إنشاء حساب جديد.";
            if ("ERROR_INVALID_EMAIL".equals(code)) return "صيغة البريد الإلكتروني غير صحيحة.";
            if ("ERROR_WEAK_PASSWORD".equals(code) || "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS".equals(code))
                return "كلمة المرور ضعيفة. استخدم 6 أحرف على الأقل.";
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(code))
                return "تسجيل البريد وكلمة المرور غير مفعّل في Firebase Authentication.";
            if ("ERROR_TOO_MANY_REQUESTS".equals(code))
                return "محاولات كثيرة. انتظر قليلاً ثم أعد المحاولة.";
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code))
                return "تعذر الاتصال. تحقق من الإنترنت وحاول مجدداً.";
            if ("ERROR_USER_NOT_FOUND".equals(code) || "ERROR_INVALID_CREDENTIAL".equals(code))
                return "البريد أو كلمة المرور غير صحيحة.";
            if ("ERROR_WRONG_PASSWORD".equals(code)) return "كلمة المرور غير صحيحة.";
            return "خطأ Firebase: " + code;
        }
        String message = error == null ? "" : error.getMessage();
        return "تعذر إكمال العملية" + (message == null || message.isEmpty() ? "." : ": " + message);
    }

    private void showLogin() {
        setup();
        header();
        text("بوابة الدخول الخاصة",18,WHITE,true);
        text("أهلاً بك في SIMSIM CALL",15,BLUE,true);
        final EditText email=input("البريد الإلكتروني",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText password=input("كلمة المرور",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button login=button("➜   تسجيل الدخول");
        login.setBackground(purpleGradient());
        login.setOnClickListener(v -> {
            String e=email.getText().toString().trim(), p=password.getText().toString();
            if(e.isEmpty()||p.isEmpty()){toast("أدخل البريد الإلكتروني وكلمة المرور");return;}
            login.setEnabled(false);
            auth.signInWithEmailAndPassword(e,p).addOnCompleteListener(task -> {
                login.setEnabled(true);
                if(task.isSuccessful()) showDashboard();
                else toast(authErrorMessage(task.getException()));
            });
        });
        Button register=button("إنشاء حساب باستخدام رمز دعوة");
        register.setOnClickListener(v -> showRegister());
        Button admin=button("⚙   دخول المدير");
        admin.setOnClickListener(v -> showAdminGate());

        LinearLayout features = new LinearLayout(this);
        features.setOrientation(LinearLayout.HORIZONTAL);
        features.setGravity(Gravity.CENTER);
        String[][] items = {{"☎","مكالمات"},{"◆","خصوصية"},{"▣","محادثات"}};
        for(String[] item:items) {
            LinearLayout cell=new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            TextView symbol=new TextView(this);
            symbol.setText(item[0]); symbol.setTextSize(25); symbol.setTextColor(BLUE);
            symbol.setGravity(Gravity.CENTER);
            TextView label=new TextView(this);
            label.setText(item[1]); label.setTextSize(12); label.setTextColor(WHITE);
            label.setGravity(Gravity.CENTER);
            cell.addView(symbol); cell.addView(label);
            features.addView(cell,new LinearLayout.LayoutParams(0,-2,1));
        }
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);
        fp.setMargins(0,dp(22),0,dp(14));
        root.addView(features,fp);
        text("تيم آصف للبرمجة",15,BLUE,true);
        text("SIMSIM CALL  •  الإصدار 1.0",11,GRAY,false);
        text("AUTHORIZED USERS ONLY",10,GRAY,true);
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
                if(!task.isSuccessful()){create.setEnabled(true);toast(authErrorMessage(task.getException()));return;}
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
        final EditText email=input("بريد المدير الإلكتروني",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText password=input("كلمة مرور المدير",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button enter=button("دخول المدير");
        enter.setOnClickListener(v -> {
            String e=email.getText().toString().trim();
            String p=password.getText().toString();
            if(e.isEmpty() || p.isEmpty()){ toast("أدخل البريد وكلمة المرور"); return; }
            enter.setEnabled(false);
            auth.signInWithEmailAndPassword(e,p).addOnCompleteListener(task -> {
                if(!task.isSuccessful()){
                    enter.setEnabled(true);
                    toast("تعذر تسجيل دخول المدير");
                    return;
                }
                FirebaseUser user=auth.getCurrentUser();
                if(user==null){ enter.setEnabled(true); auth.signOut(); toast("تعذر التحقق من المدير"); return; }
                db.collection("admins").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if(doc.exists() && "admin".equals(doc.getString("role"))) showAdmin();
                        else { auth.signOut(); enter.setEnabled(true); toast("هذا الحساب ليس لديه صلاحية المدير"); }
                    })
                    .addOnFailureListener(err -> {
                        auth.signOut(); enter.setEnabled(true);
                        toast("تعذر التحقق من صلاحية المدير في Firestore");
                    });
            });
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
        text("مرحباً بك في شبكة سمسم",19,WHITE,true);
        FirebaseUser u=auth.getCurrentUser();
        if(u==null){ showLogin(); return; }
        if(u.getEmail()!=null) text(u.getEmail(),13,GRAY,false);
        text("اسمك الظاهر للأعضاء",15,BLUE,true);
        final EditText nameField=input("اكتب الاسم الذي سيظهر للآخرين",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        db.collection("publicProfiles").document(u.getUid()).get()
            .addOnSuccessListener(doc -> {
                String saved=doc.getString("displayName");
                if(saved!=null && !saved.trim().isEmpty()) {
                    myDisplayName=saved;
                    nameField.setText(saved);
                    profileReady=true;
                    startPresence();
                } else {
                    String fallback="عضو "+u.getUid().substring(0,Math.min(5,u.getUid().length()));
                    nameField.setText(fallback);
                    Map<String,Object> initial=new HashMap<>();
                    initial.put("uid",u.getUid());
                    initial.put("displayName",fallback);
                    initial.put("online",true);
                    initial.put("lastActive",FieldValue.serverTimestamp());
                    initial.put("createdAt",FieldValue.serverTimestamp());
                    db.collection("publicProfiles").document(u.getUid()).set(initial,SetOptions.merge())
                        .addOnSuccessListener(x -> { myDisplayName=fallback; profileReady=true; startPresence(); })
                        .addOnFailureListener(e -> toast("تعذر إنشاء ملفك الظاهر. انشر قواعد Firestore الجديدة"));
                }
            });
        Button saveName=button("حفظ الاسم");
        saveName.setOnClickListener(v -> {
            String value=nameField.getText().toString().trim();
            if(value.length()<2 || value.length()>30){toast("الاسم يجب أن يكون بين حرفين و30 حرفاً");return;}
            saveName.setEnabled(false);
            Map<String,Object> profile=new HashMap<>();
            profile.put("uid",u.getUid());
            profile.put("displayName",value);
            profile.put("online",true);
            profile.put("lastActive",FieldValue.serverTimestamp());
            profile.put("createdAt",FieldValue.serverTimestamp());
            db.collection("publicProfiles").document(u.getUid()).set(profile,SetOptions.merge())
                 .addOnSuccessListener(x -> {myDisplayName=value;profileReady=true;saveName.setEnabled(true);toast("تم حفظ اسمك");startPresence();})
                .addOnFailureListener(err -> {saveName.setEnabled(true);toast("تعذر حفظ الاسم. انشر قواعد publicProfiles أولاً");});
        });
        Button contacts=button("♙  الأعضاء وجهات الاتصال");
        contacts.setOnClickListener(v -> showContacts());
        Button calls=button("◉  الاتصالات");
        calls.setOnClickListener(v -> toast("قسم الاتصال الصوتي ما زال يحتاج ربط LiveKit"));
        Button messages=button("▤  المحادثات");
        messages.setOnClickListener(v -> showContacts());
        Button logout=button("تسجيل الخروج");
        logout.setOnClickListener(v -> { updatePresence(false); profileReady=false; auth.signOut(); showLogin(); });
        // Profile creation and presence start after the profile document is confirmed.
    }

    private void startPresence() {
        presenceHandler.removeCallbacks(presenceBeat);
        updatePresence(true);
        presenceHandler.postDelayed(presenceBeat,30000);
    }

    private void updatePresence(boolean online) {
        FirebaseUser u=auth.getCurrentUser();
        if(u==null || !profileReady) return;
        Map<String,Object> state=new HashMap<>();
        state.put("uid",u.getUid());
        state.put("online",online);
        state.put("lastActive",FieldValue.serverTimestamp());
        db.collection("publicProfiles").document(u.getUid()).set(state,SetOptions.merge());
    }

    private void showContacts() {
        FirebaseUser current=auth.getCurrentUser();
        if(current==null){showLogin();return;}
        setup(); header();
        text("دليل أعضاء سمسم",22,WHITE,true);
        text("اختر عضواً لعرض حالته وملفه",13,GRAY,false);
        contactsSearch=input("ابحث بالاسم...",InputType.TYPE_CLASS_TEXT);
        contactsSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {
                if(lastContacts!=null) renderContacts(lastContacts);
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        contactsList=new LinearLayout(this);
        contactsList.setOrientation(LinearLayout.VERTICAL);
        root.addView(contactsList,new LinearLayout.LayoutParams(-1,-2));
        Button refresh=button("تحديث القائمة");
        refresh.setOnClickListener(v -> loadContactsOnce());
        Button back=button("رجوع");
        back.setOnClickListener(v -> showDashboard());
        attachContactsListener();
    }

    private void attachContactsListener() {
        if(contactsListener!=null) contactsListener.remove();
        contactsListener=db.collection("publicProfiles").addSnapshotListener((snap,error) -> {
            if(error!=null){toast("تعذر تحميل الأعضاء. تحقق من قواعد Firestore");return;}
            renderContacts(snap);
        });
    }

    private void loadContactsOnce() {
        db.collection("publicProfiles").get()
            .addOnSuccessListener(this::renderContacts)
            .addOnFailureListener(e -> toast("تعذر تحديث قائمة الأعضاء"));
    }

    private void renderContacts(com.google.firebase.firestore.QuerySnapshot snap) {
        if(contactsList==null || snap==null) return;
        lastContacts=snap;
        contactsList.removeAllViews();
        String filter=contactsSearch==null?"":contactsSearch.getText().toString().trim().toLowerCase();
        int count=0;
        for(QueryDocumentSnapshot doc:snap) {
            FirebaseUser me=auth.getCurrentUser();
            if(me!=null && doc.getId().equals(me.getUid())) continue;
            String name=doc.getString("displayName");
            if(name==null || name.trim().isEmpty()) name="عضو سمسم";
            final String shownName=name;
            if(!filter.isEmpty() && !shownName.toLowerCase().contains(filter)) continue;
            boolean online=Boolean.TRUE.equals(doc.getBoolean("online"));
            com.google.firebase.Timestamp last=doc.getTimestamp("lastActive");
            if(last==null || System.currentTimeMillis()-last.toDate().getTime()>90000L) online=false;
            final boolean onlineStatus=online;
            LinearLayout card=new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16),dp(12),dp(16),dp(12));
            card.setBackground(shape(PANEL,BLUE));
            TextView title=new TextView(this);
            title.setText("◉  "+shownName);
            title.setTextColor(WHITE); title.setTextSize(18); title.setTypeface(null,Typeface.BOLD);
            TextView status=new TextView(this);
            status.setText(onlineStatus?"● متصل الآن":"○ غير متصل");
            status.setTextColor(onlineStatus?Color.rgb(40,220,160):GRAY); status.setTextSize(13);
            card.addView(title); card.addView(status);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,dp(6),0,dp(6));
            contactsList.addView(card,lp);
            final String targetUid=doc.getId();
            card.setOnClickListener(v -> openChat(targetUid,shownName));
            count++;
        }
        if(count==0) {
            TextView empty=new TextView(this);
            empty.setText("لا يوجد أعضاء آخرون بعد. اطلب من أصدقائك التسجيل برمز دعوة.");
            empty.setTextColor(GRAY); empty.setTextSize(15); empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12),dp(24),dp(12),dp(24));
            contactsList.addView(empty);
        }
    }

    private String chatIdFor(String a,String b) {
        return a.compareTo(b)<0 ? a+"_"+b : b+"_"+a;
    }

    private void openChat(String otherUid,String otherName) {
        FirebaseUser me=auth.getCurrentUser();
        if(me==null) { showLogin(); return; }
        if(otherUid==null || otherUid.equals(me.getUid())) {
            toast("اختر عضواً آخر لبدء المحادثة");
            return;
        }
        activeChatId=chatIdFor(me.getUid(),otherUid);
        final String chatId=activeChatId;
        final DocumentReference chatRef=db.collection("chats").document(chatId);
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot existing=transaction.get(chatRef);
            if(!existing.exists()) {
                Map<String,Object> chat=new HashMap<>();
                java.util.List<String> members=new java.util.ArrayList<>();
                members.add(me.getUid());
                members.add(otherUid);
                chat.put("memberIds",members);
                chat.put("createdAt",FieldValue.serverTimestamp());
                transaction.set(chatRef,chat);
            } else {
                java.util.List<String> members=(java.util.List<String>)existing.get("memberIds");
                if(members==null || !members.contains(me.getUid()) || !members.contains(otherUid)) {
                    throw new FirebaseFirestoreException("Chat membership mismatch",FirebaseFirestoreException.Code.PERMISSION_DENIED);
                }
                // Existing chats are intentionally left unchanged. Firestore rules
                // only permit lastMessage and lastMessageAt updates after creation.
            }
            return null;
        }).addOnSuccessListener(x -> showChatScreen(otherName))
          .addOnFailureListener(e -> {
              String detail=e instanceof FirebaseFirestoreException
                  ? ((FirebaseFirestoreException)e).getCode().name() : e.getClass().getSimpleName();
              toast("تعذر فتح المحادثة: "+detail+". تحقق من نشر قواعد Firestore");
          });
    }

    private void showChatScreen(String otherName) {
        setup(); header();
        text("محادثة مع "+otherName,20,WHITE,true);
        ScrollView scroll=new ScrollView(this);
        messagesContainer=new LinearLayout(this);
        messagesContainer.setOrientation(LinearLayout.VERTICAL);
        messagesContainer.setPadding(dp(4),dp(8),dp(4),dp(8));
        scroll.addView(messagesContainer);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout compose=new LinearLayout(this);
        compose.setOrientation(LinearLayout.HORIZONTAL);
        EditText message=new EditText(this);
        message.setHint("اكتب رسالتك...");
        message.setTextColor(WHITE); message.setHintTextColor(GRAY);
        message.setSingleLine(false); message.setMinHeight(dp(48));
        message.setBackground(shape(PANEL,BLUE));
        compose.addView(message,new LinearLayout.LayoutParams(0,-2,1));
        Button send=new Button(this); send.setText("إرسال"); send.setAllCaps(false);
        compose.addView(send,new LinearLayout.LayoutParams(dp(82),dp(52)));
        root.addView(compose,new LinearLayout.LayoutParams(-1,-2));
        send.setOnClickListener(v -> {
            String body=message.getText().toString().trim();
            FirebaseUser me=auth.getCurrentUser();
            if(body.isEmpty() || me==null) return;
            if(body.length()>4000){toast("الرسالة طويلة جداً");return;}
            send.setEnabled(false);
            Map<String,Object> data=new HashMap<>();
            data.put("senderId",me.getUid());
            data.put("text",body);
            data.put("createdAt",FieldValue.serverTimestamp());
            db.collection("chats").document(activeChatId).collection("messages").add(data)
                .addOnSuccessListener(x -> {
                    Map<String,Object> update=new HashMap<>();
                    update.put("lastMessage",body);
                    update.put("lastMessageAt",FieldValue.serverTimestamp());
                    db.collection("chats").document(activeChatId).set(update,SetOptions.merge());
                    message.setText(""); send.setEnabled(true);
                })
                .addOnFailureListener(e -> {send.setEnabled(true);toast("تعذر إرسال الرسالة");});
        });
        Button back=button("رجوع إلى الأعضاء");
        back.setOnClickListener(v -> { if(messagesListener!=null) messagesListener.remove(); showContacts(); });
        if(messagesListener!=null) messagesListener.remove();
        messagesListener=db.collection("chats").document(activeChatId).collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener((snap,error) -> {
                if(error!=null){toast("تعذر تحميل الرسائل");return;}
                messagesContainer.removeAllViews();
                if(snap==null) return;
                for(com.google.firebase.firestore.QueryDocumentSnapshot item:snap) {
                    String sender=item.getString("senderId");
                    String body=item.getString("text");
                    TextView line=new TextView(this);
                    line.setText((auth.getCurrentUser()!=null && auth.getCurrentUser().getUid().equals(sender)?"أنت: ":"العضو: ")+(body==null?"":body));
                    line.setTextColor(WHITE); line.setTextSize(16);
                    line.setPadding(dp(12),dp(10),dp(12),dp(10));
                    line.setBackground(shape(PANEL,BLUE));
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
                    lp.setMargins(0,dp(4),0,dp(4));
                    messagesContainer.addView(line,lp);
                }
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            });
    }

    @Override protected void onResume() {
        super.onResume();
        if(auth!=null && auth.getCurrentUser()!=null && profileReady) startPresence();
    }

    @Override protected void onPause() {
        super.onPause();
        presenceHandler.removeCallbacks(presenceBeat);
        if(auth!=null && auth.getCurrentUser()!=null && profileReady) updatePresence(false);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        presenceHandler.removeCallbacks(presenceBeat);
        if(contactsListener!=null) contactsListener.remove();
    }
}
