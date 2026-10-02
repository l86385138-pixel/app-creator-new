package com.gayangangacoachingcenter.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private EditText courseIdInput, subjectIdInput, testIdInput;
    private EditText noteTitleInput, noteDescriptionInput, noteCourseIdInput;
    private Uri pendingUri;
    private int pendingType = 0;
    private static final int PICK_VIDEO = 701;
    private static final int PICK_NOTE = 702;
    private static final int PICK_GENERIC = 703;

    private int dp(float v){ return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView tv(String s,float z,boolean b){
        TextView t=new TextView(this);
        t.setText(s); t.setTextSize(z); t.setTextColor(Color.DKGRAY);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    private TextView button(String s){
        TextView b=tv(s,15,true);
        b.setTextColor(Color.WHITE); b.setGravity(Gravity.CENTER);
        b.setBackgroundColor(Color.rgb(25,118,210));
        return b;
    }

    private EditText input(String hint){
        EditText e=new EditText(this);
        e.setHint(hint); e.setTextSize(15); e.setSingleLine(true);
        e.setPadding(dp(14),0,dp(14),0);
        e.setBackgroundColor(Color.WHITE);
        return e;
    }

    private void add(LinearLayout p,View v,int h){
        LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(-1,dp(h));
        x.setMargins(0,dp(5),0,dp(5)); p.addView(v,x);
    }

    private void addSectionTitle(LinearLayout p,String title){
        TextView t=tv(title,20,true);
        t.setTextColor(Color.rgb(25,118,210));
        t.setPadding(dp(4),dp(18),dp(4),dp(6));
        add(p,t,55);
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        storage=FirebaseStorage.getInstance();
        verifyAdmin();
    }

    private void verifyAdmin(){
        if(auth.getCurrentUser()==null){
            Toast.makeText(this,"Admin login required",Toast.LENGTH_LONG).show();
            finish(); return;
        }
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
            .addOnSuccessListener(d -> {
                if(!d.exists() || !"admin".equals(d.getString("role"))){
                    Toast.makeText(this,"Admin access denied",Toast.LENGTH_LONG).show();
                    finish();
                } else setContentView(panel());
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this,"Admin verification failed: "+error(e),Toast.LENGTH_LONG).show();
                finish();
            });
    }

    private LinearLayout panel(){
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.rgb(247,249,252));

        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18),dp(16),dp(18),dp(8));
        TextView h=tv("ADMIN CONTROL PANEL",24,true);
        h.setTextColor(Color.rgb(25,118,210));
        header.addView(h,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView info=tv("Free System • Course • Subject • Video • Live • PDF • Test",13,false);
        header.addView(info,new LinearLayout.LayoutParams(-1,dp(38)));
        page.addView(header,new LinearLayout.LayoutParams(-1,dp(95)));

        ScrollView scroll=new ScrollView(this);
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(18),0,dp(18),dp(25));
        scroll.addView(r);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        buildCourseSection(r);
        buildSubjectSection(r);
        buildVideoSection(r);
        buildLiveSection(r);
        buildNotesSection(r);
        buildTestSection(r);
        buildQuestionSection(r);
        buildStudentsSection(r);
        buildResultsSection(r);
        buildGenericUploadSection(r);

        TextView logout=button("ADMIN LOGOUT");
        add(r,logout,55);
        logout.setOnClickListener(v->{ auth.signOut(); finish(); });

        return page;
    }

    private void buildCourseSection(LinearLayout r){
        addSectionTitle(r,"1. FREE COURSE");
        EditText title=input("Course title");
        EditText desc=input("Course description");
        EditText category=input("Category (example: railway)");
        add(r,title,52); add(r,desc,52); add(r,category,52);

        TextView save=button("CREATE FREE COURSE");
        add(r,save,54);
        save.setOnClickListener(v -> {
            String t=title.getText().toString().trim();
            if(t.isEmpty()){ toast("Course title required"); return; }
            Map<String,Object> m=new HashMap<>();
            m.put("title",t);
            m.put("description",desc.getText().toString().trim());
            m.put("category",category.getText().toString().trim());
            m.put("access","free");
            m.put("price","0");
            m.put("paymentUrl","");
            m.put("enrolledUsers",new ArrayList<String>());
            m.put("createdAt",FieldValue.serverTimestamp());
            m.put("updatedAt",FieldValue.serverTimestamp());
            save.setText("SAVING...");
            db.collection("courses").add(m).addOnSuccessListener(x -> {
                courseIdInput.setText(x.getId());
                save.setText("CREATE FREE COURSE");
                toast("Course created. Course ID: "+x.getId());
            }).addOnFailureListener(e -> {
                save.setText("CREATE FREE COURSE");
                toast("Course create failed: "+error(e));
            });
        });
        TextView help=tv("Course ID ऊपर बनने के बाद नीचे के sections में वही ID डालें.",13,false);
        add(r,help,38);
    }

    private void buildSubjectSection(LinearLayout r){
        addSectionTitle(r,"2. SUBJECT");
        courseIdInput=input("Course ID");
        EditText subject=input("Subject name");
        add(r,courseIdInput,52); add(r,subject,52);
        TextView save=button("ADD SUBJECT");
        add(r,save,54);
        save.setOnClickListener(v -> {
            String cid=courseIdInput.getText().toString().trim();
            String st=subject.getText().toString().trim();
            if(cid.isEmpty()||st.isEmpty()){toast("Course ID और Subject name भरें");return;}
            Map<String,Object> m=new HashMap<>();
            m.put("title",st); m.put("createdAt",FieldValue.serverTimestamp());
            db.collection("courses").document(cid).collection("subjects").add(m)
                .addOnSuccessListener(x -> {
                    subjectIdInput.setText(x.getId());
                    toast("Subject added. Subject ID: "+x.getId());
                })
                .addOnFailureListener(e->toast("Subject add failed: "+error(e)));
        });
        TextView note=tv("Subject ID बनने के बाद Recorded Video में वही ID इस्तेमाल करें.",13,false);
        add(r,note,38);
    }

    private void buildVideoSection(LinearLayout r){
        addSectionTitle(r,"3. RECORDED VIDEO CLASS");
        subjectIdInput=input("Subject ID");
        EditText lesson=input("Video/Lesson title");
        EditText videoUrl=input("YouTube / video URL (optional)");
        add(r,subjectIdInput,52); add(r,lesson,52); add(r,videoUrl,52);

        TextView upload=button("UPLOAD VIDEO FILE");
        add(r,upload,54);
        upload.setOnClickListener(v -> {
            if(subjectIdInput.getText().toString().trim().isEmpty() || lesson.getText().toString().trim().isEmpty()){
                toast("Subject ID और Video title पहले भरें"); return;
            }
            pendingType=1;
            chooseFile(PICK_VIDEO,"video/*");
        });

        TextView saveUrl=button("SAVE VIDEO URL");
        add(r,saveUrl,54);
        saveUrl.setOnClickListener(v -> {
            String sid=subjectIdInput.getText().toString().trim();
            String lt=lesson.getText().toString().trim();
            String vu=videoUrl.getText().toString().trim();
            if(sid.isEmpty()||lt.isEmpty()||vu.isEmpty()){toast("Subject ID, title और video URL भरें");return;}
            saveLesson(sid,lt,vu,null);
        });
    }

    private void buildLiveSection(LinearLayout r){
        addSectionTitle(r,"4. LIVE CLASS");
        EditText cid=input("Course ID");
        EditText liveTitle=input("Live class title");
        EditText liveUrl=input("Google Meet / YouTube Live / Zoom link");
        EditText schedule=input("Schedule (example: Today 7:00 PM)");
        add(r,cid,52); add(r,liveTitle,52); add(r,liveUrl,52); add(r,schedule,52);
        TextView start=button("START / UPDATE LIVE CLASS");
        add(r,start,54);
        start.setOnClickListener(v -> {
            String id=cid.getText().toString().trim();
            if(id.isEmpty()||liveUrl.getText().toString().trim().isEmpty()){toast("Course ID और Live link भरें");return;}
            Map<String,Object> m=new HashMap<>();
            m.put("liveTitle",liveTitle.getText().toString().trim());
            m.put("liveUrl",liveUrl.getText().toString().trim());
            m.put("liveSchedule",schedule.getText().toString().trim());
            m.put("liveActive",true);
            m.put("updatedAt",FieldValue.serverTimestamp());
            db.collection("courses").document(id).set(m,com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(x->toast("Live class started/updated"))
                .addOnFailureListener(e->toast("Live update failed: "+error(e)));
        });
        TextView stop=button("STOP LIVE CLASS");
        add(r,stop,54);
        stop.setOnClickListener(v -> {
            String id=cid.getText().toString().trim();
            if(id.isEmpty()){toast("Course ID भरें");return;}
            Map<String,Object> m=new HashMap<>();
            m.put("liveActive",false);
            m.put("updatedAt",FieldValue.serverTimestamp());
            db.collection("courses").document(id).set(m,com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(x->toast("Live class stopped"))
                .addOnFailureListener(e->toast("Update failed: "+error(e)));
        });
    }

    private void buildNotesSection(LinearLayout r){
        addSectionTitle(r,"5. PDF / NOTES");
        noteCourseIdInput=input("Course ID (optional)");
        noteTitleInput=input("PDF / Notes title");
        noteDescriptionInput=input("Description");
        add(r,noteCourseIdInput,52); add(r,noteTitleInput,52); add(r,noteDescriptionInput,52);
        TextView upload=button("SELECT & UPLOAD PDF / NOTES");
        add(r,upload,54);
        upload.setOnClickListener(v -> {
            if(noteTitleInput.getText().toString().trim().isEmpty()){toast("Notes title भरें");return;}
            pendingType=2;
            chooseFile(PICK_NOTE,"application/pdf");
        });
    }

    private void buildTestSection(LinearLayout r){
        addSectionTitle(r,"6. FREE TEST");
        EditText title=input("Test title");
        EditText description=input("Test description");
        EditText cid=input("Course ID (optional)");
        add(r,title,52); add(r,description,52); add(r,cid,52);
        TextView create=button("CREATE FREE TEST");
        add(r,create,54);
        create.setOnClickListener(v -> {
            String t=title.getText().toString().trim();
            if(t.isEmpty()){toast("Test title required");return;}
            Map<String,Object> m=new HashMap<>();
            m.put("title",t);
            m.put("description",description.getText().toString().trim());
            m.put("courseId",cid.getText().toString().trim());
            m.put("access","free");
            m.put("createdAt",FieldValue.serverTimestamp());
            db.collection("tests").add(m).addOnSuccessListener(x->{
                testIdInput.setText(x.getId());
                toast("Test created. Test ID: "+x.getId());
            }).addOnFailureListener(e->toast("Test create failed: "+error(e)));
        });
    }

    private void buildQuestionSection(LinearLayout r){
        addSectionTitle(r,"7. TEST QUESTIONS");
        testIdInput=input("Test ID");
        EditText q=input("Question");
        EditText o1=input("Option 1");
        EditText o2=input("Option 2");
        EditText o3=input("Option 3");
        EditText o4=input("Option 4");
        EditText ans=input("Correct option number: 1 / 2 / 3 / 4");
        add(r,testIdInput,52); add(r,q,52); add(r,o1,52); add(r,o2,52); add(r,o3,52); add(r,o4,52); add(r,ans,52);
        TextView addQ=button("ADD QUESTION");
        add(r,addQ,54);
        addQ.setOnClickListener(v -> {
            String tid=testIdInput.getText().toString().trim();
            String qq=q.getText().toString().trim();
            String a=ans.getText().toString().trim();
            if(tid.isEmpty()||qq.isEmpty()||a.isEmpty()){toast("Test ID, Question और correct option भरें");return;}
            Map<String,Object> m=new HashMap<>();
            m.put("question",qq);
            m.put("option1",o1.getText().toString().trim());
            m.put("option2",o2.getText().toString().trim());
            m.put("option3",o3.getText().toString().trim());
            m.put("option4",o4.getText().toString().trim());
            m.put("answer",a);
            m.put("correctAnswer",a);
            m.put("createdAt",FieldValue.serverTimestamp());
            db.collection("tests").document(tid).collection("questions").add(m)
                .addOnSuccessListener(x->{
                    q.setText(""); o1.setText(""); o2.setText(""); o3.setText(""); o4.setText(""); ans.setText("");
                    toast("Question added");
                }).addOnFailureListener(e->toast("Question add failed: "+error(e)));
        });
    }

    private void buildStudentsSection(LinearLayout r){
        addSectionTitle(r,"8. STUDENTS");
        TextView load=button("VIEW STUDENTS");
        add(r,load,54);
        load.setOnClickListener(v->{
            db.collection("users").get().addOnSuccessListener(result->{
                StringBuilder s=new StringBuilder("STUDENTS\n\n");
                int count=0;
                for(QueryDocumentSnapshot d:result){
                    if("admin".equals(d.getString("role"))) continue;
                    count++;
                    String name=d.getString("name"); if(name==null) name="Student";
                    String email=d.getString("email"); if(email==null) email="";
                    s.append(count).append(". ").append(name).append("\n").append(email).append("\n\n");
                }
                showText("Student List",s.length()>10?s.toString():"No students found");
            }).addOnFailureListener(e->toast("Students load failed: "+error(e)));
        });
    }

    private void buildResultsSection(LinearLayout r){
        addSectionTitle(r,"9. TEST RESULTS");
        TextView load=button("VIEW TEST RESULTS");
        add(r,load,54);
        load.setOnClickListener(v->{
            db.collection("users").get().addOnSuccessListener(users->{
                StringBuilder s=new StringBuilder("TEST RESULTS\n\n");
                final int[] pending={users.size()};
                final ArrayList<String> rows=new ArrayList<>();
                if(users.isEmpty()){showText("Results","No students found");return;}
                for(DocumentSnapshot u:users){
                    u.getReference().collection("testResults").get().addOnSuccessListener(res->{
                        for(QueryDocumentSnapshot x:res){
                            String email=u.getString("email"); if(email==null) email="";
                            String test=x.getString("testTitle"); if(test==null) test=x.getString("testId");
                            Object score=x.get("score"); Object total=x.get("total");
                            rows.add(email+"\n"+test+"\nScore: "+String.valueOf(score)+"/"+String.valueOf(total)+"\n");
                        }
                        pending[0]--;
                        if(pending[0]<=0){
                            StringBuilder out=new StringBuilder("TEST RESULTS\n\n");
                            for(String row:rows) out.append(row).append("\n");
                            showText("Test Results",out.length()>15?out.toString():"No test results found");
                        }
                    }).addOnFailureListener(e->{pending[0]--; if(pending[0]<=0)showText("Test Results",rows.isEmpty()?"No test results found":rows.toString());});
                }
            }).addOnFailureListener(e->toast("Results load failed: "+error(e)));
        });
    }

    private void buildGenericUploadSection(LinearLayout r){
        addSectionTitle(r,"10. OTHER FILES");
        TextView b=button("UPLOAD ANY FILE");
        add(r,b,54);
        b.setOnClickListener(v->{pendingType=3;chooseFile(PICK_GENERIC,"*/*");});
        TextView info=tv("Videos/PDF/Images/other files Firebase Storage में सुरक्षित होंगे.",13,false);
        add(r,info,40);
    }

    private void saveLesson(String subjectId,String title,String url,String storagePath){
        // Find the subject's parent course by checking the currently entered course ID.
        String cid=courseIdInput==null?"":courseIdInput.getText().toString().trim();
        if(cid.isEmpty()){toast("Course ID field में course ID भरें");return;}
        Map<String,Object> m=new HashMap<>();
        m.put("title",title);
        m.put("videoUrl",url);
        if(storagePath!=null)m.put("storagePath",storagePath);
        m.put("type","recorded");
        m.put("createdAt",FieldValue.serverTimestamp());
        db.collection("courses").document(cid).collection("subjects").document(subjectId).collection("lessons").add(m)
            .addOnSuccessListener(x->toast("Recorded video saved"))
            .addOnFailureListener(e->toast("Video save failed: "+error(e)));
    }

    private void chooseFile(int requestCode,String type){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType(type); i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,requestCode);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null)return;
        Uri uri=data.getData(); if(uri==null)return;
        pendingUri=uri;
        if(requestCode==PICK_VIDEO) uploadSelectedVideo(uri);
        else if(requestCode==PICK_NOTE) uploadSelectedNote(uri);
        else if(requestCode==PICK_GENERIC) uploadGeneric(uri);
    }

    private void uploadSelectedVideo(Uri uri){
        String sid=subjectIdInput.getText().toString().trim();
        String title=getFieldTextAroundVideo();
        String cid=courseIdInput.getText().toString().trim();
        if(sid.isEmpty()||title.isEmpty()||cid.isEmpty()){toast("Course ID, Subject ID और Video title भरें");return;}
        String name=safeName(uri);
        String path="course_videos/"+auth.getCurrentUser().getUid()+"/"+System.currentTimeMillis()+"_"+name;
        StorageReference ref=storage.getReference().child(path);
        Toast.makeText(this,"Video upload शुरू हो रहा है...",Toast.LENGTH_LONG).show();
        ref.putFile(uri).addOnSuccessListener(task->ref.getDownloadUrl().addOnSuccessListener(url->saveLesson(sid,title,url.toString(),path)))
            .addOnFailureListener(e->toast("Video upload failed: "+error(e)));
    }

    private String getFieldTextAroundVideo(){
        // The video title is the third input in the video section. It is kept in the view tree;
        // use a small tag lookup so the UI remains simple without a large member-variable list.
        return findInputWithHint("Video/Lesson title");
    }

    private String findInputWithHint(String hint){
        return findInputRecursive((View)getWindow().getDecorView(),hint);
    }

    private String findInputRecursive(View v,String hint){
        if(v instanceof EditText){
            EditText e=(EditText)v;
            if(hint.equals(e.getHint()==null?"":e.getHint().toString())) return e.getText().toString().trim();
        }
        if(v instanceof android.view.ViewGroup){
            android.view.ViewGroup g=(android.view.ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                String s=findInputRecursive(g.getChildAt(i),hint);
                if(s!=null)return s;
            }
        }
        return "";
    }

    private void uploadSelectedNote(Uri uri){
        String title=noteTitleInput.getText().toString().trim();
        if(title.isEmpty()){toast("Notes title required");return;}
        String name=safeName(uri);
        String path="course_notes/"+auth.getCurrentUser().getUid()+"/"+System.currentTimeMillis()+"_"+name;
        StorageReference ref=storage.getReference().child(path);
        Toast.makeText(this,"PDF upload शुरू हो रहा है...",Toast.LENGTH_LONG).show();
        ref.putFile(uri).addOnSuccessListener(task->ref.getDownloadUrl().addOnSuccessListener(url->{
            Map<String,Object> m=new HashMap<>();
            m.put("title",title);
            m.put("description",noteDescriptionInput.getText().toString().trim());
            m.put("courseId",noteCourseIdInput.getText().toString().trim());
            m.put("storagePath",path);
            m.put("downloadUrl",url.toString());
            m.put("uploadedBy",auth.getCurrentUser().getUid());
            m.put("createdAt",FieldValue.serverTimestamp());
            db.collection("notes").add(m).addOnSuccessListener(x->toast("PDF/Notes uploaded successfully"))
                .addOnFailureListener(e->toast("Notes record save failed: "+error(e)));
        })).addOnFailureListener(e->toast("PDF upload failed: "+error(e)));
    }

    private void uploadGeneric(Uri uri){
        String name=safeName(uri);
        String path="admin_uploads/"+auth.getCurrentUser().getUid()+"/"+System.currentTimeMillis()+"_"+name;
        StorageReference ref=storage.getReference().child(path);
        Toast.makeText(this,"File upload शुरू हो रहा है...",Toast.LENGTH_LONG).show();
        ref.putFile(uri).addOnSuccessListener(task->ref.getDownloadUrl().addOnSuccessListener(url->{
            Map<String,Object> m=new HashMap<>();
            m.put("name",name); m.put("storagePath",path); m.put("downloadUrl",url.toString());
            m.put("uploadedBy",auth.getCurrentUser().getUid()); m.put("uploadedAt",FieldValue.serverTimestamp());
            db.collection("adminUploads").add(m).addOnSuccessListener(x->toast("File uploaded successfully"))
                .addOnFailureListener(e->toast("File record failed: "+error(e)));
        })).addOnFailureListener(e->toast("Upload failed: "+error(e)));
    }

    private String safeName(Uri uri){
        String n=uri.getLastPathSegment();
        if(n==null||n.trim().isEmpty()) n="file";
        return n.replaceAll("[^a-zA-Z0-9._-]","_");
    }

    private void showText(String title,String body){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20),dp(15),dp(20),dp(15));
        TextView t=tv(title,22,true); t.setTextColor(Color.rgb(25,118,210)); box.addView(t,new LinearLayout.LayoutParams(-1,dp(50)));
        TextView b=tv(body,15,false); b.setGravity(Gravity.TOP); box.addView(b,new LinearLayout.LayoutParams(-1,0,1));
        TextView close=button("CLOSE"); box.addView(close,new LinearLayout.LayoutParams(-1,dp(50)));
        setContentView(box); close.setOnClickListener(v->setContentView(panel()));
    }

    private String error(Exception e){
        return e==null?"Unknown error":(e.getMessage()==null?"Operation failed":e.getMessage());
    }

    private void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
}
