
package com.simsim.call;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.text.InputType;
import android.widget.*;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private final int BG = Color.rgb(5, 9, 18);
    private final int PANEL = Color.rgb(13, 22, 38);
    private final int BLUE = Color.rgb(0, 190, 255);
    private final int WHITE = Color.WHITE;
    private final int GRAY = Color.rgb(145, 165, 185);

    private LinearLayout root;
    private SharedPreferences storage;

    private final String ADMIN_KEY = "SIMSIM-ADMIN-2026";
    private final String PREF = "simsim_private_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        storage = getSharedPreferences(PREF, MODE_PRIVATE);
        showLogin();
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private void setup() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(30));
        root.setBackgroundColor(BG);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void text(String value, int size,
                      int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);

        if (bold) {
            t.setTypeface(null, Typeface.BOLD);
        }

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(0, dp(7), 0, dp(7));
        root.addView(t, p);
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setBackground(shape(PANEL, BLUE));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, dp(58));

        p.setMargins(0, dp(9), 0, dp(9));
        root.addView(b, p);

        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(GRAY);
        e.setTextColor(WHITE);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setPadding(dp(15), 0, dp(15), 0);
        e.setBackground(shape(PANEL, BLUE));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, dp(58));

        p.setMargins(0, dp(12), 0, dp(12));
        root.addView(e, p);

        return e;
    }

    private void header() {
        text("◈", 55, BLUE, true);
        text("SIMSIM-CALL", 32, WHITE, true);
        text("PRIVATE COMMUNICATION NETWORK",
                11, BLUE, true);
        text("SYSTEM STATUS  •  PROTECTED ACCESS",
                11, GRAY, false);
    }

    private void showLogin() {
        setup();
        header();

        text("SECURITY GATE", 20, WHITE, true);
        text("بوابة الدخول الآمن", 17, BLUE, true);
        text("أدخل رمز الدعوة للوصول إلى النظام",
                14, GRAY, false);

        final EditText code = input("رمز الدخول السري");

        code.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        );

        Button enter = button("دخول إلى النظام");

        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = code.getText().toString().trim();

                if (value.equals(ADMIN_KEY)) {
                    showAdmin();
                    return;
                }

                Set<String> codes = new HashSet<String>(
                        storage.getStringSet(
                                "access_codes",
                                new HashSet<String>()
                        )
                );

                if (!value.isEmpty() && codes.contains(value)) {
                    showDashboard();
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            "رمز الدخول غير صحيح",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });

        text("AUTHORIZED USERS ONLY", 11, BLUE, true);
        text("SIMSIM SECURITY SYSTEM © 2026",
                10, GRAY, false);
    }

    private void showAdmin() {
        setup();
        header();

        text("ADMIN CONTROL", 23, WHITE, true);
        text("لوحة تحكم المدير الرئيسي", 17, BLUE, true);

        Button generate = button("＋ إنشاء رمز دعوة جديد");

        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = createCode();

                Set<String> codes = new HashSet<String>(
                        storage.getStringSet(
                                "access_codes",
                                new HashSet<String>()
                        )
                );

                codes.add(code);

                storage.edit()
                        .putStringSet("access_codes", codes)
                        .apply();

                showGeneratedCode(code);
            }
        });

        Button count = button("عرض عدد رموز الدعوة");

        count.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<String> codes = storage.getStringSet(
                        "access_codes",
                        new HashSet<String>()
                );

                Toast.makeText(
                        MainActivity.this,
                        "عدد الرموز: " + codes.size(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        Button logout = button("تسجيل الخروج");
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogin();
            }
        });

        text("ADMINISTRATOR • CONTROL PANEL",
                10, BLUE, false);
    }

    private String createCode() {
        String chars =
                "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder("SIM-");

        for (int i = 0; i < 8; i++) {
            code.append(
                    chars.charAt(random.nextInt(chars.length()))
            );
        }

        return code.toString();
    }

    private void showGeneratedCode(final String code) {
        setup();
        header();

        text("INVITATION CREATED", 19, WHITE, true);
        text("رمز الدعوة الجديد", 17, GRAY, true);

        TextView result = new TextView(this);
        result.setText(code);
        result.setTextSize(23);
        result.setTextColor(BLUE);
        result.setGravity(Gravity.CENTER);
        result.setTypeface(null, Typeface.BOLD);
        result.setPadding(dp(10), dp(20), dp(10), dp(20));
        result.setBackground(shape(PANEL, BLUE));

        root.addView(result,
                new LinearLayout.LayoutParams(-1, dp(75)));

        Button copy = button("نسخ رمز الدعوة");

        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );

                clipboard.setPrimaryClip(
                        ClipData.newPlainText("SIMSIM CODE", code)
                );

                Toast.makeText(
                        MainActivity.this,
                        "تم نسخ الرمز",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        Button back = button("العودة إلى لوحة المدير");

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdmin();
            }
        });
    }

    private void showDashboard() {
        setup();
        header();

        text("ACCESS GRANTED", 20, BLUE, true);
        text("مرحباً بك في الشبكة الخاصة", 19, WHITE, true);

        text("IDENTITY VERIFIED", 12, GRAY, true);

        Button calls = button("◉  الاتصالات");
        calls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(
                        MainActivity.this,
                        "خدمة الاتصالات قيد التطوير",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        Button messages = button("▤  المحادثات");
        messages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(
                        MainActivity.this,
                        "خدمة المحادثات قيد التطوير",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        Button logout = button("تسجيل الخروج");
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogin();
            }
        });

        text("SIMSIM PRIVATE NETWORK",
                10, BLUE, false);
    }
}
