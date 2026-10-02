const { initializeApp } = require("firebase/app");
const { getAuth, signInWithEmailAndPassword, onAuthStateChanged, signOut } = require("firebase/auth");
const { getFirestore, collection, getDocs, doc, getDoc, query, orderBy } = require("firebase/firestore");
const cfg = require("./firebase-config");

const app = initializeApp(cfg);
const auth = getAuth(app);
const db = getFirestore(app);
const root = document.getElementById("app");

function esc(v){return String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));}
function layout(content){
  root.innerHTML = '<div class="top"><div class="brand">GAYAN GANGA COCHING CENTER</div><button class="btn gray" id="logout">LOGOUT</button></div><div class="wrap">'+content+'</div>';
  document.getElementById("logout").onclick=()=>signOut(auth);
}
function login(){
  root.innerHTML='<div class="login"><h1>Student Login</h1><p class="muted">Laptop/Desktop Student App</p><input id="email" class="input" placeholder="Email"><input id="pass" type="password" class="input" placeholder="Password"><div id="msg" class="error"></div><button id="login" class="btn">LOGIN</button></div>';
  document.getElementById("login").onclick=async()=>{
    const msg=document.getElementById("msg");
    try{msg.textContent="Please wait...";await signInWithEmailAndPassword(auth,document.getElementById("email").value.trim(),document.getElementById("pass").value); }
    catch(e){msg.textContent=e.message||"Login failed";}
  };
}
async function loadHome(){
  layout('<h2>Student Dashboard</h2><p class="muted">Free learning content</p><div id="content">Loading...</div>');
  const c=document.getElementById("content");
  try{
    const courses=await getDocs(collection(db,"courses"));
    let html='<div class="section"><h2>Free Courses</h2><div class="grid">';
    courses.forEach(d=>{const x=d.data();if(x.access==="free"||x.price==="0"||!x.price)html+='<div class="card"><h3>'+esc(x.title||"Course")+'</h3><p>'+esc(x.description||"")+'</p><button class="btn" onclick="openCourse(\''+d.id+'\',\''+esc(x.title||"Course").replace(/'/g,"\\'")+'\')">OPEN COURSE</button></div>';});
    html+='</div></div><div class="section"><h2>Free Tests</h2><div id="tests">Loading...</div></div><div class="section"><h2>PDF / Notes</h2><div id="notes">Loading...</div></div>';
    c.innerHTML=html;
    await loadTests(); await loadNotes();
  }catch(e){c.innerHTML='<div class="error">'+esc(e.message)+'</div>';}
}
async function loadTests(){
  const el=document.getElementById("tests"); const snap=await getDocs(collection(db,"tests"));
  let h='<div class="grid">'; snap.forEach(d=>{const x=d.data();h+='<div class="card"><h3>'+esc(x.title||"Test")+'</h3><p>'+esc(x.description||"")+'</p><button class="btn" onclick="openTest(\''+d.id+'\')">START TEST</button></div>';}); h+='</div>'; el.innerHTML=h;
}
async function loadNotes(){
  const el=document.getElementById("notes"); const snap=await getDocs(collection(db,"notes"));
  let h='<div class="grid">'; snap.forEach(d=>{const x=d.data();const u=x.downloadUrl||"";h+='<div class="card"><h3>📄 '+esc(x.title||"Notes")+'</h3><p>'+esc(x.description||"")+'</p>'+(u?'<a class="btn" href="'+esc(u)+'" target="_blank">OPEN PDF</a>':"")+'</div>';}); h+='</div>';el.innerHTML=h;
}
async function openCourse(id,title){
  layout('<div class="row"><button class="btn gray" onclick="loadHome()">← BACK</button><h2>'+esc(title)+'</h2></div><div id="courseContent">Loading...</div>');
  const el=document.getElementById("courseContent"); const subs=await getDocs(collection(db,"courses",id,"subjects"));
  let h='<div class="grid">';
  subs.forEach(s=>{const x=s.data();h+='<div class="card"><h3>'+esc(x.title||"Subject")+'</h3><button class="btn" onclick="openSubject(\''+id+'\',\''+s.id+'\',\''+esc(x.title||"Subject").replace(/'/g,"\\'")+'\')">OPEN SUBJECT</button></div>';});
  h+='</div><div class="section"><div class="card" id="liveBox">Checking live class...</div></div>';el.innerHTML=h;
  const c=await getDoc(doc(db,"courses",id));const x=c.data()||{};
  document.getElementById("liveBox").innerHTML=x.liveActive&&x.liveUrl?'<h3>🔴 '+esc(x.liveTitle||"Live Class")+'</h3><p>'+esc(x.liveSchedule||"")+'</p><a class="btn" href="'+esc(x.liveUrl)+'" target="_blank">OPEN LIVE CLASS</a>':'<h3>Live Class</h3><p class="muted">No live class running now.</p>';
}
async function openSubject(cid,sid,title){
  layout('<div class="row"><button class="btn gray" onclick="openCourse(\''+cid+'\',\'Course\')">← BACK</button><h2>'+esc(title)+'</h2></div><div id="lessons">Loading...</div>');
  const el=document.getElementById("lessons");const snap=await getDocs(collection(db,"courses",cid,"subjects",sid,"lessons"));let h='<div class="grid">';
  snap.forEach(d=>{const x=d.data();h+='<div class="card"><h3>▶ '+esc(x.title||"Video")+'</h3>'+(x.videoUrl?'<a class="btn" href="'+esc(x.videoUrl)+'" target="_blank">WATCH VIDEO</a>':"")+'</div>';});
  h+='</div>';el.innerHTML=h;
}
async function openTest(testId){
  const qSnap=await getDocs(collection(db,"tests",testId,"questions"));let qs=[];qSnap.forEach(d=>qs.push({id:d.id,...d.data()}));
  let i=0,score=0;
  function render(){
    if(i>=qs.length){layout('<h2>Test Result</h2><div class="card"><h2>'+score+' / '+qs.length+'</h2><p>Test completed.</p><button class="btn" onclick="loadHome()">BACK HOME</button></div>');return;}
    const q=qs[i];layout('<h2>Free Test</h2><div class="card"><h3>Q'+(i+1)+'. '+esc(q.question)+'</h3><label><input type="radio" name="a" value="1"> '+esc(q.option1)+'</label><br><label><input type="radio" name="a" value="2"> '+esc(q.option2)+'</label><br><label><input type="radio" name="a" value="3"> '+esc(q.option3)+'</label><br><label><input type="radio" name="a" value="4"> '+esc(q.option4)+'</label><br><button id="next" class="btn">'+(i===qs.length-1?"SUBMIT":"NEXT")+'</button></div>');
    document.getElementById("next").onclick=()=>{const a=document.querySelector('input[name="a"]:checked');if(!a)return; if(a.value===String(q.answer||q.correctAnswer))score++;i++;render();};
  }
  render();
}
window.loadHome=loadHome;window.openCourse=openCourse;window.openSubject=openSubject;window.openTest=openTest;
onAuthStateChanged(auth,u=>u?loadHome():login());