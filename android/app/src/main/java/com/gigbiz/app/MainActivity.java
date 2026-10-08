package com.gigbiz.app;

import android.app.*;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.content.*;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.InputStream;
import android.widget.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;
import java.util.*;

public class MainActivity extends Activity {
 FirebaseAuth auth; FirebaseFirestore db; FirebaseStorage storage;
 int purple=Color.rgb(109,58,190), purple2=Color.rgb(133,74,214), green=Color.rgb(38,198,112), dark=Color.rgb(27,43,52), muted=Color.rgb(105,113,122), bg=Color.rgb(249,250,252), white=Color.WHITE;
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView tv(String s,float z,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(dark);t.setTypeface(Typeface.DEFAULT,b?Typeface.BOLD:Typeface.NORMAL);t.setPadding(dp(7),dp(5),dp(7),dp(5));return t;}
 GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 GradientDrawable stroke(int color,int line,int radius){GradientDrawable g=bg(white,radius);g.setStroke(dp(line),color);return g;}
 TextView button(String s,int color){TextView t=tv(s,14,true);t.setGravity(Gravity.CENTER);t.setTextColor(white);t.setBackground(bg(color,10));return t;}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 LinearLayout base(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(bg);return l;}
 void add(LinearLayout p,View v,int h,int top){LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,dp(h));q.topMargin=dp(top);p.addView(v,q);}
 @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();storage=FirebaseStorage.getInstance();if(auth.getCurrentUser()==null)login();else boot();}
 void login(){LinearLayout r=base();r.setGravity(Gravity.CENTER);r.setPadding(dp(24),dp(20),dp(24),dp(20));TextView logo=tv("G",42,true);logo.setGravity(Gravity.CENTER);logo.setTextColor(white);logo.setBackground(bg(purple,22));r.addView(logo,new LinearLayout.LayoutParams(dp(78),dp(78)));TextView h=tv("Gigbiz",32,true);h.setGravity(Gravity.CENTER);r.addView(h,new LinearLayout.LayoutParams(-1,dp(60)));TextView s=tv("Earn • Work • Grow",17,false);s.setGravity(Gravity.CENTER);r.addView(s,new LinearLayout.LayoutParams(-1,dp(40)));EditText e=field("Email"),p=field("Password");p.setInputType(0x81);add(r,e,54,10);add(r,p,54,10);TextView b=button("LOGIN",purple);add(r,b,54,18);TextView n=tv("New worker? Create account",15,true);n.setGravity(Gravity.CENTER);add(r,n,45,8);b.setOnClickListener(v->{auth.signInWithEmailAndPassword(e.getText().toString().trim(),p.getText().toString()).addOnCompleteListener(x->{if(x.isSuccessful())boot();else toast(err(x.getException()));});});n.setOnClickListener(v->signup());setContentView(r);}
 EditText field(String h){EditText e=new EditText(this);e.setHint(h);e.setSingleLine(true);e.setTextSize(15);e.setPadding(dp(14),0,dp(14),0);e.setBackground(stroke(Color.rgb(220,225,232),1,11));return e;}
 void signup(){LinearLayout r=base();r.setPadding(dp(22),dp(28),dp(22),dp(20));TextView h=tv("Create your Gigbiz account",26,true);h.setGravity(Gravity.CENTER);add(r,h,65,0);EditText n=field("Full name"),e=field("Email"),ph=field("Mobile number"),sk=field("Skills"),loc=field("City / Location"),pw=field("Password (6+)");pw.setInputType(0x81);for(EditText x:new EditText[]{n,e,ph,sk,loc,pw})add(r,x,53,8);TextView b=button("CREATE ACCOUNT",purple);add(r,b,53,18);b.setOnClickListener(v->{auth.createUserWithEmailAndPassword(e.getText().toString().trim(),pw.getText().toString()).addOnCompleteListener(x->{if(!x.isSuccessful()){toast(err(x.getException()));return;}Map<String,Object> m=new HashMap<>();m.put("name",n.getText().toString().trim());m.put("email",e.getText().toString().trim());m.put("phone",ph.getText().toString().trim());m.put("skills",sk.getText().toString().trim());m.put("location",loc.getText().toString().trim());m.put("role","worker");m.put("wallet",0);m.put("status","active");m.put("createdAt",FieldValue.serverTimestamp());db.collection("users").document(auth.getCurrentUser().getUid()).set(m).addOnSuccessListener(z->boot()).addOnFailureListener(q->toast(err(q)));});});setContentView(r);}
 void boot(){db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(d->{if(!"worker".equals(d.getString("role"))){auth.signOut();toast("Worker account required.");login();}else home(d);}).addOnFailureListener(e->toast(err(e)));}
 void home(DocumentSnapshot u){
  LinearLayout root=base();
  ScrollView scroll=new ScrollView(this);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(8),dp(16),dp(100));
  LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(dp(0),dp(4),dp(0),dp(4));
  TextView avatar=tv("G",18,true);avatar.setGravity(Gravity.CENTER);avatar.setTextColor(white);avatar.setBackground(bg(purple,50));head.addView(avatar,new LinearLayout.LayoutParams(dp(46),dp(46)));avatar.setOnClickListener(v->profileScreen());
  LinearLayout hi=new LinearLayout(this);hi.setOrientation(LinearLayout.VERTICAL);String nm=u.getString("name");if(nm==null)nm="Worker";TextView hh=tv("Hi, "+nm,20,true);TextView hs=tv("Ready to earn today?",12,false);hs.setTextColor(muted);hi.addView(hh);hi.addView(hs);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,dp(55),1);hp.setMargins(dp(7),0,0,0);head.addView(hi,hp);
  TextView bell=tv("♧",28,false);bell.setTextColor(purple);head.addView(bell,new LinearLayout.LayoutParams(dp(42),dp(50)));TextView noti=tv("♢",28,false);noti.setTextColor(purple);head.addView(noti,new LinearLayout.LayoutParams(dp(42),dp(50)));c.addView(head);
  TextView banner=tv("GIGBIZ\nTurn your skills into income\nFind gigs • Complete work • Get paid",19,true);banner.setTextColor(white);banner.setPadding(dp(20),dp(16),dp(20),dp(12));banner.setBackground(bg(purple,18));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(126));bp.topMargin=dp(8);c.addView(banner,bp);
  TextView earn=tv("YOUR EARNINGS",13,true);earn.setTextColor(muted);earn.setPadding(0,dp(20),0,dp(6));c.addView(earn);
  LinearLayout stats=new LinearLayout(this);stats.setGravity(Gravity.CENTER);stats.addView(stat("₹"+String.valueOf(u.get("wallet")==null?0:u.get("wallet")),"Wallet",green),new LinearLayout.LayoutParams(0,dp(82),1));stats.addView(stat("0","Completed",purple),new LinearLayout.LayoutParams(0,dp(82),1));stats.addView(stat("0","Applications",purple2),new LinearLayout.LayoutParams(0,dp(82),1));c.addView(stats);
  sectionTitle(c,"WORK & EARN","See All",v->gigsScreen());
  LinearLayout serviceRow=new LinearLayout(this);serviceRow.setOrientation(LinearLayout.HORIZONTAL);HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);hsv.addView(serviceRow);c.addView(hsv,new LinearLayout.LayoutParams(-1,dp(138)));
  db.collection("services").whereEqualTo("status","active").limit(8).get().addOnSuccessListener(s->{if(s.isEmpty()){serviceRow.addView(serviceCard("Delivery","Earn by completing local jobs","🚚",green),new LinearLayout.LayoutParams(dp(190),dp(120)));serviceRow.addView(serviceCard("Sales","Promote products & services","📈",purple),new LinearLayout.LayoutParams(dp(190),dp(120)));serviceRow.addView(serviceCard("Digital Work","Data entry, design & online work","💻",purple2),new LinearLayout.LayoutParams(dp(190),dp(120)));}else for(DocumentSnapshot d:s)serviceRow.addView(serviceCard(d.getString("name"),d.getString("description"),"★",purple),new LinearLayout.LayoutParams(dp(190),dp(120)));});
  sectionTitle(c,"ADDITIONAL EARNING OPTIONS","",null);
  LinearLayout extra=new LinearLayout(this);extra.setOrientation(LinearLayout.HORIZONTAL);extra.addView(serviceCard("Referral Program","Invite workers and earn","🤝",green),new LinearLayout.LayoutParams(dp(205),dp(125)));extra.addView(serviceCard("Team Building","Build your worker network","👥",purple),new LinearLayout.LayoutParams(dp(205),dp(125)));c.addView(extra);
  TextView ky=tv("✓  Complete Your Profile & KYC\n   Get more work opportunities",16,true);ky.setPadding(dp(16),dp(10),dp(10),dp(10));ky.setBackground(stroke(Color.rgb(215,222,228),1,14));LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(-1,dp(75));kp.topMargin=dp(16);c.addView(ky,kp);
  sectionTitle(c,"FEATURED","",null);LinearLayout feat=new LinearLayout(this);feat.setOrientation(LinearLayout.HORIZONTAL);feat.addView(feature("Need Help?","Get support","?",purple),new LinearLayout.LayoutParams(0,dp(112),1));feat.addView(feature("Training","Learn skills","🎓",purple),new LinearLayout.LayoutParams(0,dp(112),1));feat.addView(feature("Marketing","Grow faster","📣",purple),new LinearLayout.LayoutParams(0,dp(112),1));c.addView(feat);
  sectionTitle(c,"RECOMMENDED GIGS","See All",v->gigsScreen());
  LinearLayout rec=new LinearLayout(this);rec.setOrientation(LinearLayout.HORIZONTAL);HorizontalScrollView rh=new HorizontalScrollView(this);rh.setHorizontalScrollBarEnabled(false);rh.addView(rec);c.addView(rh,new LinearLayout.LayoutParams(-1,dp(150)));
  db.collection("gigs").whereEqualTo("status","active").limit(6).get().addOnSuccessListener(s->{if(s.isEmpty()){rec.addView(gigMini("Local Delivery","₹500+","📦"),new LinearLayout.LayoutParams(dp(220),dp(125)));rec.addView(gigMini("Sales Executive","₹1,000+","📈"),new LinearLayout.LayoutParams(dp(220),dp(125)));}else for(DocumentSnapshot d:s)rec.addView(gigMini(d.getString("title"),"₹"+String.valueOf(d.get("pay")==null?"":d.get("pay")),"💼"),new LinearLayout.LayoutParams(dp(220),dp(125)));});
  scroll.addView(c);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root.addView(bottomNav(),new LinearLayout.LayoutParams(-1,dp(72)));setContentView(root);
 }
 View stat(String value,String label,int color){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.setBackground(stroke(Color.rgb(232,234,239),1,14));TextView a=tv(value,19,true);a.setTextColor(color);a.setGravity(Gravity.CENTER);x.addView(a);TextView b=tv(label,11,false);b.setTextColor(muted);b.setGravity(Gravity.CENTER);x.addView(b);return x;}
 void sectionTitle(LinearLayout c,String a,String b,View.OnClickListener l){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView x=tv(a,18,true);x.setTextColor(dark);r.addView(x,new LinearLayout.LayoutParams(0,dp(54),1));if(!b.isEmpty()){TextView y=tv(b,13,true);y.setTextColor(purple);r.addView(y,new LinearLayout.LayoutParams(dp(65),dp(45)));if(l!=null)y.setOnClickListener(l);}c.addView(r);}
 View serviceCard(String title,String desc,String icon,int color){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(14),dp(10),dp(10),dp(8));x.setBackground(bg(white,14));TextView i=tv(icon,22,true);i.setTextColor(color);x.addView(i);x.addView(tv(title==null?"Service":title,15,true));TextView d=tv(desc==null?"":desc,11,false);d.setTextColor(muted);x.addView(d);TextView go=tv("START NOW",12,true);go.setTextColor(purple);x.addView(go);return x;}
 View feature(String title,String desc,String icon,int color){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.setPadding(dp(4),dp(8),dp(4),dp(5));x.setBackground(stroke(Color.rgb(225,229,235),1,13));TextView i=tv(icon,24,true);i.setTextColor(color);i.setGravity(Gravity.CENTER);x.addView(i);TextView a=tv(title,12,true);a.setGravity(Gravity.CENTER);x.addView(a);TextView d=tv(desc,10,false);d.setTextColor(muted);d.setGravity(Gravity.CENTER);x.addView(d);return x;}
 View gigMini(String title,String pay,String icon){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(14),dp(10),dp(10),dp(8));x.setBackground(bg(white,14));TextView i=tv(icon,23,true);i.setTextColor(purple);x.addView(i);x.addView(tv(title==null?"Gig":title,15,true));TextView p=tv("Earn "+pay,13,true);p.setTextColor(purple);x.addView(p);x.addView(tv("View & Apply →",11,false));return x;}
 View bottomNav(){LinearLayout n=new LinearLayout(this);n.setGravity(Gravity.CENTER);n.setPadding(dp(5),dp(3),dp(5),dp(3));n.setBackground(bg(white,20));String[] labels={"⌂\nHome","▤\nGigs","＋","♧\nReferral","♙\nMy Team"};for(int i=0;i<labels.length;i++){TextView b=tv(labels[i],i==2?25:11,true);b.setGravity(Gravity.CENTER);if(i==2){b.setTextColor(white);b.setBackground(bg(green,50));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(62),dp(62));p.setMargins(dp(5),-dp(14),dp(5),0);n.addView(b,p);b.setOnClickListener(v->gigsScreen());}else{b.setTextColor(i==0?purple:dark);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(62),1);n.addView(b,p);if(i==1)b.setOnClickListener(v->gigsScreen());if(i==3)b.setOnClickListener(v->toast("Referral feature coming next."));if(i==4)b.setOnClickListener(v->toast("My Team feature coming next."));}}return n;}
 void gigsScreen(){LinearLayout r=base();topBar(r,"Find Gigs & Services");LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);ScrollView s=new ScrollView(this);s.addView(list);r.addView(s,new LinearLayout.LayoutParams(-1,0,1));db.collection("gigs").whereEqualTo("status","active").get().addOnSuccessListener(q->{if(q.isEmpty())list.addView(tv("No active gigs right now.",17,false));for(DocumentSnapshot d:q)list.addView(gigCard(d));});setContentView(r);}
 View gigCard(DocumentSnapshot d){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(12),dp(16),dp(12));c.setBackground(bg(white,15));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(175));p.setMargins(dp(10),dp(7),dp(10),dp(7));c.setLayoutParams(p);c.addView(tv(String.valueOf(d.get("title")),18,true));c.addView(tv("Earn ₹"+String.valueOf(d.get("pay")==null?"":d.get("pay"))+"  •  "+String.valueOf(d.get("location")==null?"Any":d.get("location")),13,false));c.addView(tv(String.valueOf(d.get("description")==null?"":d.get("description")),12,false),new LinearLayout.LayoutParams(-1,dp(55)));TextView a=button("APPLY NOW",purple);c.addView(a,new LinearLayout.LayoutParams(-1,dp(44)));a.setOnClickListener(v->apply(d));return c;}
 void apply(DocumentSnapshot d){Map<String,Object>m=new HashMap<>();m.put("workerId",auth.getCurrentUser().getUid());m.put("workerEmail",auth.getCurrentUser().getEmail());m.put("gigId",d.getId());m.put("gigTitle",d.getString("title"));m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("applications").add(m).addOnSuccessListener(x->toast("Application submitted successfully.")).addOnFailureListener(e->toast(err(e)));}
 void profileScreen(){
  final String uid=auth.getCurrentUser().getUid();
  db.collection("users").document(uid).get().addOnSuccessListener(u->{
   LinearLayout root=base(); ScrollView sc=new ScrollView(this); LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16),dp(8),dp(16),dp(30));
   LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
   TextView back=tv("‹",34,true); back.setTextColor(purple); bar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(58))); back.setOnClickListener(v->home(u));
   TextView title=tv("My Profile",24,true); bar.addView(title,new LinearLayout.LayoutParams(0,dp(58),1));
   TextView lang=tv("अ  A",17,true); lang.setTextColor(purple); bar.addView(lang,new LinearLayout.LayoutParams(dp(60),dp(58))); c.addView(bar);
   LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(14),dp(12),dp(12),dp(12)); card.setBackground(bg(Color.rgb(105,105,105),20));
   TextView av=tv("G",30,true); av.setGravity(Gravity.CENTER); av.setTextColor(white); av.setBackground(bg(purple,50)); card.addView(av,new LinearLayout.LayoutParams(dp(72),dp(72)));
   LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(12),0,0,0); String name=u.getString("name"); if(name==null||name.isEmpty())name="Worker"; info.addView(tv(name,21,true)); info.addView(tv("WORKER ID : "+uid.substring(0,Math.min(8,uid.length())).toUpperCase(),11,false));
   TextView share=button("Share Profile",purple2); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(150),dp(42));sp.topMargin=dp(7);info.addView(share,sp);card.addView(info,new LinearLayout.LayoutParams(0,dp(100),1));
   TextView level=tv("★\nWORKER",13,true);level.setGravity(Gravity.CENTER);level.setTextColor(white);card.addView(level,new LinearLayout.LayoutParams(dp(85),dp(90)));c.addView(card,new LinearLayout.LayoutParams(-1,dp(124)));
   share.setOnClickListener(v->shareProfile(u));
   TextView edit=tv("Tap any section to view or update your information",13,false);edit.setTextColor(muted);c.addView(edit);
   addProfileRow(c,"👤","Personal Details","name, phone, email, city, address",v->editPersonal(u));
   addProfileRow(c,"💳","Payment Settings","UPI, bank account, account holder",v->editPayment(u));
   addProfileRow(c,"💼","Professional Details","skills, experience, profession",v->editProfessional(u));
   addProfileRow(c,"🪪","KYC Details","Aadhaar/PAN and verification status",v->editKyc(u));
   addProfileRow(c,"🎓","Education Details","qualification, institute, year",v->editEducation(u));
   addProfileRow(c,"🏆","Contest History","View your completed contests",v->contestHistory());
   addProfileRow(c,"🔒","M-PIN","Set or change your secure app PIN",v->editPin());
   addProfileRow(c,"GST","GST Management","GST number and business details",v->editGst(u));
   addProfileRow(c,"🎧","Help & Support","Get help from Gigbiz support",v->helpSupport());
   TextView links=tv("OTHER LINKS",13,true);links.setTextColor(purple);links.setPadding(dp(0),dp(18),0,dp(5));c.addView(links);
   addProfileRow(c,"📄","Advisor Agreement","Gigbiz worker terms",v->showInfo("Advisor Agreement","Please review the worker agreement provided by Gigbiz before accepting work."));
   addProfileRow(c,"🔐","Privacy Policy","How your account information is used",v->showInfo("Privacy Policy","Your account information is used to operate Gigbiz services, applications, payments and support."));
   TextView del=button("DELETE ACCOUNT",Color.rgb(205,55,70));LinearLayout.LayoutParams dp1=new LinearLayout.LayoutParams(-1,dp(50));dp1.topMargin=dp(24);c.addView(del,dp1);
   TextView warning=tv("This permanently removes your Gigbiz account. This action cannot be undone.",12,false);warning.setTextColor(muted);warning.setGravity(Gravity.CENTER);c.addView(warning);
   del.setOnClickListener(v->deleteAccountConfirm(u));
   sc.addView(c);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  }).addOnFailureListener(e->toast(err(e)));
 }
 void addProfileRow(LinearLayout c,String icon,String title,String sub,View.OnClickListener l){
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(4),dp(6),dp(4),dp(6));row.setClickable(true);
  TextView i=tv(icon,24,true);i.setGravity(Gravity.CENTER);i.setTextColor(purple);row.addView(i,new LinearLayout.LayoutParams(dp(58),dp(68)));
  LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.addView(tv(title,17,true));TextView s=tv(sub,11,false);s.setTextColor(muted);tx.addView(s);row.addView(tx,new LinearLayout.LayoutParams(0,dp(68),1));
  TextView a=tv("›",30,false);a.setTextColor(purple2);a.setGravity(Gravity.CENTER);row.addView(a,new LinearLayout.LayoutParams(dp(45),dp(68)));row.setOnClickListener(l);c.addView(row,new LinearLayout.LayoutParams(-1,dp(78)));
 }
 EditText dialogField(LinearLayout box,String label,String value){TextView l=tv(label,12,true);l.setTextColor(muted);box.addView(l);EditText e=field(label);if(value!=null)e.setText(value);box.addView(e,new LinearLayout.LayoutParams(-1,dp(50)));return e;}
 void saveFields(String msg,Map<String,Object> m){db.collection("users").document(auth.getCurrentUser().getUid()).update(m).addOnSuccessListener(v->toast(msg)).addOnFailureListener(e->toast(err(e)));}
 void editPersonal(DocumentSnapshot u){
  LinearLayout b=base();b.setPadding(dp(8),0,dp(8),0);EditText n=dialogField(b,"Full Name",u.getString("name"));EditText ph=dialogField(b,"Mobile",u.getString("phone"));EditText em=dialogField(b,"Email",u.getString("email"));EditText city=dialogField(b,"City",u.getString("location"));EditText addr=dialogField(b,"Address",u.getString("address"));
  new AlertDialog.Builder(this).setTitle("Personal Details").setView(b).setPositiveButton("SAVE",(d,w)->{Map<String,Object>m=new HashMap<>();m.put("name",n.getText().toString().trim());m.put("phone",ph.getText().toString().trim());m.put("email",em.getText().toString().trim());m.put("location",city.getText().toString().trim());m.put("address",addr.getText().toString().trim());saveFields("Personal details updated.",m);}).setNegativeButton("CANCEL",null).show();
 }
 void editPayment(DocumentSnapshot u){
  LinearLayout b=base();b.setPadding(dp(8),0,dp(8),0);EditText upi=dialogField(b,"UPI ID",u.getString("upiId"));EditText holder=dialogField(b,"Account Holder",u.getString("accountHolder"));EditText bank=dialogField(b,"Bank Name",u.getString("bankName"));EditText acc=dialogField(b,"Account Number",u.getString("accountNumber"));EditText ifsc=dialogField(b,"IFSC",u.getString("ifsc"));
  new AlertDialog.Builder(this).setTitle("Payment Settings").setView(b).setPositiveButton("SAVE",(d,w)->{Map<String,Object>m=new HashMap<>();m.put("upiId",upi.getText().toString().trim());m.put("accountHolder",holder.getText().toString().trim());m.put("bankName",bank.getText().toString().trim());m.put("accountNumber",acc.getText().toString().trim());m.put("ifsc",ifsc.getText().toString().trim());saveFields("Payment settings updated.",m);}).setNegativeButton("CANCEL",null).show();
 }
 void editProfessional(DocumentSnapshot u){
  LinearLayout b=base();b.setPadding(dp(8),0,dp(8),0);EditText skill=dialogField(b,"Skills",u.getString("skills"));EditText prof=dialogField(b,"Profession",u.getString("profession"));EditText exp=dialogField(b,"Experience",u.getString("experience"));EditText bio=dialogField(b,"About / Bio",u.getString("bio"));
  new AlertDialog.Builder(this).setTitle("Professional Details").setView(b).setPositiveButton("SAVE",(d,w)->{Map<String,Object>m=new HashMap<>();m.put("skills",skill.getText().toString().trim());m.put("profession",prof.getText().toString().trim());m.put("experience",exp.getText().toString().trim());m.put("bio",bio.getText().toString().trim());saveFields("Professional details updated.",m);}).setNegativeButton("CANCEL",null).show();
 }
 void editKyc(DocumentSnapshot u){
  LinearLayout b=base();b.setPadding(dp(8),0,dp(8),0);EditText pan=dialogField(b,"PAN Number",u.getString("pan"));EditText aad=dialogField(b,"Aadhaar Last 4 Digits",u.getString("aadhaarLast4"));EditText status=dialogField(b,"KYC Status",u.getString("kycStatus"));status.setEnabled(false);
  new AlertDialog.Builder(this).setTitle("KYC Details").setView(b).setPositiveButton("SAVE",(d,w)->{Map<String,Object>m=new HashMap<>();m.put("pan",pan.getText().toString().trim());m.put("aadhaarLast4",aad.getText().toString().trim());m.put("kycStatus",u.getString("kycStatus")==null?"pending":u.getString("kycStatus"));saveFields("KYC details updated.",m);}).setNegativeButton("CANCEL",null).show();
 }
 void editEducation(DocumentSnapshot u){
  LinearLayout b=base();b.setPadding(dp(8),0,dp(8),0);EditText q=dialogField(b,"Qualification",u.getString("qualification"));EditText inst=dialogField(b,"Institute",u.getString("institute"));EditText year=dialogField(b,"Passing Year",u.getString("passingYear"));
  new AlertDialog.Builder(this).setTitle("Education Details").setView(b).setPositiveButton("SAVE",(d,w)->{Map<String,Object>m=new HashMap<>();m.put("qualification",q.getText().toString().trim());m.put("institute",inst.getText().toString().trim());m.put("passingYear",year.getText().toString().trim());saveFields("Education details updated.",m);}).setNegativeButton("CANCEL",null).show();
 }
 void editGst(DocumentSnapshot u){
  LinearLayout b=base();b.setPadding(dp(8),0,dp(8),0);EditText gst=dialogField(b,"GST Number",u.getString("gstNumber"));EditText business=dialogField(b,"Business Name",u.getString("businessName"));EditText address=dialogField(b,"Business Address",u.getString("businessAddress"));
  new AlertDialog.Builder(this).setTitle("GST Management").setView(b).setPositiveButton("SAVE",(d,w)->{Map<String,Object>m=new HashMap<>();m.put("gstNumber",gst.getText().toString().trim());m.put("businessName",business.getText().toString().trim());m.put("businessAddress",address.getText().toString().trim());saveFields("GST details updated.",m);}).setNegativeButton("CANCEL",null).show();
 }
 void editPin(){
  final String uid=auth.getCurrentUser().getUid();android.content.SharedPreferences p=getSharedPreferences("gigbiz_secure",MODE_PRIVATE);String old=p.getString("mpin","");
  LinearLayout b=base();b.setPadding(dp(8),0,dp(8),0);EditText pin=dialogField(b,old.isEmpty()?"Create 4 digit M-PIN":"New 4 digit M-PIN","");pin.setInputType(2);new AlertDialog.Builder(this).setTitle(old.isEmpty()?"Set M-PIN":"Change M-PIN").setView(b).setPositiveButton("SAVE",(d,w)->{String x=pin.getText().toString().trim();if(x.length()!=4){toast("M-PIN must be exactly 4 digits.");return;}p.edit().putString("mpin",x).apply();toast("M-PIN saved on this device.");}).setNegativeButton("CANCEL",null).show();
 }
 void contestHistory(){
  LinearLayout b=base();b.setPadding(dp(8),0,dp(8),0);ScrollView s=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);s.addView(list);b.addView(s,new LinearLayout.LayoutParams(-1,dp(320)));
  db.collection("contestHistory").whereEqualTo("workerId",auth.getCurrentUser().getUid()).get().addOnSuccessListener(q->{if(q.isEmpty())list.addView(tv("No contest history yet.",15,false));for(DocumentSnapshot d:q)list.addView(tv("🏆 "+String.valueOf(d.get("title"))+"\nStatus: "+String.valueOf(d.get("status"))+"\nReward: ₹"+String.valueOf(d.get("reward")==null?"0":d.get("reward")),14,false));});
  new AlertDialog.Builder(this).setTitle("Contest History").setView(b).setPositiveButton("CLOSE",null).show();
 }
 void helpSupport(){
  new AlertDialog.Builder(this).setTitle("Help & Support").setMessage("Need help with your Gigbiz account, work, payment or KYC?\n\nUse your registered email to contact support.").setPositiveButton("EMAIL SUPPORT",(d,w)->{Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:"));i.putExtra(Intent.EXTRA_SUBJECT,"Gigbiz Worker Support");try{startActivity(i);}catch(Exception e){toast("No email app found.");}}).setNegativeButton("CLOSE",null).show();
 }
 void shareProfile(DocumentSnapshot u){
  String n=u.getString("name");if(n==null)n="Gigbiz Worker";String text="Gigbiz Worker Profile\nName: "+n+"\nSkills: "+String.valueOf(u.get("skills")==null?"":u.get("skills"))+"\nLocation: "+String.valueOf(u.get("location")==null?"":u.get("location"))+"\nContact: "+String.valueOf(u.get("phone")==null?"":u.get("phone"));
  Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,text);startActivity(Intent.createChooser(i,"Share Your Card"));
 }
 void showInfo(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK",null).show();}
 void deleteAccountConfirm(DocumentSnapshot u){
  new AlertDialog.Builder(this).setTitle("Delete Account?").setMessage("Your Gigbiz profile and account will be permanently deleted. This cannot be undone. Continue?").setNegativeButton("CANCEL",null).setPositiveButton("DELETE",(d,w)->{
   String uid=auth.getCurrentUser().getUid();db.collection("users").document(uid).delete().addOnCompleteListener(x->{auth.getCurrentUser().delete().addOnCompleteListener(y->{if(y.isSuccessful()){getSharedPreferences("gigbiz_secure",MODE_PRIVATE).edit().clear().apply();toast("Account deleted.");login();}else toast("For security, please login again and retry account deletion.");});});
  }).show();
 }
 void topBar(LinearLayout r,String title){TextView b=tv("‹  "+title,20,true);b.setTextColor(purple);b.setPadding(dp(14),dp(15),0,dp(15));r.addView(b,new LinearLayout.LayoutParams(-1,dp(60)));b.setOnClickListener(v->{db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(this::home);});}
 String err(Exception e){return e==null?"Something went wrong":e.getMessage()==null?"Operation failed":e.getMessage();}
}