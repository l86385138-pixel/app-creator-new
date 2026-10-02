package com.gayangangacoachingcenter.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
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
        storage=FirebaseStorage.getInstance();
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
        c.setOnClickListener(v -> {
            if(title.equals("Paid Classes")) paidClassesScreen();
            else if(title.equals("Free Courses")) coursesScreen();
            else if(title.equals("Free Weekly Test") || title.equals("Paid Test Series") || title.equals("Daily Quiz")) testsScreen();
            else if(title.equals("Books") || title.equals("PDF Class Notes")) notesScreen();
            else showFeature(title);
        });
        return c;
    }

    private void paidClassesScreen(){
        LinearLayout root=baseScreen("Paid Classes");
        LinearLayout tabs=new LinearLayout(this); tabs.setPadding(dp(10),dp(8),dp(10),dp(8)); tabs.setGravity(Gravity.CENTER_VERTICAL);
        String[] tabNames={"My Courses","All Courses","RWA Railway Exams"};
        for(String tabName:tabNames){
            TextView tab=tv(tabName,15,tabName.equals("My Courses")?Color.WHITE:Color.DKGRAY,true);
            tab.setGravity(Gravity.CENTER); tab.setBackground(bg(tabName.equals("My Courses")?Color.rgb(25,118,210):Color.WHITE,10));
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(48),1); tp.setMargins(dp(4),0,dp(4),0); tabs.addView(tab,tp);
            if(tabName.equals("My Courses")) tab.setOnClickListener(v -> loadPaidCourses(root,"my"));
            if(tabName.equals("All Courses")) tab.setOnClickListener(v -> loadPaidCourses(root,"all"));
            if(tabName.equals("RWA Railway Exams")) tab.setOnClickListener(v -> loadPaidCourses(root,"railway"));
        }
        root.addView(tabs,new LinearLayout.LayoutParams(-1,dp(68)));
        loadPaidCourses(root,"my");
        setContentView(root);
    }

    private void loadPaidCourses(LinearLayout root,String filter){
        if(root.getChildCount()>2) root.removeViews(2,root.getChildCount()-2);
        TextView loading=tv("Courses loading...",16,Color.DKGRAY,false); loading.setPadding(dp(18),dp(15),dp(18),dp(15));
        root.addView(loading,new LinearLayout.LayoutParams(-1,0,1));
        com.google.firebase.firestore.Query query=db.collection("courses");
        if(filter.equals("railway")) query=query.whereEqualTo("category","railway");
        else if(filter.equals("my")){
            FirebaseUser u=auth.getCurrentUser();
            if(u!=null) query=query.whereArrayContains("enrolledUsers",u.getUid());
        }
        query.get().addOnSuccessListener(result -> {
            LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(12),dp(8),dp(12),dp(25));
            if(result.isEmpty()){
                TextView empty=tv(filter.equals("my")?"My Courses में अभी कोई enrolled course नहीं है.":"अभी कोई paid course उपलब्ध नहीं है.",17,Color.DKGRAY,false);
                empty.setGravity(Gravity.CENTER); list.addView(empty,new LinearLayout.LayoutParams(-1,dp(180)));
            } else for(QueryDocumentSnapshot doc:result) list.addView(paidCourseCard(doc));
            root.removeView(loading); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        }).addOnFailureListener(e -> loading.setText("Courses load नहीं हो सके.\n\n"+error(e)));
    }

    private LinearLayout paidCourseCard(QueryDocumentSnapshot doc){
        String title=doc.getString("title"); if(title==null) title=doc.getString("name"); if(title==null) title="Course";
        String description=doc.getString("description"); if(description==null) description="";
        String imageUrl=doc.getString("imageUrl");
        String price=doc.getString("price"); if(price==null && doc.get("price")!=null) price=String.valueOf(doc.get("price")); if(price==null) price="";
        final String courseTitle=title;
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(10),dp(14),dp(14)); card.setBackground(bg(Color.WHITE,14));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(390)); cp.setMargins(0,dp(7),0,dp(10)); card.setLayoutParams(cp);
        ImageView image=new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(image,new LinearLayout.LayoutParams(-1,dp(220)));
        if(imageUrl!=null && !imageUrl.isEmpty()) Glide.with(this).load(imageUrl).into(image);
        else image.setBackground(bg(Color.rgb(235,235,235),8));
        TextView t=tv(title,18,Color.rgb(45,45,45),false); t.setPadding(dp(5),dp(12),dp(5),dp(4)); card.addView(t,new LinearLayout.LayoutParams(-1,dp(55)));
        TextView d=tv(description,14,Color.DKGRAY,false); d.setPadding(dp(5),0,dp(5),0); card.addView(d,new LinearLayout.LayoutParams(-1,dp(45)));
        TextView view=button("VIEW"); LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,dp(50)); vp.setMargins(dp(5),dp(5),dp(5),0); card.addView(view,vp);
        view.setOnClickListener(v -> paidCourseDetail(doc.getId(),courseTitle));
        return card;
    }

    private void paidCourseDetail(String courseId,String title){
        LinearLayout root=baseScreen(title);
        LinearLayout tabs=new LinearLayout(this); tabs.setPadding(dp(10),dp(8),dp(10),dp(8));
        String[] names={"Subjects","Live","Telegram"};
        for(String n:names){
            TextView tab=tv(n,16,n.equals("Subjects")?Color.WHITE:Color.DKGRAY,true); tab.setGravity(Gravity.CENTER);
            tab.setBackground(bg(n.equals("Subjects")?Color.rgb(25,118,210):Color.WHITE,8));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1); p.setMargins(dp(4),0,dp(4),0); tabs.addView(tab,p);
            if(n.equals("Subjects")) tab.setOnClickListener(v -> loadSubjects(root,courseId));
            if(n.equals("Live")) tab.setOnClickListener(v -> loadLive(root,courseId));
            if(n.equals("Telegram")) tab.setOnClickListener(v -> loadTelegram(root,courseId));
        }
        root.addView(tabs,new LinearLayout.LayoutParams(-1,dp(66)));
        loadSubjects(root,courseId);
        setContentView(root);
    }

    private void loadSubjects(LinearLayout root,String courseId){
        replaceContent(root,"Subjects loading...");
        db.collection("courses").document(courseId).collection("subjects").get().addOnSuccessListener(result -> {
            LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(10),dp(5),dp(10),dp(20));
            if(result.isEmpty()) list.addView(centerMessage("अभी subjects add नहीं किए गए हैं."));
            else for(QueryDocumentSnapshot doc:result){
                String title=doc.getString("title"); if(title==null) title=doc.getString("name"); if(title==null) title="Subject";
                list.addView(subjectRow(courseId,doc.getId(),title,doc.getString("imageUrl")));
            }
            replaceContent(root,list);
        }).addOnFailureListener(e -> replaceContentText(root,"Subjects load नहीं हुए.\n\n"+error(e)));
    }

    private LinearLayout subjectRow(String courseId,String subjectId,String title,String imageUrl){
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(12),dp(8),dp(12),dp(8)); row.setBackgroundColor(Color.WHITE);
        ImageView icon=new ImageView(this); icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(icon,new LinearLayout.LayoutParams(dp(58),dp(58)));
        if(imageUrl!=null && !imageUrl.isEmpty()) Glide.with(this).load(imageUrl).into(icon); else icon.setBackground(bg(Color.LTGRAY,8));
        TextView t=tv(title,17,Color.rgb(35,35,35),true); t.setPadding(dp(16),0,dp(8),0); row.addView(t,new LinearLayout.LayoutParams(0,dp(74),1));
        TextView arrow=tv("›",30,Color.DKGRAY,false); row.addView(arrow,new LinearLayout.LayoutParams(dp(40),dp(74)));
        row.setOnClickListener(v -> subjectLessons(courseId,subjectId,title));
        return row;
    }

    private void subjectLessons(String courseId,String subjectId,String subjectTitle){
        LinearLayout root=baseScreen(subjectTitle);
        TextView loading=tv("Lessons loading...",16,Color.DKGRAY,false); loading.setPadding(dp(18),dp(18),dp(18),dp(18)); root.addView(loading,new LinearLayout.LayoutParams(-1,-1)); setContentView(root);
        db.collection("courses").document(courseId).collection("subjects").document(subjectId).collection("lessons").get().addOnSuccessListener(result -> {
            LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(12),dp(8),dp(12),dp(20));
            if(result.isEmpty()) list.addView(centerMessage("अभी lessons उपलब्ध नहीं हैं."));
            else for(QueryDocumentSnapshot doc:result){
                String title=doc.getString("title"); if(title==null) title="Lesson";
                String videoUrl=doc.getString("videoUrl");
                TextView item=tv("▶  "+title+"\n",17,Color.rgb(35,35,35),true); item.setPadding(dp(15),dp(16),dp(15),dp(16)); item.setBackground(bg(Color.WHITE,10));
                item.setOnClickListener(v -> openUrl(videoUrl)); list.addView(item,new LinearLayout.LayoutParams(-1,dp(75)));
            }
            root.removeView(loading); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        }).addOnFailureListener(e -> loading.setText("Lessons load नहीं हुए.\n\n"+error(e)));
    }

    private void loadLive(LinearLayout root,String courseId){
        replaceContent(root,"Live classes loading...");
        db.collection("courses").document(courseId).get().addOnSuccessListener(doc -> {
            String url=doc.getString("liveUrl");
            LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(25),dp(25),dp(25),dp(25));
            box.addView(centerMessage("Live class उपलब्ध होने पर नीचे से open करें."));
            TextView b=button("OPEN LIVE CLASS"); box.addView(b,new LinearLayout.LayoutParams(-1,dp(54))); b.setOnClickListener(v -> openUrl(url));
            replaceContent(root,box);
        });
    }

    private void loadTelegram(LinearLayout root,String courseId){
        replaceContent(root,"Telegram loading...");
        db.collection("courses").document(courseId).get().addOnSuccessListener(doc -> {
            String url=doc.getString("telegramUrl");
            LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(25),dp(25),dp(25),dp(25));
            box.addView(centerMessage("Course का Telegram group/channel नीचे से खोलें."));
            TextView b=button("OPEN TELEGRAM"); box.addView(b,new LinearLayout.LayoutParams(-1,dp(54))); b.setOnClickListener(v -> openUrl(url));
            replaceContent(root,box);
        });
    }

    private void replaceContent(LinearLayout root,String text){
        if(root.getChildCount()>2) root.removeViews(2,root.getChildCount()-2);
        TextView t=tv(text,16,Color.DKGRAY,false); t.setPadding(dp(18),dp(15),dp(18),dp(15)); root.addView(t,new LinearLayout.LayoutParams(-1,0,1));
    }

    private void replaceContentText(LinearLayout root,String text){
        if(root.getChildCount()>2) root.removeViews(2,root.getChildCount()-2);
        TextView t=tv(text,16,Color.DKGRAY,false); t.setPadding(dp(18),dp(15),dp(18),dp(15)); root.addView(t,new LinearLayout.LayoutParams(-1,0,1));
    }

    private void replaceContent(LinearLayout root,LinearLayout content){
        if(root.getChildCount()>2) root.removeViews(2,root.getChildCount()-2);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
    }

    private TextView centerMessage(String text){
        TextView t=tv(text,17,Color.DKGRAY,false); t.setGravity(Gravity.CENTER); return t;
    }

    private void openUrl(String url){
        if(url==null || url.trim().isEmpty()){ Toast.makeText(this,"Link उपलब्ध नहीं है.",Toast.LENGTH_LONG).show(); return; }
        try{ startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url))); }
        catch(Exception e){ Toast.makeText(this,"Link open नहीं हो सका.",Toast.LENGTH_LONG).show(); }
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

    private void notesScreen(){
        LinearLayout root=baseScreen("Books & PDF Notes");
        TextView loading=tv("Study material loading from Firebase...",17,Color.DKGRAY,false);
        loading.setPadding(dp(20),dp(20),dp(20),dp(20));
        root.addView(loading,new LinearLayout.LayoutParams(-1,-1));
        setContentView(root);
        db.collection("notes").get().addOnSuccessListener(result -> {
            LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(14),dp(10),dp(14),dp(20));
            if(result.isEmpty()){
                TextView empty=tv("अभी कोई PDF/Notes उपलब्ध नहीं है.\\n\\nFirestore → notes collection में material जोड़ें.",17,Color.DKGRAY,false);
                empty.setGravity(Gravity.CENTER); list.addView(empty,new LinearLayout.LayoutParams(-1,dp(180)));
            } else {
                for(QueryDocumentSnapshot doc:result){
                    String title=doc.getString("title"); if(title==null) title="Study Material";
                    String description=doc.getString("description"); if(description==null) description="";
                    String path=doc.getString("storagePath");
                    list.addView(noteCard(title,description,path));
                }
            }
            root.removeViews(1,root.getChildCount()-1);
            root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        }).addOnFailureListener(e -> loading.setText("Notes load नहीं हो सके.\\n\\n"+error(e)));
    }

    private LinearLayout noteCard(String title,String description,String storagePath){
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(14),dp(16),dp(12)); card.setBackground(bg(Color.WHITE,12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(145)); lp.setMargins(0,dp(7),0,dp(7)); card.setLayoutParams(lp);
        TextView t=tv("📄  "+title,18,Color.rgb(25,118,210),true); card.addView(t,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView d=tv(description,14,Color.DKGRAY,false); d.setGravity(Gravity.TOP); card.addView(d,new LinearLayout.LayoutParams(-1,0,1));
        TextView open=button("OPEN PDF / NOTES"); card.addView(open,new LinearLayout.LayoutParams(-1,dp(42)));
        open.setOnClickListener(v -> openMaterial(storagePath));
        return card;
    }

    private void openMaterial(String storagePath){
        if(storagePath==null || storagePath.trim().isEmpty()){
            Toast.makeText(this,"इस material का Storage path नहीं मिला.",Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this,"PDF link तैयार हो रहा है...",Toast.LENGTH_SHORT).show();
        storage.getReference().child(storagePath).getDownloadUrl().addOnSuccessListener(uri -> {
            try{
                android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_VIEW,uri);
                startActivity(intent);
            }catch(Exception e){
                Toast.makeText(this,"PDF खोलने के लिए कोई viewer उपलब्ध नहीं है.",Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this,"PDF नहीं खुल सका. Firebase Storage rules/path check करें.",Toast.LENGTH_LONG).show());
    }

    private void testsScreen(){
        LinearLayout root=baseScreen("Tests & Quiz");
        TextView loading=tv("Tests loading from Firebase...",17,Color.DKGRAY,false);
        loading.setPadding(dp(20),dp(20),dp(20),dp(20));
        root.addView(loading,new LinearLayout.LayoutParams(-1,-1));
        setContentView(root);
        db.collection("tests").get().addOnSuccessListener(result -> {
            LinearLayout list=new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(14),dp(10),dp(14),dp(20));
            if(result.isEmpty()){
                TextView empty=tv("अभी कोई test उपलब्ध नहीं है.\n\nFirestore → tests collection में test जोड़ें.",17,Color.DKGRAY,false);
                empty.setGravity(Gravity.CENTER);
                list.addView(empty,new LinearLayout.LayoutParams(-1,dp(180)));
            } else {
                for(QueryDocumentSnapshot doc:result){
                    String title=doc.getString("title");
                    if(title==null) title=doc.getString("name");
                    if(title==null) title="Test";
                    String description=doc.getString("description");
                    if(description==null) description="";
                    String duration=doc.getString("duration");
                    if(duration==null) duration="";
                    list.addView(testCard(doc.getId(),title,description,duration));
                }
            }
            root.removeViews(1,root.getChildCount()-1);
            root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        }).addOnFailureListener(e -> loading.setText("Tests load नहीं हो सके.\n\n"+error(e)));
    }

    private LinearLayout testCard(String id,String title,String description,String duration){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(14),dp(16),dp(12));
        card.setBackground(bg(Color.WHITE,12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(150));
        lp.setMargins(0,dp(7),0,dp(7));
        card.setLayoutParams(lp);

        TextView t=tv(title,19,Color.rgb(25,118,210),true);
        card.addView(t,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView d=tv(description,14,Color.DKGRAY,false);
        d.setGravity(Gravity.TOP);
        card.addView(d,new LinearLayout.LayoutParams(-1,0,1));
        TextView info=tv(duration.isEmpty()?"START TEST":"Duration: "+duration,15,Color.rgb(210,55,55),true);
        info.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(info,new LinearLayout.LayoutParams(-1,dp(34)));
        card.setOnClickListener(v -> startTest(id,title));
        return card;
    }

    private void startTest(String testId,String testTitle){
        db.collection("tests").document(testId).collection("questions").get()
            .addOnSuccessListener(result -> {
                if(result.isEmpty()){
                    Toast.makeText(this,"इस test में अभी questions नहीं हैं.",Toast.LENGTH_LONG).show();
                    return;
                }
                java.util.ArrayList<com.google.firebase.firestore.DocumentSnapshot> questions=new java.util.ArrayList<>();
                for(com.google.firebase.firestore.DocumentSnapshot doc:result) questions.add(doc);
                showQuestion(testId,testTitle,questions,0,new int[]{0},new int[]{-1});
            })
            .addOnFailureListener(e -> Toast.makeText(this,"Questions load नहीं हुए: "+error(e),Toast.LENGTH_LONG).show());
    }

    private void showQuestion(String testId,String testTitle,
                              java.util.ArrayList<com.google.firebase.firestore.DocumentSnapshot> questions,
                              int index,int[] score,int[] selected){
        LinearLayout root=baseScreen(testTitle);
        ScrollView scroll=new ScrollView(this);
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18),dp(16),dp(18),dp(24));

        TextView progress=tv("Question "+(index+1)+" / "+questions.size(),15,Color.DKGRAY,true);
        body.addView(progress,new LinearLayout.LayoutParams(-1,dp(40)));

        com.google.firebase.firestore.DocumentSnapshot q=questions.get(index);
        String question=q.getString("question");
        if(question==null) question=q.getString("text");
        if(question==null) question="Question";

        TextView qt=tv(question,20,Color.rgb(35,35,35),true);
        qt.setGravity(Gravity.TOP);
        body.addView(qt,new LinearLayout.LayoutParams(-1,dp(95)));

        RadioGroup options=new RadioGroup(this);
        options.setOrientation(RadioGroup.VERTICAL);

        String[] keys={"option1","option2","option3","option4"};
        for(String key:keys){
            String value=q.getString(key);
            if(value==null) value="";
            if(value.trim().isEmpty()) continue;
            RadioButton rb=new RadioButton(this);
            rb.setText(value);
            rb.setTextSize(17);
            rb.setPadding(dp(8),dp(10),dp(8),dp(10));
            options.addView(rb,new RadioGroup.LayoutParams(-1,dp(58)));
        }

        body.addView(options,new LinearLayout.LayoutParams(-1,dp(245)));

        TextView action=button(index==questions.size()-1?"SUBMIT TEST":"NEXT QUESTION");
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(55));
        ap.topMargin=dp(18);
        body.addView(action,ap);

        if(selected[0]>=0){
            int child=selected[0];
            if(child<options.getChildCount()) ((RadioButton)options.getChildAt(child)).setChecked(true);
        }

        action.setOnClickListener(v -> {
            int checked=options.getCheckedRadioButtonId();
            if(checked==-1){
                Toast.makeText(this,"पहले एक option select करें.",Toast.LENGTH_SHORT).show();
                return;
            }
            View checkedView=options.findViewById(checked);
            int selectedIndex=options.indexOfChild(checkedView);
            String answer=q.getString("answer");
            if(answer==null) answer=q.getString("correctAnswer");
            String correctText="";
            if(answer!=null){
                String[] answerKeys={"option1","option2","option3","option4"};
                if(answer.matches("[1-4]")) correctText=q.getString(answerKeys[Integer.parseInt(answer)-1]);
                else correctText=answer;
            }
            if(correctText!=null && !correctText.isEmpty() &&
               ((RadioButton)checkedView).getText().toString().trim().equalsIgnoreCase(correctText.trim())){
                score[0]++;
            }
            selected[0]=selectedIndex;
            if(index<questions.size()-1){
                showQuestion(testId,testTitle,questions,index+1,score,new int[]{-1});
            } else {
                saveTestResult(testId,testTitle,questions.size(),score[0]);
            }
        });

        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private void saveTestResult(String testId,String testTitle,int total,int score){
        FirebaseUser u=auth.getCurrentUser();
        if(u==null){ setContentView(authScreen(false)); return; }
        Map<String,Object> result=new HashMap<>();
        result.put("testId",testId);
        result.put("testTitle",testTitle);
        result.put("totalQuestions",total);
        result.put("score",score);
        result.put("percentage",total==0?0:(score*100.0/total));
        result.put("submittedAt",com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("users").document(u.getUid()).collection("testResults").add(result)
            .addOnSuccessListener(ref -> showScore(testTitle,total,score))
            .addOnFailureListener(e -> {
                Toast.makeText(this,"Score save नहीं हुआ: "+error(e),Toast.LENGTH_LONG).show();
                showScore(testTitle,total,score);
            });
    }

    private void showScore(String testTitle,int total,int score){
        LinearLayout root=baseScreen("Test Result");
        double pct=total==0?0:(score*100.0/total);
        TextView result=tv("🎉 Test Completed!\n\n"+testTitle+"\n\nScore: "+score+" / "+total+
                "\nPercentage: "+String.format(java.util.Locale.US,"%.1f",pct)+"%"+
                "\n\nYour result has been saved to Firebase.",22,Color.rgb(35,35,35),true);
        result.setGravity(Gravity.CENTER);
        root.addView(result,new LinearLayout.LayoutParams(-1,0,1));
        TextView home=button("BACK TO HOME");
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(55));
        hp.setMargins(dp(20),0,dp(20),dp(20));
        root.addView(home,hp);
        home.setOnClickListener(v->loadHome());
        setContentView(root);
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
        nav.getChildAt(1).setOnClickListener(v -> coursesScreen());
        nav.getChildAt(2).setOnClickListener(v -> testsScreen());
        nav.getChildAt(3).setOnClickListener(v -> profileScreen(name));
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
