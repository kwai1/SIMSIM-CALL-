
package com.simsim.call;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {

    int bg = Color.rgb(5, 9, 18);
    int panel = Color.rgb(13, 22, 38);
    int blue = Color.rgb(0, 190, 255);
    int white = Color.WHITE;
    int gray = Color.rgb(135, 155, 180);

    LinearLayout root;
    TextView status;

    int dp(int v) {
        return (int)(v * getResources()
                .getDisplayMetrics().density);
    }

    GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }

    void text(String value, int size, int color,
              boolean bold, int margin) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(margin), 0, dp(margin));
        root.addView(t, p);
    }

    TextView button(String value) {
        TextView b = new TextView(this);
        b.setText(value);
        b.setTextSize(17);
        b.setTextColor(white);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(shape(panel, blue, 16));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, dp(58));
        p.setMargins(0, dp(9), 0, dp(9));
        root.addView(b, p);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        ScrollView scroll = new ScrollView(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(23), dp(25), dp(23), dp(30));
        root.setBackgroundColor(bg);

        scroll.addView(root);
        setContentView(scroll);

        text("◈", 58, blue, true, 2);
        text("SIMSIM-CALL", 31, white, true, 0);
        text("PRIVATE VOICE NETWORK", 11, blue, true, 5);

        text("نظام الاتصالات الصوتية الخاصة", 18,
                gray, true, 20);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(15), dp(20), dp(15), dp(20));
        card.setBackground(shape(panel, blue, 18));

        TextView network = new TextView(this);
        network.setText("●  SYSTEM ONLINE");
        network.setTextColor(Color.rgb(0, 220, 150));
        network.setTextSize(14);
        network.setGravity(Gravity.CENTER);

        TextView privateText = new TextView(this);
        privateText.setText("PRIVATE CHANNEL");
        privateText.setTextColor(blue);
        privateText.setTextSize(20);
        privateText.setTypeface(null, Typeface.BOLD);
        privateText.setGravity(Gravity.CENTER);
        privateText.setPadding(0, dp(15), 0, dp(10));

        TextView detail = new TextView(this);
        detail.setText("واجهة الاتصال جاهزة");
        detail.setTextColor(white);
        detail.setTextSize(14);
        detail.setGravity(Gravity.CENTER);

        card.addView(network);
        card.addView(privateText);
        card.addView(detail);

        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(15), 0, dp(20));
        root.addView(card, cp);

        text("بيانات الاتصال", 19, white, true, 12);

        EditText user = new EditText(this);
        user.setHint("أدخل رمز المستخدم");
        user.setHintTextColor(gray);
        user.setTextColor(white);
        user.setTextSize(16);
        user.setSingleLine(true);
        user.setPadding(dp(15), 0, dp(15), 0);
        user.setBackground(shape(panel, blue, 13));

        root.addView(user,
                new LinearLayout.LayoutParams(-1, dp(58)));

        text("قناة صوتية خاصة", 14, gray, true, 18);

        TextView call = button("◉   بدء الاتصال الصوتي");

        status = new TextView(this);
        status.setText("بانتظار إدخال رمز المستخدم");
        status.setTextColor(blue);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(18), 0, dp(18));
        root.addView(status);

        call.setOnClickListener(v -> {
            if (user.getText().toString().trim().isEmpty()) {
                status.setText("يرجى إدخال رمز المستخدم");
            } else {
                status.setText(
                    "تم تجهيز طلب الاتصال. الربط الصوتي يحتاج خادماً."
                );
            }
        });

        TextView end = button("■   إنهاء الاتصال");
        end.setOnClickListener(v ->
                status.setText("لا يوجد اتصال نشط"));

        text("SECURITY STATUS", 12, blue, true, 25);
        text("PRIVATE ACCESS  •  SIMSIM NETWORK",
                10, gray, false, 5);
        text("إصدار الواجهة 1.0", 11, gray, false, 5);
    }
}
