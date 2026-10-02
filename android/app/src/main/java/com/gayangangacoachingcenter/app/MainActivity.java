package com.gayangangacoachingcenter.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private int dp(float v){ return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView tv(String text,float size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private GradientDrawable bg(int color,float radius){
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }
    private EditText input(String hint){
        EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); e.setSingleLine(true);
        e.setPadding(dp(14),0,dp(14),0); e.setBackground(bg(Color.WHITE,10));
        return e;
    }
    private TextView button(String text){
        TextView b=tv(text,16,Color.WHITE,true); b.setGravity(Gravity.CENTER);
        b.setBackground(bg(Color.rgb(25,118,210),10));
        return b;
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        if(auth.getCurrentUser()==null) setContentView(authScreen(false));
        else loadHome();
    }

    private View authScreen(boolean register){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24),dp(36),dp(24),dp(24));
        root.setBackgroundColor(Color.rgb(255,248,248));

        TextView logo=tv("GAYAN GANGA",30,Color.rgb(25,118,210),true);
        logo.setGravity(Gravity.CENTER); root.addView(logo,new LinearLayout.LayoutParams(-1,dp(55)));
        TextView sub=tv(register?"Create your student account":"Student Login",21,Color.DKGRAY,true);
        sub.setGravity(Gravity.CENTER); root.addView(sub,new LinearLayout.LayoutParams(-1,dp(50)));

        EditText name=input("Full Name");
        EditText email=input("Email");
        EditText pass=input("Password");
        pass.setInputType(0x00000081);
        root.addView(name,new LinearLayout.LayoutParams(-1,dp(56)));
        LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-1,dp(56)); gap.topMargin=dp(12);
        root.addView(email,gap);
        LinearLayout.LayoutParams gap2=new LinearLayout.LayoutParams(-1,dp(56)); gap2.topMargin=dp(12);
        root.addView(pass,gap2);

        TextView action=button(register?"CREATE ACCOUNT":"LOGIN");
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(54)); ap.topMargin=dp(20);
        root.addView(action,ap);

        TextView switcher=tv(register?"Already have an account? Login":"New student? Create account",15,Color.rgb(25,118,210),false);
        switcher.setGravity(Gravity.CENTER); root.addView(switcher,new LinearLayout.LayoutParams(-1,dp(55)));

        name.setVisibility(register?View.VISIBLE:View.GONE);
        action.setOnClickListener(v -> {
            String n=name.getText().toString().trim();
            String em=email.getText().toString().trim();
            String pw=pass.getText().toString();
            if(em.isEmpty() || pw.length()<6 || (register && n.isEmpty())){
                Toast.makeText(this,register?"Name, email और 6+ character password भरें":"Email और 6+ character password भरें",Toast.LENGTH_SHORT).show();
                return;
            }
            action.setText("PLEASE WAIT...");
            action.setEnabled(false);
            if(register){
                auth.createUserWithEmailAndPassword(em,pw).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        FirebaseUser u=auth.getCurrentUser();
                        Map<String,Object> profile=new HashMap<>();
                        profile.put("name",n); profile.put("email",em); profile.put("role","student");
                        profile.put("createdAt",com.google.firebase.firestore.FieldValue.serverTimestamp());
                        db.collection("users").document(u.getUid()).set(profile)
                            .addOnCompleteListener(x -> loadHome());
                    } else {
                        action.setEnabled(true); action.setText("CREATE ACCOUNT");
                        Toast.makeText(this,error(task.getException()),Toast.LENGTH_LONG).show();
                    }
                });
            }else{
                auth.signInWithEmailAndPassword(em,pw).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) loadHome();
                    else { action.setEnabled(true); action.setText("LOGIN"); Toast.makeText(this,error(task.getException()),Toast.LENGTH_LONG).show(); }
                });
            }
        });
        switcher.setOnClickListener(v -> setContentView(authScreen(!register)));
        return root;
    }

    private String error(Exception e){
        return e==null?"Something went wrong":(e.getMessage()==null?"Authentication failed":e.getMessage());
    }

    private void loadHome(){
        FirebaseUser u=auth.getCurrentUser();
        if(u==null){setContentView(authScreen(false));return;}
        db.collection("users").document(u.getUid()).get().addOnSuccessListener(doc -> {
            String name=doc.exists()?doc.getString("name"):null;
            if(name==null || name.trim().isEmpty()) name=u.getEmail()==null?"Student":u.getEmail().split("@")[0];
            setContentView(home(name));
        }).addOnFailureListener(e -> setContentView(home(u.getEmail()==null?"Student":u.getEmail().split("@")[0])));
    }

    private LinearLayout card(String icon,String title){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setGravity(Gravity.CENTER);
        c.setPadding(dp(8),dp(10),dp(8),dp(8)); c.setBackground(bg(Color.WHITE,10));
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0; lp.height=dp(150);
        lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f); lp.setMargins(dp(6),dp(6),dp(6),dp(6)); c.setLayoutParams(lp);
        TextView i=tv(icon,38,Color.DKGRAY,false); i.setGravity(Gravity.CENTER);
        c.addView(i,new LinearLayout.LayoutParams(-1,dp(72)));
        TextView t=tv(title,16,Color.rgb(45,45,45),false); t.setGravity(Gravity.CENTER);
        c.addView(t,new LinearLayout.LayoutParams(-1,dp(58)));
        c.setOnClickListener(v -> {\n            if(title.equals("Paid Classes") || title.equals("Free Courses")) coursesScreen();\n            else if(title.equals("Free Weekly Test") || title.equals("Paid Test Series") || title.equals("Daily Quiz")) testsScreen();\n            else showFeature(title);\n        });
        return c;
    }

    private void coursesScreen(){
        LinearLayout root=baseScreen("Courses");
        TextView loading=tv("Courses loading from Firebase...",17,Color.DKGRAY,false);
        loading.setPadding(dp(20),dp(20),dp(20),dp(20));
        root.addView(loading,new LinearLayout.LayoutParams(-1,-1));
        setContentView(root);
        db.collection("courses").get().addOnSuccessListener(result -> {
            LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(14),dp(10),dp(14),dp(20));
            if(result.isEmpty()){
                TextView empty=tv("अभी कोई course उपलब्ध नहीं है.\nAdmin Firebase Firestore में 'courses' collection में course add कर सकता है.",17,Color.DKGRAY,false);
                empty.setGravity(Gravity.CENTER); list.addView(empty,new LinearLayout.LayoutParams(-1,dp(180)));
            } else {
                for(QueryDocumentSnapshot doc:result){
                    String title=doc.getString("title");
                    if(title==null) title=doc.getString("name");
                    if(title==null) title="Course";
                    String description=doc.getString("description");
                    if(description==null) description="";
                    String price=doc.getString("price");
                    if(price==null && doc.get("price")!=null) price=String.valueOf(doc.get("price"));
                    if(price==null) price="";
                    list.addView(courseCard(title,description,price));
                }
            }
            root.removeViews(1,root.getChildCount()-1);
            root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        }).addOnFailureListener(e -> {
            loading.setText("Courses load नहीं हो सके.\n\n"+error(e));
        });
    }

    private LinearLayout courseCard(String title,String description,String price){
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(16),dp(14),dp(16),dp(14));
        card.setBackground(bg(Color.WHITE,12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(135)); lp.setMargins(0,dp(7),0,dp(7)); card.setLayoutParams(lp);
        TextView t=tv(title,19,Color.rgb(25,118,210),true); card.addView(t,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView d=tv(description,14,Color.DKGRAY,false); d.setGravity(Gravity.TOP); card.addView(d,new LinearLayout.LayoutParams(-1,0,1));
        TextView p=tv(price.isEmpty()?"":("₹ "+price),16,Color.rgb(210,55,55),true); p.setGravity(Gravity.CENTER_VERTICAL); card.addView(p,new LinearLayout.LayoutParams(-1,dp(32)));
        card.setOnClickListener(v -> courseDetail(title,description,price));
        return card;
    }

    private void courseDetail(String title,String description,String price){
        LinearLayout root=baseScreen(title);
        TextView body=tv(title+"\n\n"+description+"\n\n"+(price.isEmpty()?"":"Price: ₹ "+price)+"\n\nयह course Firebase Firestore से load हुआ है.",18,Color.DKGRAY,false);
        body.setGravity(Gravity.TOP); body.setPadding(dp(22),dp(25),dp(22),dp(25));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }

    private void testsScreen(){
        LinearLayout root=baseScreen("Tests & Quiz");
        TextView loading=tv("Tests loading from Firebase...",17,Color.DKGRAY,false); loading.setPadding(dp(20),dp(20),dp(20),dp(20));
        root.addView(loading,new LinearLayout.LayoutParams(-1,-1)); setContentView(root);
        db.collection("tests").get().addOnSuccessListener(result -> {
            LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(14),dp(10),dp(14),dp(20));
            if(result.isEmpty()){
                TextView empty=tv("अभी कोई test उपलब्ध नहीं है.\nFirestore → tests collection में test जोड़ें.",17,Color.DKGRAY,false);
                empty.setGravity(Gravity.CENTER); list.addView(empty,new LinearLayout.LayoutParams(-1,dp(180)));
            } else for(QueryDocumentSnapshot doc:result){
                String title=doc.getString("title"); if(title==null) title=doc.getString("name"); if(title==null) title="Test";
                String description=doc.getString("description"); if(description==null) description="";
                list.addView(courseCard(title,description,""));
            }
            root.removeViews(1,root.getChildCount()-1); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        }).addOnFailureListener(e -> loading.setText("Tests load नहीं हो सके.\n\n"+error(e)));
    }

    private LinearLayout baseScreen(String title){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(255,248,248));
        TextView bar=tv("‹   "+title,20,Color.WHITE,true); bar.setPadding(dp(10),0,dp(10),0); bar.setBackgroundColor(Color.rgb(25,118,210));
        root.addView(bar,new LinearLayout.LayoutParams(-1,dp(60))); bar.setOnClickListener(v->loadHome());
        return root;
    }

    private void showFeature(String title){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(255,248,248));
        TextView bar=tv("‹   "+title,20,Color.WHITE,true); bar.setPadding(dp(10),0,dp(10),0); bar.setBackgroundColor(Color.rgb(25,118,210));
        root.addView(bar,new LinearLayout.LayoutParams(-1,dp(60)));
        bar.setOnClickListener(v->loadHome());
        TextView body=tv(title+"\n\nFirebase-connected native screen.\nContent will be loaded from Firestore in the next module.",18,Color.DKGRAY,false);
        body.setGravity(Gravity.TOP); body.setPadding(dp(24),dp(30),dp(24),dp(30));
        root.addView(body,new LinearLayout.LayoutParams(-1,-1));
        setContentView(root);
    }

    private View home(String name){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(255,248,248));
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(16),0,dp(10),0); top.setBackgroundColor(Color.rgb(25,118,210));
        TextView menu=tv("☰",28,Color.WHITE,false); top.addView(menu,new LinearLayout.LayoutParams(dp(46),dp(62)));
        TextView title=tv("GAYAN GANGA COCHING CENTER",18,Color.WHITE,true); top.addView(title,new LinearLayout.LayoutParams(0,dp(62),1));
        TextView profile=tv("●",22,Color.WHITE,false); profile.setGravity(Gravity.CENTER); top.addView(profile,new LinearLayout.LayoutParams(dp(45),dp(62)));
        profile.setOnClickListener(v -> profileScreen(name));
        root.addView(top);

        ScrollView scroll=new ScrollView(this);
        LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(14),dp(14),dp(14),dp(14));
        TextView hello=tv("Hello, "+name,24,Color.rgb(35,35,35),false); hello.setPadding(dp(6),dp(4),0,dp(16));
        content.addView(hello,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView banner=tv("GAYAN GANGA\nCOACHING CENTER\n\nLive Classes  •  Mock Test  •  Notes  •  Experienced Teachers",19,Color.WHITE,true);
        banner.setGravity(Gravity.CENTER); banner.setBackground(bg(Color.rgb(210,55,55),8));
        content.addView(banner,new LinearLayout.LayoutParams(-1,dp(180)));

        GridLayout grid=new GridLayout(this); grid.setColumnCount(3); grid.setUseDefaultMargins(false);
        String[][] items={{"🎓","Paid Classes"},{"👩‍🏫","Free Courses"},{"📝","Free Weekly Test"},{"📚","Books"},{"📄","PDF Class Notes"},{"⏱","Paid Test Series"},{"📖","Syllabus\nPrevious Year"},{"💡","Create Test"},{"🔤","Daily Quiz"}};
        for(String[] x:items) grid.addView(card(x[0],x[1]));
        content.addView(grid,new LinearLayout.LayoutParams(-1,dp(480)));
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setBackground(bg(Color.WHITE,22)); nav.setPadding(dp(8),0,dp(8),0);
        String[] n={"⌂\nHome","▤\nCourses","▣\nTests","●\nProfile"};
        for(String s:n){ TextView b=tv(s,13,Color.DKGRAY,false); b.setGravity(Gravity.CENTER); nav.addView(b,new LinearLayout.LayoutParams(0,dp(64),1)); }
        nav.getChildAt(1).setOnClickListener(v -> coursesScreen());\n        nav.getChildAt(2).setOnClickListener(v -> testsScreen());\n        nav.getChildAt(3).setOnClickListener(v -> profileScreen(name));
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(70)));
        return root;
    }

    private void profileScreen(String name){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(255,248,248));
        TextView bar=tv("‹   Profile",20,Color.WHITE,true); bar.setPadding(dp(10),0,dp(10),0); bar.setBackgroundColor(Color.rgb(25,118,210));
        root.addView(bar,new LinearLayout.LayoutParams(-1,dp(60))); bar.setOnClickListener(v->loadHome());
        TextView info=tv("Name: "+name+"\n\nEmail: "+(auth.getCurrentUser()==null?"":auth.getCurrentUser().getEmail())+"\n\nAccount: Firebase Auth",18,Color.DKGRAY,false);
        info.setGravity(Gravity.TOP); info.setPadding(dp(24),dp(30),dp(24),dp(20)); root.addView(info,new LinearLayout.LayoutParams(-1,0,1));
        TextView logout=button("LOGOUT"); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54)); lp.setMargins(dp(24),0,dp(24),dp(24)); root.addView(logout,lp);
        logout.setOnClickListener(v -> { auth.signOut(); setContentView(authScreen(false)); });
        setContentView(root);
    }
}
