package com.gayangangacoachingcenter.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends Activity {
    private int dp(float v){ return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView tv(String text, float size, int color, boolean bold){
        TextView t=new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private GradientDrawable bg(int color,float radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private LinearLayout card(String icon,String title){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setGravity(Gravity.CENTER); c.setPadding(dp(8),dp(10),dp(8),dp(8)); c.setBackground(bg(Color.WHITE,10));
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0; lp.height=dp(150); lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f); lp.setMargins(dp(6),dp(6),dp(6),dp(6)); c.setLayoutParams(lp);
        TextView i=tv(icon,38,Color.DKGRAY,false); i.setGravity(Gravity.CENTER); c.addView(i,new LinearLayout.LayoutParams(-1,dp(72)));
        TextView t=tv(title,16,Color.rgb(45,45,45),false); t.setGravity(Gravity.CENTER); c.addView(t,new LinearLayout.LayoutParams(-1,dp(58)));
        c.setOnClickListener(v -> showFeature(title)); return c;
    }
    private void showFeature(String title){
        setContentView(simpleScreen(title));
    }
    private View simpleScreen(String title){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(255,248,248));
        TextView bar=tv("‹   "+title,20,Color.WHITE,true); bar.setPadding(dp(10),0,dp(10),0); bar.setBackgroundColor(Color.rgb(25,118,210)); root.addView(bar,new LinearLayout.LayoutParams(-1,dp(60)));
        bar.setOnClickListener(v->setContentView(home()));
        TextView body=tv("यह सेक्शन native Android screen के लिए तैयार है.\nContent/Firebase data यहाँ जोड़ा जा सकता है.",18,Color.DKGRAY,false); body.setPadding(dp(24),dp(30),dp(24),dp(30)); root.addView(body,new LinearLayout.LayoutParams(-1,-1));
        return root;
    }
    private View home(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(255,248,248));
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(16),0,dp(10),0); top.setBackgroundColor(Color.rgb(25,118,210));
        TextView menu=tv("☰",28,Color.WHITE,false); top.addView(menu,new LinearLayout.LayoutParams(dp(46),dp(62)));
        TextView title=tv("GAYAN GANGA COCHING CENTER",18,Color.WHITE,true); top.addView(title,new LinearLayout.LayoutParams(0,dp(62),1));
        TextView bell=tv("♧",25,Color.WHITE,false); top.addView(bell,new LinearLayout.LayoutParams(dp(45),dp(62)));
        root.addView(top);

        ScrollView scroll=new ScrollView(this); LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(14),dp(14),dp(14),dp(14));
        TextView hello=tv("Hello, Lavkush Kumar",24,Color.rgb(35,35,35),false); hello.setPadding(dp(6),dp(4),0,dp(16)); content.addView(hello,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView banner=tv("GAYAN GANGA\nCOACHING CENTER\n\nLive Classes  •  Mock Test  •  Notes  •  Experienced Teachers",19,Color.WHITE,true); banner.setGravity(Gravity.CENTER); banner.setBackground(bg(Color.rgb(210,55,55),8)); content.addView(banner,new LinearLayout.LayoutParams(-1,dp(180)));
        GridLayout grid=new GridLayout(this); grid.setColumnCount(3); grid.setUseDefaultMargins(false);
        String[][] items={{"🎓","Paid Classes"},{"👩‍🏫","Free Courses"},{"📝","Free Weekly Test"},{"📚","Books"},{"📄","PDF Class Notes"},{"⏱","Paid Test Series"},{"📖","Syllabus\nPrevious Year"},{"💡","Create Test"},{"🔤","Daily Quiz"}};
        for(String[] x:items)grid.addView(card(x[0],x[1])); content.addView(grid,new LinearLayout.LayoutParams(-1,dp(480)));
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setBackground(bg(Color.WHITE,22)); nav.setPadding(dp(8),0,dp(8),0);
        String[] n={"⌂\nHome","▤\nCourses","▣\nTests","●\nProfile"}; for(String s:n){TextView b=tv(s,13,Color.DKGRAY,false); b.setGravity(Gravity.CENTER); nav.addView(b,new LinearLayout.LayoutParams(0,dp(64),1));}
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(70))); return root;
    }
    @Override public void onCreate(Bundle b){ super.onCreate(b); setContentView(home()); }
}
