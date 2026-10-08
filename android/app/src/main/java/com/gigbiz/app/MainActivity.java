package com.gigbiz.app;

import android.app.*;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;
import java.util.*;

public class MainActivity extends Activity {
  FirebaseAuth auth; FirebaseFirestore db; FirebaseStorage storage;
  int blue=Color.rgb(255,90,31), dark=Color.rgb(18,33,47), bg=Color.rgb(247,249,252);
  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  TextView tv(String s,int z,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(dark);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
  GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(1),Color.rgb(225,231,237));return g;}
  EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setSingleLine(true);e.setTextSize(15);e.setPadding(dp(14),0,dp(14),0);e.setBackground(box(Color.WHITE,10));return e;}
  TextView btn(String s){TextView b=tv(s,15,true);b.setGravity(Gravity.CENTER);b.setTextColor(Color.WHITE);b.setBackground(box(blue,10));return b;}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();storage=FirebaseStorage.getInstance();if(auth.getCurrentUser()==null)showLogin();else bootUser();}
  void showLogin(){
    LinearLayout r=base();r.setGravity(Gravity.CENTER_HORIZONTAL);r.setPadding(dp(24),dp(35),dp(24),dp(24));
    TextView logo=tv("G",42,true);logo.setGravity(Gravity.CENTER);logo.setTextColor(Color.WHITE);logo.setBackground(box(blue,18));r.addView(logo,new LinearLayout.LayoutParams(dp(72),dp(72)));
    TextView title=tv("Gigbiz",30,true);title.setGravity(Gravity.CENTER);r.addView(title,new LinearLayout.LayoutParams(-1,dp(55)));
    TextView sub=tv("Gig Worker App",18,false);sub.setGravity(Gravity.CENTER);r.addView(sub,new LinearLayout.LayoutParams(-1,dp(40)));
    EditText email=input("Email");EditText pass=input("Password");pass.setInputType(0x81);
    add(r,email,55,12);add(r,pass,55,12);TextView login=btn("LOGIN");add(r,login,54,18);
    TextView signup=tv("New worker? Create account",15,true);signup.setGravity(Gravity.CENTER);add(r,signup,48,8);
    login.setOnClickListener(v->{String e=email.getText().toString().trim(),p=pass.getText().toString();if(e.isEmpty()||p.length()<6){toast("Email और 6+ character password भरें");return;}login.setText("PLEASE WAIT...");auth.signInWithEmailAndPassword(e,p).addOnCompleteListener(x->{if(x.isSuccessful())bootUser();else{login.setText("LOGIN");toast(err(x.getException()));}});});
    signup.setOnClickListener(v->showSignup());setContentView(r);
  }
  void showSignup(){
    LinearLayout r=base();r.setPadding(dp(24),dp(28),dp(24),dp(24));
    TextView h=tv("Create Gig Worker Account",25,true);h.setGravity(Gravity.CENTER);add(r,h,65,0);
    EditText name=input("Full name"),email=input("Email"),phone=input("Mobile number"),skills=input("Skills (e.g. Delivery, Sales, Data Entry)"),city=input("City / Location");EditText pass=input("Password (6+)");pass.setInputType(0x81);
    add(r,name,54,8);add(r,email,54,10);add(r,phone,54,10);add(r,skills,54,10);add(r,city,54,10);add(r,pass,54,10);
    TextView c=btn("CREATE ACCOUNT");add(r,c,54,18);TextView back=tv("← Back to Login",15,true);back.setGravity(Gravity.CENTER);add(r,back,45,5);
    c.setOnClickListener(v->{if(name.getText().toString().trim().isEmpty()||email.getText().toString().trim().isEmpty()||phone.getText().toString().trim().isEmpty()||pass.getText().toString().length()<6){toast("Name, mobile, email और 6+ password भरें");return;}c.setText("CREATING...");auth.createUserWithEmailAndPassword(email.getText().toString().trim(),pass.getText().toString()).addOnCompleteListener(x->{if(!x.isSuccessful()){c.setText("CREATE ACCOUNT");toast(err(x.getException()));return;}Map<String,Object> m=new HashMap<>();m.put("name",name.getText().toString().trim());m.put("email",email.getText().toString().trim());m.put("phone",phone.getText().toString().trim());m.put("skills",skills.getText().toString().trim());m.put("location",city.getText().toString().trim());m.put("role","worker");m.put("wallet",0);m.put("status","active");m.put("createdAt",FieldValue.serverTimestamp());db.collection("users").document(auth.getCurrentUser().getUid()).set(m).addOnSuccessListener(z->bootUser()).addOnFailureListener(e->{c.setText("CREATE ACCOUNT");toast(err(e));});});});
    back.setOnClickListener(v->showLogin());setContentView(r);
  }
  void bootUser(){db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(d->{String role=d.getString("role");if("admin".equals(role)){auth.signOut();toast("Admin के लिए Windows Admin App इस्तेमाल करें.");showLogin();}else showHome(d);}).addOnFailureListener(e->toast(err(e)));}
  LinearLayout base(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setBackgroundColor(bg);return r;}
  void add(LinearLayout r,View v,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(h));p.topMargin=dp(top);r.addView(v,p);}
  ScrollView scroll(LinearLayout r){ScrollView s=new ScrollView(this);s.addView(r);return s;}
  void showHome(DocumentSnapshot u){
    LinearLayout root=base();LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(14),dp(12),dp(14),dp(12));top.setBackgroundColor(Color.WHITE);
    TextView brand=tv("Gigbiz",25,true);brand.setTextColor(blue);top.addView(brand,new LinearLayout.LayoutParams(0,dp(55),1));TextView out=tv("↪",28,true);top.addView(out,new LinearLayout.LayoutParams(dp(55),dp(55)));out.setOnClickListener(v->{auth.signOut();showLogin();});root.addView(top);
    LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(14),dp(10),dp(14),dp(30));
    String name=u.getString("name");if(name==null)name="Worker";
    TextView hello=tv("Hello, "+name+" 👋",24,true);content.addView(hello,new LinearLayout.LayoutParams(-1,dp(60)));
    TextView sub=tv("Find gigs, complete tasks and earn.",15,false);content.addView(sub,new LinearLayout.LayoutParams(-1,dp(38)));
    LinearLayout wallet=new LinearLayout(this);wallet.setPadding(dp(18),dp(14),dp(18),dp(14));wallet.setBackground(box(dark,16));TextView wt=tv("Wallet Balance\n₹"+String.valueOf(u.get("wallet")==null?0:u.get("wallet")),20,true);wt.setTextColor(Color.WHITE);wallet.addView(wt,new LinearLayout.LayoutParams(0,dp(82),1));content.addView(wallet);
    GridLayout g=new GridLayout(this);g.setColumnCount(2);g.setPadding(0,dp(8),0,dp(8));String[][] cards={{"💼","Find Gigs"},{"📁","My Projects"},{"📄","My Leads"},{"✅","My Tasks"},{"💰","Wallet"},{"🪪","My ID Card"},{"👤","My Profile"},{"🕒","Activity"}};
    for(String[] c:cards){TextView v=tv(c[0]+"\n"+c[1],17,true);v.setGravity(Gravity.CENTER);v.setBackground(box(Color.WHITE,14));GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(100);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);p.setMargins(dp(5),dp(5),dp(5),dp(5));g.addView(v,p);v.setOnClickListener(x->openSection(c[1]));}content.addView(g);
    root.addView(scroll(content),new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  }
  void openSection(String s){
    if(s.equals("Find Gigs")) gigsScreen(); else if(s.equals("My Projects")) listScreen("projects","My Projects","No projects assigned."); else if(s.equals("My Leads")) listScreen("leads","My Leads","No leads from Admin."); else if(s.equals("My Tasks")) tasksScreen(); else if(s.equals("Wallet")) walletScreen(); else if(s.equals("My ID Card")) idCard(); else if(s.equals("My Profile")) profile(); else activityScreen();
  }
  void header(LinearLayout r,String title){TextView back=tv("←  "+title,21,true);back.setTextColor(blue);back.setPadding(dp(14),dp(15),dp(10),dp(15));r.addView(back,new LinearLayout.LayoutParams(-1,dp(60)));back.setOnClickListener(v->bootUser());}
  void gigsScreen(){
    LinearLayout r=base();header(r,"Find Gigs & Services");LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);ScrollView sv=new ScrollView(this);sv.addView(list);r.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
    db.collection("gigs").whereEqualTo("status","active").get().addOnSuccessListener(res->{if(res.isEmpty()){list.addView(tv("No active gigs right now.",17,false));return;}for(DocumentSnapshot d:res)list.addView(gigCard(d));}).addOnFailureListener(e->list.addView(tv(err(e),14,false)));setContentView(r);
  }
  View gigCard(DocumentSnapshot d){
    Map<String,Object>x=d.getData();LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(box(Color.WHITE,14));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(190));cp.setMargins(dp(10),dp(8),dp(10),dp(8));c.setLayoutParams(cp);
    TextView t=tv(String.valueOf(x.get("title")==null?"Gig":x.get("title")),19,true);c.addView(t);c.addView(tv("💰 ₹"+String.valueOf(x.get("pay")==null?"—":x.get("pay"))+"   📍 "+String.valueOf(x.get("location")==null?"Any":x.get("location")),14,false));c.addView(tv(String.valueOf(x.get("description")==null?"":x.get("description")),14,false),new LinearLayout.LayoutParams(-1,dp(65)));TextView a=btn("APPLY NOW");c.addView(a,new LinearLayout.LayoutParams(-1,dp(48)));a.setOnClickListener(v->apply(d));return c;
  }
  void apply(DocumentSnapshot d){Map<String,Object>m=new HashMap<>();m.put("workerId",auth.getCurrentUser().getUid());m.put("workerEmail",auth.getCurrentUser().getEmail());m.put("gigId",d.getId());m.put("gigTitle",d.getString("title"));m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("applications").add(m).addOnSuccessListener(x->{toast("Application submitted.");activity("Applied for "+d.getString("title"));}).addOnFailureListener(e->toast(err(e)));}
  void listScreen(String col,String title,String empty){LinearLayout r=base();header(r,title);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);ScrollView s=new ScrollView(this);s.addView(list);r.addView(s,new LinearLayout.LayoutParams(-1,0,1));String uid=auth.getCurrentUser().getUid();Query q=db.collection(col);if(col.equals("leads")||col.equals("projects"))q=q.whereEqualTo("workerId",uid);q.get().addOnSuccessListener(res->{if(res.isEmpty())list.addView(tv(empty,17,false));for(DocumentSnapshot d:res){Map<String,Object>x=d.getData();LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(box(Color.WHITE,12));String titleV=String.valueOf(x.get("title")==null?x.get("name")==null?col:x.get("name"):x.get("title"));c.addView(tv(titleV,18,true));c.addView(tv(String.valueOf(x.get("description")==null?x.get("instructions")==null?"":x.get("instructions"):x.get("description")),14,false));c.addView(tv("Status: "+String.valueOf(x.get("status")==null?"assigned":x.get("status")),13,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(145));p.setMargins(dp(10),dp(7),dp(10),dp(7));c.setLayoutParams(p);list.addView(c);}}).addOnFailureListener(e->list.addView(tv(err(e),14,false)));setContentView(r);}
  void tasksScreen(){listScreen("tasks","My Tasks","No tasks assigned.");}
  void walletScreen(){LinearLayout r=base();header(r,"Wallet");TextView b=tv("💰  Wallet",28,true);add(r,b,70,10);db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(d->{add(r,tv("Available Balance\n₹"+String.valueOf(d.get("wallet")==null?0:d.get("wallet")),24,true),100,8);});TextView w=btn("REQUEST WITHDRAWAL");add(r,w,54,20);w.setOnClickListener(v->requestWithdrawal());setContentView(r);}
  void requestWithdrawal(){final EditText amt=input("Amount");final EditText upi=input("UPI ID / Bank detail");LinearLayout r=base();header(r,"Withdraw");add(r,tv("Withdrawal Request",23,true),55,10);add(r,amt,54,10);add(r,upi,54,10);TextView b=btn("SUBMIT REQUEST");add(r,b,54,18);b.setOnClickListener(v->{Map<String,Object>m=new HashMap<>();m.put("workerId",auth.getCurrentUser().getUid());m.put("amount",amt.getText().toString().trim());m.put("paymentDetail",upi.getText().toString().trim());m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("withdrawals").add(m).addOnSuccessListener(x->{toast("Withdrawal request submitted.");bootUser();}).addOnFailureListener(e->toast(err(e)));});setContentView(r);}
  void idCard(){db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(d->{LinearLayout r=base();header(r,"My ID Card");LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(22),dp(20),dp(22),dp(20));card.setBackground(box(Color.WHITE,18));TextView g=tv("GIGBIZ",25,true);g.setTextColor(blue);card.addView(g);card.addView(tv("GIG WORKER ID",13,true));card.addView(tv("Name: "+String.valueOf(d.get("name"))+"\nMobile: "+String.valueOf(d.get("phone"))+"\nWorker ID: "+d.getId()+"\nSkills: "+String.valueOf(d.get("skills")),16,false));r.addView(card,new LinearLayout.LayoutParams(-1,dp(260)));setContentView(r);});}
  void profile(){db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(d->{LinearLayout r=base();header(r,"My Profile");for(String k:new String[]{"name","email","phone","skills","location"})add(r,tv(k.toUpperCase()+"\n"+String.valueOf(d.get(k)),17,false),70,8);setContentView(r);});}
  void activityScreen(){LinearLayout r=base();header(r,"Activity");TextView l=tv("Loading...",16,false);add(r,l,80,10);db.collection("activity").whereEqualTo("userId",auth.getCurrentUser().getUid()).get().addOnSuccessListener(s->{StringBuilder b=new StringBuilder();for(DocumentSnapshot d:s)b.append("• ").append(d.getString("message")).append("\n\n");l.setText(b.length()==0?"No activity yet.":b.toString());});setContentView(r);}
  void activity(String msg){Map<String,Object>m=new HashMap<>();m.put("userId",auth.getCurrentUser().getUid());m.put("message",msg);m.put("createdAt",FieldValue.serverTimestamp());db.collection("activity").add(m);}
  String err(Exception e){return e==null?"Something went wrong":e.getMessage()==null?"Operation failed":e.getMessage();}
}