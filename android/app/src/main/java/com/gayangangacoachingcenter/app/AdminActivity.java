package com.gayangangacoachingcenter.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private EditText input(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setTextSize(15); e.setSingleLine(true); e.setPadding(dp(14),0,dp(14),0); e.setBackgroundColor(Color.WHITE); return e; }
    private TextView tv(String s,float z,boolean b){ TextView t=new TextView(this); t.setText(s);t.setTextSize(z);t.setTextColor(Color.DKGRAY);t.setGravity(Gravity.CENTER_VERTICAL);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t; }
    private TextView button(String s){TextView b=tv(s,16,true);b.setTextColor(Color.WHITE);b.setGravity(Gravity.CENTER);b.setBackgroundColor(Color.rgb(25,118,210));return b;}
    private void add(LinearLayout p,View v,int h){LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(-1,dp(h));x.setMargins(0,dp(6),0,dp(6));p.addView(v,x);}
    @Override public void onCreate(Bundle b){
        super.onCreate(b); auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();storage=FirebaseStorage.getInstance();
        verifyAdmin();
    }
    private void verifyAdmin(){
        if(auth.getCurrentUser()==null){Toast.makeText(this,"Admin login required",Toast.LENGTH_LONG).show();finish();return;}
        db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(d->{
            if(!d.exists() || !"admin".equals(d.getString("role"))){Toast.makeText(this,"Admin access denied",Toast.LENGTH_LONG).show();finish();}
            else setContentView(panel());
        });
    }
    private LinearLayout panel(){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(20),dp(18),dp(25));r.setBackgroundColor(Color.rgb(247,249,252));
        TextView h=tv("ADMIN PANEL",25,true);h.setTextColor(Color.rgb(25,118,210));add(r,h,55);
        TextView info=tv("Courses • Subjects • Videos • PDFs • Live • Telegram • Payment",14,false);add(r,info,45);
        EditText course=input("Course title"); EditText desc=input("Course description"); EditText price=input("Price (example 1499)"); EditText image=input("Course image URL (optional)"); EditText category=input("Category (railway / banking / other)"); EditText payment=input("PAYMENT GATEWAY LINK");
        add(r,course,52);add(r,desc,52);add(r,price,52);add(r,image,52);add(r,category,52);add(r,payment,52);
        TextView save=button("CREATE / UPDATE COURSE");add(r,save,55);
        save.setOnClickListener(v->{String title=course.getText().toString().trim();if(title.isEmpty()){Toast.makeText(this,"Course title required",Toast.LENGTH_SHORT).show();return;}Map<String,Object> m=new HashMap<>();m.put("title",title);m.put("description",desc.getText().toString().trim());m.put("price",price.getText().toString().trim());m.put("imageUrl",image.getText().toString().trim());m.put("category",category.getText().toString().trim());m.put("paymentUrl",payment.getText().toString().trim());m.put("updatedAt",FieldValue.serverTimestamp());m.put("enrolledUsers",new java.util.ArrayList<String>());db.collection("courses").add(m).addOnSuccessListener(x->Toast.makeText(this,"Course created: "+x.getId(),Toast.LENGTH_LONG).show()).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());});
        EditText cid=input("Course ID");EditText subject=input("Subject name");EditText lesson=input("Lesson title");EditText video=input("Recorded video URL");EditText live=input("Live class URL");EditText telegram=input("Telegram URL");
        add(r,cid,52);add(r,subject,52);add(r,lesson,52);add(r,video,52);add(r,live,52);add(r,telegram,52);
        TextView content=button("SAVE SUBJECT / LESSON / LIVE / TELEGRAM");add(r,content,55);
        content.setOnClickListener(v->{String id=cid.getText().toString().trim();if(id.isEmpty()||subject.getText().toString().trim().isEmpty()){Toast.makeText(this,"Course ID and Subject required",Toast.LENGTH_SHORT).show();return;}Map<String,Object> sm=new HashMap<>();sm.put("title",subject.getText().toString().trim());db.collection("courses").document(id).collection("subjects").add(sm).addOnSuccessListener(s->{if(!lesson.getText().toString().trim().isEmpty()){Map<String,Object> lm=new HashMap<>();lm.put("title",lesson.getText().toString().trim());lm.put("videoUrl",video.getText().toString().trim());db.collection("courses").document(id).collection("subjects").document(s.getId()).collection("lessons").add(lm);}Map<String,Object> cm=new HashMap<>();cm.put("liveUrl",live.getText().toString().trim());cm.put("telegramUrl",telegram.getText().toString().trim());db.collection("courses").document(id).set(cm,com.google.firebase.firestore.SetOptions.merge());Toast.makeText(this,"Content saved",Toast.LENGTH_LONG).show();});});
        TextView upload=button("UPLOAD PDF / VIDEO / FILE");add(r,upload,55);upload.setOnClickListener(v->chooseFile());
        TextView logout=button("ADMIN LOGOUT");add(r,logout,55);logout.setOnClickListener(v->{auth.signOut();finish();});
        ScrollViewWrap(r);
        return r;
    }
    private void ScrollViewWrap(LinearLayout r){ /* layout is intentionally simple; Android parent can scroll when hosted in ScrollView in next UI revision */ }
    private void chooseFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,701);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode!=701||resultCode!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;String name=uri.getLastPathSegment();if(name==null)name="file";String path="admin_uploads/"+auth.getCurrentUser().getUid()+"/"+System.currentTimeMillis()+"_"+name;StorageReference ref=storage.getReference().child(path);ref.putFile(uri).addOnSuccessListener(t->ref.getDownloadUrl().addOnSuccessListener(url->{Map<String,Object> m=new HashMap<>();m.put("name",name);m.put("storagePath",path);m.put("downloadUrl",url.toString());m.put("uploadedBy",auth.getCurrentUser().getUid());m.put("uploadedAt",FieldValue.serverTimestamp());db.collection("adminUploads").add(m);Toast.makeText(this,"File uploaded successfully",Toast.LENGTH_LONG).show();})).addOnFailureListener(e->Toast.makeText(this,"Upload failed: "+e.getMessage(),Toast.LENGTH_LONG).show());}
}
