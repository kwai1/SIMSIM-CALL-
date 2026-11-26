package com.simsim.call;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
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
import java.util.UUID;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private final int BG = Color.rgb(5, 9, 18);
    private final int PANEL = Color.rgb(13, 22, 38);
    private final int BLUE = Color.rgb(0, 190, 255);
    private final int WHITE = Color.WHITE;
    private final int GRAY = Color.rgb(145, 165, 185);
    private LinearLayout root;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private final Handler presenceHandler = new Handler(Looper.getMainLooper());
    private ListenerRegistration contactsListener;
    private LinearLayout contactsList;
    private EditText contactsSearch;
    private com.google.firebase.firestore.QuerySnapshot lastContacts;
    private String myDisplayName = "عضو سمسم";
    private String myPublicKey;
    private String myUserCode;
    private String activeOtherUid;
    private String activeOtherPublicKey;
    private boolean profileReady = false;
    private ListenerRegistration messagesListener;
    private ListenerRegistration incomingCallsListener;
    private ListenerRegistration currentCallListener;
    private DocumentReference currentCallRef;
    private CallManager callManager;
    private AlertDialog incomingCallDialog;
    private AlertDialog activeCallDialog;
    private String joiningCallId;
    private String pendingCallId;
    private boolean pendingOutgoingCall = false;
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
        callManager = new CallManager(this);
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
            profile.put("invitationCode",code);
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
        Button manage=button("إدارة المستخدمين");
        manage.setOnClickListener(v -> showAdminUsers());
        Button logout=button("تسجيل الخروج"); logout.setOnClickListener(v -> { auth.signOut(); showLogin(); });
    }


    private void showAdminUsers() {
        setup(); header();
        text("إدارة الحسابات",22,WHITE,true);
        db.collection("users").get()
            .addOnSuccessListener(snapshot -> {
                if(snapshot.isEmpty()) text("لا توجد حسابات مسجلة بعد",14,GRAY,false);
                for(com.google.firebase.firestore.DocumentSnapshot member:snapshot) {
                    String email=member.getString("email");
                    String status=member.getString("status");
                    String uid=member.getId();
                    text((email==null?uid:email)+"\n"+uid+"\nالحالة: "+status,13,WHITE,false);
                    boolean active="active".equals(status);
                    Button toggle=button(active?"تعطيل هذا الحساب":"إعادة تفعيل الحساب");
                    toggle.setOnClickListener(v -> {
                        toggle.setEnabled(false);
                        db.collection("users").document(uid)
                            .update("status",active?"disabled":"active")
                            .addOnSuccessListener(x -> {toast(active?"تم تعطيل الحساب":"تم تفعيل الحساب");showAdminUsers();})
                            .addOnFailureListener(e -> {toggle.setEnabled(true);toast("تعذر تعديل صلاحية الحساب");});
                    });
                }
            })
            .addOnFailureListener(e -> toast("تعذر تحميل قائمة المستخدمين. تحقق من صلاحية المدير."));
        Button back=button("رجوع إلى لوحة المدير");
        back.setOnClickListener(v -> showAdmin());
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
        setup(); header();
        text("ACCESS GRANTED",20,BLUE,true);
        text("مرحباً بك في شبكة سمسم",19,WHITE,true);
        FirebaseUser u=auth.getCurrentUser();
        if(u==null){ showLogin(); return; }
        if(u.getEmail()!=null) text(u.getEmail(),13,GRAY,false);
        text("رمز المستخدم الخاص بك",15,BLUE,true);
        TextView codeView=new TextView(this);
        codeView.setText("جارٍ تجهيز هويتك الآمنة...");
        codeView.setTextColor(WHITE); codeView.setTextSize(20);
        codeView.setGravity(Gravity.CENTER); codeView.setPadding(dp(12),dp(14),dp(12),dp(14));
        codeView.setBackground(shape(PANEL,BLUE));
        root.addView(codeView,new LinearLayout.LayoutParams(-1,dp(62)));
        final EditText nameField=input("اسم العرض",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        db.collection("publicProfiles").document(u.getUid()).get()
            .addOnSuccessListener(doc -> {
                String saved=doc.getString("displayName");
                if(saved==null || saved.trim().isEmpty())
                    saved="عضو "+u.getUid().substring(0,Math.min(5,u.getUid().length()));
                nameField.setText(saved);
                ensureSecureProfile(u,saved,codeView);
            })
            .addOnFailureListener(e -> toast("تعذر تحميل ملفك. تحقق من قواعد Firestore."));
        Button saveName=button("حفظ الاسم");
        saveName.setOnClickListener(v -> {
            String value=nameField.getText().toString().trim();
            if(value.length()<2 || value.length()>30){toast("الاسم يجب أن يكون بين حرفين و30 حرفاً");return;}
            if(myPublicKey==null || myUserCode==null){toast("انتظر تجهيز الهوية الآمنة");return;}
            Map<String,Object> profile=new HashMap<>();
            profile.put("uid",u.getUid()); profile.put("displayName",value);
            profile.put("online",true); profile.put("lastActive",FieldValue.serverTimestamp());
            profile.put("createdAt",FieldValue.serverTimestamp());
            profile.put("publicKey",myPublicKey); profile.put("userCode",myUserCode);
            saveName.setEnabled(false);
            db.collection("publicProfiles").document(u.getUid()).set(profile,SetOptions.merge())
                .addOnSuccessListener(x -> {myDisplayName=value;saveName.setEnabled(true);toast("تم حفظ الاسم");startPresence();})
                .addOnFailureListener(err -> {saveName.setEnabled(true);toast("تعذر حفظ الاسم");});
        });
        Button copyUserCode=button("نسخ رمز المستخدم");
        copyUserCode.setOnClickListener(v -> {
            if(myUserCode==null){toast("انتظر تجهيز رمزك");return;}
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("SIMSIM USER CODE",myUserCode));
            toast("تم نسخ رمز المستخدم");
        });
        Button search=button("⌕  البحث عن شخص برمز المستخدم");
        search.setOnClickListener(v -> showContacts());
        Button calls=button("◉  الاتصالات الصوتية");
        calls.setOnClickListener(v -> toast("افتح محادثة مع الشخص ثم اضغط اتصال صوتي مشفّر."));
        Button logout=button("تسجيل الخروج");
        logout.setOnClickListener(v -> {
            updatePresence(false); profileReady=false; myPublicKey=null; myUserCode=null;
            auth.signOut(); showLogin();
        });
    }

    private String createUserCode() {
        String chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random=new SecureRandom();
        StringBuilder code=new StringBuilder("SSM");
        for(int i=0;i<8;i++) code.append(chars.charAt(random.nextInt(chars.length())));
        return code.toString();
    }

    private void ensureSecureProfile(FirebaseUser user,String displayName,TextView codeView) {
        try {
            final String localPublicKey=CryptoManager.getPublicKeyBase64();
            final DocumentReference profileRef=db.collection("publicProfiles").document(user.getUid());
            profileRef.get().addOnSuccessListener(profileDoc -> {
                String registeredKey=profileDoc.getString("publicKey");
                if(registeredKey!=null && !registeredKey.equals(localPublicKey)) {
                    profileReady=false;
                    codeView.setText("مفتاح جهاز مختلف");
                    toast("مفتاح التشفير على هذا الجهاز لا يطابق المفتاح المسجل. لا تحذف بيانات التطبيق قبل إعداد استعادة آمنة.");
                    return;
                }
                String existingCode=profileDoc.getString("userCode");
                final String code=(existingCode==null || existingCode.isEmpty())?createUserCode():existingCode;
                DocumentReference codeRef=db.collection("userCodes").document(code);
                db.runTransaction(transaction -> {
                    com.google.firebase.firestore.DocumentSnapshot codeDoc=transaction.get(codeRef);
                    if(codeDoc.exists() && !user.getUid().equals(codeDoc.getString("uid")))
                        throw new IllegalStateException("USER_CODE_COLLISION");
                    Map<String,Object> data=new HashMap<>();
                    data.put("uid",user.getUid()); data.put("displayName",displayName);
                    data.put("online",true); data.put("lastActive",FieldValue.serverTimestamp());
                    data.put("createdAt",FieldValue.serverTimestamp());
                    data.put("publicKey",localPublicKey); data.put("userCode",code);
                    transaction.set(profileRef,data,SetOptions.merge());
                    if(!codeDoc.exists()) {
                        Map<String,Object> mapping=new HashMap<>();
                        mapping.put("uid",user.getUid());
                        mapping.put("createdAt",FieldValue.serverTimestamp());
                        transaction.set(codeRef,mapping);
                    }
                    return code;
                }).addOnSuccessListener(savedCode -> {
                    myPublicKey=localPublicKey; myUserCode=savedCode; myDisplayName=displayName;
                    codeView.setText(savedCode);
                    profileReady=true; startPresence(); attachIncomingCallsListener();
                }).addOnFailureListener(e -> {
                    profileReady=false; codeView.setText("تعذر تجهيز الرمز");
                    toast("تعذر تجهيز رمز المستخدم. تحقق من قواعد Firestore الجديدة.");
                });
            }).addOnFailureListener(e -> toast("تعذر قراءة ملف المستخدم"));
        } catch(Exception e) {
            profileReady=false;
            toast("تعذر إنشاء مفتاح التشفير على هذا الجهاز");
        }
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
        text("البحث برمز المستخدم",22,WHITE,true);
        text("أدخل رمز SIMSIM الذي أعطاك إياه الشخص",13,GRAY,false);
        EditText codeInput=input("مثال: SSMXXXXXXXX",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        Button find=button("بحث");
        find.setOnClickListener(v -> {
            String code=codeInput.getText().toString().trim().toUpperCase();
            if(code.length()!=11 || !code.startsWith("SSM")) {
                toast("تحقق من الرمز. يجب أن يبدأ بـ SSM");
                return;
            }
            find.setEnabled(false);
            db.collection("userCodes").document(code).get()
                .addOnSuccessListener(mapping -> {
                    if(!mapping.exists()) {
                        find.setEnabled(true); toast("لم يتم العثور على مستخدم بهذا الرمز"); return;
                    }
                    String uid=mapping.getString("uid");
                    if(uid==null || uid.equals(current.getUid())) {
                        find.setEnabled(true); toast("هذا رمز حسابك أو أنه غير صالح"); return;
                    }
                    db.collection("publicProfiles").document(uid).get()
                        .addOnSuccessListener(profile -> {
                            find.setEnabled(true);
                            if(!profile.exists()) {toast("ملف المستخدم غير متاح");return;}
                            String name=profile.getString("displayName");
                            String publicKey=profile.getString("publicKey");
                            if(publicKey==null || publicKey.isEmpty()) {toast("هذا المستخدم يحتاج تحديث التطبيق لتفعيل التشفير");return;}
                            openChat(uid,name==null?"عضو سمسم":name,publicKey);
                        })
                        .addOnFailureListener(e -> {find.setEnabled(true);toast("تعذر تحميل ملف المستخدم");});
                })
                .addOnFailureListener(e -> {find.setEnabled(true);toast("تعذر البحث. تحقق من اتصال الإنترنت");});
        });
        Button back=button("رجوع");
        back.setOnClickListener(v -> showDashboard());
    }

    private String chatIdFor(String a,String b) {
        return a.compareTo(b)<0 ? a+"_"+b : b+"_"+a;
    }

    private void openChat(String otherUid,String otherName,String otherPublicKey) {
        FirebaseUser me=auth.getCurrentUser();
        if(me==null || !profileReady || myPublicKey==null) {showLogin();return;}
        activeOtherUid=otherUid;
        activeOtherPublicKey=otherPublicKey;
        activeChatId=chatIdFor(me.getUid(),otherUid);
        DocumentReference chatRef=db.collection("chats").document(activeChatId);
        chatRef.get().addOnSuccessListener(existing -> {
            if(existing.exists()) {
                showChatScreen(otherName);
                return;
            }
            Map<String,Object> chat=new HashMap<>();
            java.util.List<String> members=new java.util.ArrayList<>();
            members.add(me.getUid()); members.add(otherUid);
            chat.put("memberIds",members);
            chat.put("createdAt",FieldValue.serverTimestamp());
            chatRef.set(chat)
                .addOnSuccessListener(x -> showChatScreen(otherName))
                .addOnFailureListener(e -> toast("تعذر إنشاء المحادثة. تحقق من صلاحية الحسابين."));
        }).addOnFailureListener(e -> toast("تعذر فتح المحادثة"));
    }

    private void showChatScreen(String otherName) {
        setup(); header();
        text("محادثة مشفرة مع "+otherName,20,WHITE,true);
        text("تشفير طرف إلى طرف • لا يُحفظ النص مكشوفاً",11,BLUE,true);
        Button callButton=button("اتصال صوتي مشفّر");
        callButton.setOnClickListener(v -> startOutgoingCall());
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
            if(myPublicKey==null || activeOtherPublicKey==null || activeOtherUid==null) {
                toast("مفاتيح التشفير غير جاهزة"); return;
            }
            send.setEnabled(false);
            try {
                Map<String,Object> data=CryptoManager.encryptForChat(
                    body,me.getUid(),myPublicKey,activeOtherUid,activeOtherPublicKey);
                data.put("senderId",me.getUid());
                data.put("createdAt",FieldValue.serverTimestamp());
                db.collection("chats").document(activeChatId).collection("messages").add(data)
                    .addOnSuccessListener(x -> {
                        Map<String,Object> update=new HashMap<>();
                        update.put("lastMessageAt",FieldValue.serverTimestamp());
                        db.collection("chats").document(activeChatId).set(update,SetOptions.merge());
                        message.setText(""); send.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {send.setEnabled(true);toast("تعذر إرسال الرسالة المشفرة");});
            } catch(Exception e) {
                send.setEnabled(true);
                toast("فشل تشفير الرسالة. تحقق من مفاتيح الطرفين.");
            }
        });
        Button back=button("رجوع إلى البحث");
        back.setOnClickListener(v -> {
            if(messagesListener!=null) messagesListener.remove();
            showContacts();
        });
        if(messagesListener!=null) messagesListener.remove();
        messagesListener=db.collection("chats").document(activeChatId).collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener((snap,error) -> {
                if(error!=null){toast("تعذر تحميل الرسائل المشفرة");return;}
                messagesContainer.removeAllViews();
                if(snap==null) return;
                for(com.google.firebase.firestore.QueryDocumentSnapshot item:snap) {
                    String sender=item.getString("senderId");
                    String plain;
                    try {
                        Map<String,Object> wrapped=(Map<String,Object>)item.get("wrappedKeys");
                        plain=CryptoManager.decryptMessage(
                            item.getString("ciphertext"),item.getString("iv"),wrapped,
                            auth.getCurrentUser().getUid());
                    } catch(Exception decryptError) {
                        plain="رسالة مشفرة — لا يمكن فكها بهذا الجهاز";
                    }
                    TextView line=new TextView(this);
                    line.setText((auth.getCurrentUser()!=null && auth.getCurrentUser().getUid().equals(sender)?"أنت: ":"العضو: ")+plain);
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


    private void attachIncomingCallsListener() {
        FirebaseUser me=auth.getCurrentUser();
        if(me==null || !profileReady) return;
        if(incomingCallsListener!=null) incomingCallsListener.remove();
        incomingCallsListener=db.collection("calls")
            .whereEqualTo("calleeUid",me.getUid())
            .addSnapshotListener((snap,error) -> {
                if(error!=null || snap==null) return;
                for(com.google.firebase.firestore.QueryDocumentSnapshot call:snap) {
                    if("ringing".equals(call.getString("status"))) showIncomingCall(call);
                }
            });
    }

    private void showIncomingCall(com.google.firebase.firestore.DocumentSnapshot call) {
        if(incomingCallDialog!=null && incomingCallDialog.isShowing()) return;
        String callerUid=call.getString("callerUid");
        if(callerUid==null) return;
        db.collection("publicProfiles").document(callerUid).get()
            .addOnSuccessListener(profile -> {
                String callerName=profile.getString("displayName");
                if(callerName==null) callerName="عضو سمسم";
                if(isFinishing() || (incomingCallDialog!=null && incomingCallDialog.isShowing())) return;
                incomingCallDialog=new AlertDialog.Builder(this)
                    .setTitle("مكالمة واردة")
                    .setMessage("مكالمة صوتية مشفرة من: "+callerName)
                    .setPositiveButton("قبول", (dialog,which) ->
                        call.getReference().update(
                            "status","accepted",
                            "acceptedAt",FieldValue.serverTimestamp())
                            .addOnSuccessListener(x -> joinCallFromDocument(call))
                            .addOnFailureListener(e -> toast("تعذر قبول المكالمة")))
                    .setNegativeButton("رفض", (dialog,which) ->
                        call.getReference().update("status","rejected")
                            .addOnFailureListener(e -> toast("تعذر رفض المكالمة")))
                    .setCancelable(false)
                    .create();
                incomingCallDialog.show();
            });
    }

    private void startOutgoingCall() {
        FirebaseUser me=auth.getCurrentUser();
        if(me==null || activeOtherUid==null || activeOtherPublicKey==null
                || myPublicKey==null || myUserCode==null) {
            toast("افتح محادثة مع مستخدم صالح أولاً"); return;
        }
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            pendingOutgoingCall=true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},701);
            return;
        }
        final String callId=UUID.randomUUID().toString();
        final String roomName="simsim-"+callId;
        try {
            String secret=CryptoManager.createCallSecret();
            Map<String,Object> encrypted=CryptoManager.encryptForChat(
                secret,me.getUid(),myPublicKey,activeOtherUid,activeOtherPublicKey);
            Map<String,Object> call=new HashMap<>();
            call.put("callerUid",me.getUid());
            call.put("calleeUid",activeOtherUid);
            call.put("roomName",roomName);
            call.put("status","ringing");
            call.put("callKeyCiphertext",encrypted.get("ciphertext"));
            call.put("callKeyIv",encrypted.get("iv"));
            call.put("callKeyWrappedKeys",encrypted.get("wrappedKeys"));
            call.put("encryptionVersion",1);
            call.put("createdAt",FieldValue.serverTimestamp());
            DocumentReference ref=db.collection("calls").document(callId);
            ref.set(call).addOnSuccessListener(x -> {
                currentCallRef=ref;
                watchCurrentCall(ref);
                toast("تم إرسال طلب الاتصال. بانتظار قبول الطرف الآخر.");
            }).addOnFailureListener(e -> toast("تعذر إنشاء المكالمة. تحقق من قواعد Firestore."));
        } catch(Exception e) {
            toast("تعذر إنشاء مفتاح تشفير المكالمة");
        }
    }


    private void watchCurrentCall(DocumentReference ref) {
        if(currentCallListener!=null) currentCallListener.remove();
        currentCallListener=ref.addSnapshotListener((snapshot,error) -> {
            if(error!=null || snapshot==null || !snapshot.exists()) return;
            String status=snapshot.getString("status");
            if("accepted".equals(status)) joinCallFromDocument(snapshot);
            else if("rejected".equals(status)) {
                joiningCallId=null;
                toast("تم رفض المكالمة");
                if(currentCallListener!=null) currentCallListener.remove();
            } else if("ended".equals(status)) {
                joiningCallId=null;
                if(callManager!=null) callManager.disconnect(null);
                if(activeCallDialog!=null && activeCallDialog.isShowing()) activeCallDialog.dismiss();
                toast("انتهت المكالمة");
            }
        });
    }

    private void joinCallFromDocument(com.google.firebase.firestore.DocumentSnapshot call) {
        FirebaseUser me=auth.getCurrentUser();
        if(me==null || !call.exists()) return;
        String callId=call.getId();
        currentCallRef=call.getReference();
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            pendingCallId=callId;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},701);
            return;
        }
        if(callId.equals(joiningCallId)) return;
        joiningCallId=callId;
        try {
            Map<String,Object> wrapped=(Map<String,Object>)call.get("callKeyWrappedKeys");
            String sharedKey=CryptoManager.decryptMessage(
                call.getString("callKeyCiphertext"),
                call.getString("callKeyIv"),
                wrapped,
                me.getUid());
            requestCallTokenAndConnect(callId,sharedKey);
        } catch(Exception e) {
            joiningCallId=null;
            toast("تعذر فك مفتاح المكالمة على هذا الجهاز");
        }
    }

    private void requestCallTokenAndConnect(String callId,String sharedKey) {
        String baseUrl=BuildConfig.SIMSIM_SERVER_URL;
        if(baseUrl==null || baseUrl.trim().isEmpty()) {
            joiningCallId=null;
            toast("خادم المكالمات لم يُنشر بعد. يلزم إعداد رابط Render.");
            return;
        }
        FirebaseUser me=auth.getCurrentUser();
        if(me==null) { joiningCallId=null; showLogin(); return; }
        me.getIdToken(true).addOnSuccessListener(tokenResult -> {
            String idToken=tokenResult.getToken();
            new Thread(() -> {
                HttpURLConnection connection=null;
                try {
                    URL url=new URL(baseUrl.replaceAll("/+$","")+"/token");
                    connection=(HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(20000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type","application/json");
                    connection.setRequestProperty("Authorization","Bearer "+idToken);
                    JSONObject payload=new JSONObject();
                    payload.put("callId",callId);
                    byte[] body=payload.toString().getBytes(StandardCharsets.UTF_8);
                    try(OutputStream os=connection.getOutputStream()) { os.write(body); }
                    int status=connection.getResponseCode();
                    InputStream stream=(status>=200 && status<300)
                        ? connection.getInputStream() : connection.getErrorStream();
                    ByteArrayOutputStream buffer=new ByteArrayOutputStream();
                    byte[] data=new byte[4096]; int n;
                    while(stream!=null && (n=stream.read(data))!=-1) buffer.write(data,0,n);
                    String response=new String(buffer.toByteArray(),StandardCharsets.UTF_8);
                    JSONObject json=new JSONObject(response);
                    if(status<200 || status>=300)
                        throw new Exception(json.optString("error","رفض الخادم الاتصال"));
                    String token=json.getString("token");
                    String serverUrl=json.getString("serverUrl");
                    runOnUiThread(() -> callManager.connect(serverUrl,token,sharedKey,new CallManager.Callback() {
                        @Override public void onSuccess() {
                            toast("تم الاتصال الصوتي المشفر");
                            activeCallDialog=new AlertDialog.Builder(MainActivity.this)
                                .setTitle("SIMSIM CALL")
                                .setMessage("المكالمة الصوتية المشفرة نشطة")
                                .setPositiveButton("إنهاء المكالمة",(dialog,which) -> endCurrentCall())
                                .setCancelable(false).create();
                            activeCallDialog.show();
                        }
                        @Override public void onError(String message) {
                            joiningCallId=null;
                            toast("فشل الاتصال: "+message);
                        }
                    }));
                } catch(Exception error) {
                    runOnUiThread(() -> {
                        joiningCallId=null;
                        toast("تعذر الحصول على رمز اتصال آمن: "+error.getMessage());
                    });
                } finally {
                    if(connection!=null) connection.disconnect();
                }
            }).start();
        }).addOnFailureListener(e -> {
            joiningCallId=null;
            toast("تعذر التحقق من حساب Firebase");
        });
    }


    private void endCurrentCall() {
        if(currentCallRef!=null) {
            currentCallRef.update("status","ended","endedAt",FieldValue.serverTimestamp());
        }
        if(callManager!=null) callManager.disconnect(null);
        if(currentCallListener!=null) currentCallListener.remove();
        currentCallListener=null; currentCallRef=null; joiningCallId=null;
        if(activeCallDialog!=null && activeCallDialog.isShowing()) activeCallDialog.dismiss();
        toast("تم إنهاء المكالمة");
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==701) {
            boolean granted=grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED;
            if(!granted) {
                pendingOutgoingCall=false; pendingCallId=null;
                toast("يجب السماح باستخدام الميكروفون لإجراء المكالمة");
                return;
            }
            if(pendingOutgoingCall) {
                pendingOutgoingCall=false;
                startOutgoingCall();
            } else if(pendingCallId!=null) {
                String id=pendingCallId; pendingCallId=null;
                db.collection("calls").document(id).get()
                    .addOnSuccessListener(this::joinCallFromDocument);
            }
        }
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
        if(incomingCallsListener!=null) incomingCallsListener.remove();
        if(currentCallListener!=null) currentCallListener.remove();
        if(callManager!=null) callManager.release();
    }
}
