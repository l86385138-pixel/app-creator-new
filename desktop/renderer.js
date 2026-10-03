const { initializeApp } = require("firebase/app");
const { getAuth, signInWithEmailAndPassword, createUserWithEmailAndPassword, onAuthStateChanged, signOut } = require("firebase/auth");
const { getFirestore, collection, getDocs, doc, getDoc, addDoc, setDoc, updateDoc, onSnapshot, query, where } = require("firebase/firestore");
const { getStorage, ref: storageRef, uploadBytes, getDownloadURL } = require("firebase/storage");
const cfg = require("./firebase-config");

let auth = null;
let db = null;
let storage = null;
let currentRole = "student";
let livePC = new Map();
let liveStream = null;
let liveRecorder = null;
let liveChunks = [];
let currentLiveRoom = null;
const root = document.getElementById("app");
let cachedCourses = [];

function showStartupError(err){
  const msg = err && (err.message || String(err));
  root.innerHTML = '<div class="login-page"><div class="login-card"><div class="login-logo">GG</div><h1>Gayan Ganga</h1><p>Student Login</p><div class="error">App startup error</div><div class="error" style="word-break:break-word">'+esc(msg)+'</div><button class="primary-btn" onclick="location.reload()">RETRY</button></div></div>';
}
window.addEventListener("error", e => showStartupError(e.error || e.message));
window.addEventListener("unhandledrejection", e => showStartupError(e.reason));

function esc(v){return String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));}
function isFree(x){return x.access==="free" || x.price==="0" || x.price===0 || !x.price;}

function login(){
  root.innerHTML='<div class="login-page"><div class="login-card"><div class="login-logo">GG</div><h1>Gayan Ganga</h1><p>Admin / Teacher Login</p><div class="muted" style="margin-bottom:14px">Windows app is only for Admin and Teacher.</div><input id="email" class="input" placeholder="Admin / Teacher Email"><input id="pass" type="password" class="input" placeholder="Password"><div id="msg" class="error"></div><button id="login" class="primary-btn">LOGIN</button></div></div>';
  document.getElementById("login").onclick=async()=>{
    const msg=document.getElementById("msg");
    try{msg.textContent="Please wait...";await signInWithEmailAndPassword(auth,document.getElementById("email").value.trim(),document.getElementById("pass").value);}
    catch(e){msg.textContent=e.message||"Login failed";}
  };
}
async function adminInvites(){
  const snap=await getDocs(query(collection(db,"users"),where("role","==","student")));
  let h='<div class="form-card"><h3>Existing Student को Teacher बनाएं</h3><input id="teacherUserSearch" class="input" placeholder="Student name या email खोजें..." oninput="filterTeacherCandidates()"><div class="muted">नया account नहीं बनेगा। उसी Student का existing email/password Teacher Login के लिए रहेगा।</div></div><div id="teacherCandidateList" class="course-grid">';
  snap.forEach(d=>{const x=d.data();h+='<article class="mini-card teacher-candidate" data-search="'+esc(((x.name||"")+" "+(x.email||"")).toLowerCase())+'"><span>👤</span><div><h3>'+esc(x.name||"Student")+'</h3><p>'+esc(x.email||"")+'</p><button class="primary-btn" onclick="promoteExistingUserToTeacher(\''+d.id+'\')">INVITE / MAKE TEACHER</button></div></article>';});
  h+='</div>';adminWrap("Teacher Invites",h||'<div class="empty-card">No Student users found.</div>');
}
function filterTeacherCandidates(){
  const q=(document.getElementById("teacherUserSearch")?.value||"").toLowerCase().trim();
  document.querySelectorAll(".teacher-candidate").forEach(el=>{el.style.display=!q||el.dataset.search.includes(q)?"":"none";});
}
async function createTeacherInvite(){const name=document.getElementById("invName")?.value.trim(),email=document.getElementById("invEmail")?.value.trim(),msg=document.getElementById("adminMsg");if(!name||!email){msg.textContent="Name और email दोनों भरें.";return;}try{await addDoc(collection(db,"teacherInvites"),{name,email,active:true,createdBy:auth.currentUser.uid,createdAt:new Date()});msg.textContent="Teacher invite created.";adminInvites();}catch(e){msg.textContent=e.message;}}
async function adminLiveRooms(){adminWrap("Live Rooms",'<div class="form-card"><input id="roomTitle" class="input" placeholder="Live class title"><input id="roomTeacherEmail" class="input" placeholder="Teacher email"><input id="roomSchedule" class="input" placeholder="Schedule e.g. Monday 10:00 AM"><input id="roomPassword" class="input" placeholder="Room password"><button class="primary-btn" onclick="createLiveRoom()">CREATE LIVE ROOM</button><div id="adminMsg" class="muted"></div></div><div id="roomList" class="course-grid">Loading...</div>');const snap=await getDocs(collection(db,"liveRooms"));let h="";snap.forEach(d=>{const x=d.data();h+='<article class="mini-card"><span>🔴</span><div><h3>'+esc(x.roomTitle||"Live Room")+'</h3><p>'+esc(x.teacherEmail||"Unassigned")+'</p><p>'+esc(x.schedule||"")+' · '+esc(x.status||"scheduled")+'</p></div></article>';});document.getElementById("roomList").innerHTML=h||'<div class="empty-card">No live rooms.</div>';}
async function createLiveRoom(){const title=document.getElementById("roomTitle")?.value.trim(),email=document.getElementById("roomTeacherEmail")?.value.trim(),schedule=document.getElementById("roomSchedule")?.value.trim(),password=document.getElementById("roomPassword")?.value,msg=document.getElementById("adminMsg");if(!title||!email||!password){msg.textContent="Title, teacher email और password जरूरी हैं.";return;}try{const teachers=await getDocs(query(collection(db,"users"),where("email","==",email),where("role","==","teacher")));let teacherName="",teacherUid="";teachers.forEach(d=>{teacherUid=d.id;teacherName=d.data().name||"";});if(!teacherUid){msg.textContent="इस email का active teacher account नहीं मिला.";return;}await addDoc(collection(db,"liveRooms"),{roomTitle:title,teacherUid,teacherName,teacherEmail:email,schedule,roomPassword:password,active:true,liveActive:false,status:"scheduled",createdBy:auth.currentUser.uid,createdAt:new Date()});msg.textContent="Live room created.";adminLiveRooms();}catch(e){msg.textContent=e.message;}}
async function adminCourses(){
  const snap=await getDocs(collection(db,"courses"));
  const teachers=await getDocs(query(collection(db,"users"),where("role","==","teacher")));
  let opts='<option value="">-- Select Teacher --</option>';
  teachers.forEach(d=>{const x=d.data();opts+='<option value="'+esc(d.id)+'" data-email="'+esc(x.email||"")+'" data-name="'+esc(x.name||"Teacher")+'">'+esc(x.name||"Teacher")+' — '+esc(x.email||"")+'</option>';});
  let h='<div class="form-card"><input id="courseTitle" class="input" placeholder="Course title"><input id="courseDesc" class="input" placeholder="Description"><select id="courseTeacher" class="input">'+opts+'</select><button class="primary-btn" onclick="createCourse()">CREATE COURSE</button><div id="adminMsg" class="muted"></div></div><div class="course-grid">';
  snap.forEach(d=>{const x=d.data();h+='<article class="course-card"><div class="course-placeholder">📚</div><div class="course-body"><span class="tag">FREE</span><h3>'+esc(x.title||"Course")+'</h3><p>'+esc(x.description||"")+'</p><p>Teacher: <b>'+esc(x.teacherName||"Not Assigned")+'</b></p><small>'+esc(x.teacherEmail||"")+'</small><br><small>ID: '+esc(d.id)+'</small></div></article>';});
  h+='</div>';adminWrap("Courses",h);
}
async function createCourse(){
  const title=document.getElementById("courseTitle")?.value.trim(),description=document.getElementById("courseDesc")?.value.trim(),sel=document.getElementById("courseTeacher"),msg=document.getElementById("adminMsg");
  if(!title){msg.textContent="Course title भरें.";return;}
  if(!sel?.value){msg.textContent="Teacher select करें.";return;}
  const teacherDoc=await getDoc(doc(db,"users",sel.value)); if(!teacherDoc.exists()||teacherDoc.data().role!=="teacher"){msg.textContent="Selected user Teacher नहीं है.";return;}
  const t=teacherDoc.data();
  try{
    await addDoc(collection(db,"courses"),{title,description,imageUrl:"",free:true,active:true,teacherUid:sel.value,teacherName:t.name||"Teacher",teacherEmail:t.email||"",createdAt:new Date()});
    msg.textContent="Course created and Teacher assigned.";
    adminCourses();
  }catch(e){msg.textContent=e.message;}
}
async function adminTests(){const snap=await getDocs(collection(db,"tests"));let h='<div class="course-grid">';snap.forEach(d=>{const x=d.data();h+='<article class="test-card"><span>📝</span><h3>'+esc(x.title||"Test")+'</h3><p>'+esc(x.duration||0)+' minutes</p><small>ID: '+esc(d.id)+'</small></article>';});h+='</div>';adminWrap("Tests",h||'<div class="empty-card">No tests.</div>');}
async function adminRecordings(){const snap=await getDocs(collection(db,"recordings"));let h='<div class="course-grid">';snap.forEach(d=>{const x=d.data();h+='<article class="mini-card"><span>🎥</span><div><h3>'+esc(x.title||"Recording")+'</h3><p>'+esc(x.status||"")+'</p>'+(x.url?'<a class="outline-btn" href="'+esc(x.url)+'" target="_blank">OPEN</a>':"")+'</div></article>';});h+='</div>';adminWrap("Recordings",h||'<div class="empty-card">No recordings.</div>');}
async function adminUsers(){
  const snap=await getDocs(collection(db,"users"));
  let h='<div class="form-card"><input id="userSearch" class="input" placeholder="Student name या email से खोजें..." oninput="filterAdminUsers()"><div class="muted">Existing Student को ही Teacher बनाया जाएगा। नया account नहीं बनेगा।</div></div><div id="adminUserList" class="course-grid">';
  snap.forEach(d=>{const x=d.data();h+='<article class="mini-card admin-user-card" data-search="'+esc(((x.name||"")+" "+(x.email||"")).toLowerCase())+'"><span>👤</span><div><h3>'+esc(x.name||"User")+'</h3><p>'+esc(x.email||"")+'</p><p>Role: <b>'+esc(x.role||"")+'</b></p>'+(x.role==="student"?'<button class="primary-btn" onclick="promoteExistingUserToTeacher(\''+d.id+'\')">INVITE AS TEACHER</button>':x.role==="teacher"?'<span class="tag">TEACHER</span>':"")+'</div></article>';});
  h+='</div>';adminWrap("Users",h||'<div class="empty-card">No users.</div>');
}
function filterAdminUsers(){
  const q=(document.getElementById("userSearch")?.value||"").toLowerCase().trim();
  document.querySelectorAll(".admin-user-card").forEach(el=>{el.style.display=!q||el.dataset.search.includes(q)?"":"none";});
}
async function promoteExistingUserToTeacher(uid){
  const ref=doc(db,"users",uid); const snap=await getDoc(ref);
  if(!snap.exists()){alert("User नहीं मिला.");return;}
  const x=snap.data();
  if(x.role!=="student"){alert("यह user Student नहीं है.");return;}
  if(!confirm((x.name||x.email||"Student")+" को Teacher बनाना है?\nइसका existing email/password वही रहेगा."))return;
  try{
    await updateDoc(ref,{role:"teacher",teacherInvited:true,teacherPromotedAt:new Date(),teacherPromotedBy:auth.currentUser.uid});
    await addDoc(collection(db,"teacherInvites"),{name:x.name||"",email:x.email||"",active:false,type:"existing-user-promoted",teacherUid:uid,createdBy:auth.currentUser.uid,createdAt:new Date(),activatedAt:new Date()});
    alert("Teacher बना दिया गया. अब यही email/password Windows app में Teacher Login के लिए काम करेगा.");
    adminUsers();
  }catch(e){alert("Teacher update failed: "+(e.message||e));}
}
async function loadTeacherDashboard(){
  shell('<section class="hero-title"><h1>Teacher Dashboard</h1><p>Assigned live classrooms</p></section><div id="teacherRooms" class="course-grid">Loading...</div>','Live');
  const el=document.getElementById("teacherRooms");
  try{
    const q=query(collection(db,"liveRooms"),where("teacherEmail","==",auth.currentUser.email));
    const s=await getDocs(q); let h="";
    s.forEach(d=>{
      const x=d.data();
      h+='<article class="course-card"><div class="course-placeholder">🔴</div><div class="course-body"><span class="tag">ASSIGNED</span><h3>'+esc(x.roomTitle||"Live Class")+'</h3><p>'+esc(x.schedule||"")+'</p><button class="primary-btn live-start" data-room="'+d.id+'">START LIVE CLASS</button></div></article>';
    });
    el.innerHTML=h||'<div class="empty-card">Admin ने अभी live class assign नहीं की है।</div>';
    el.querySelectorAll(".live-start").forEach(b=>b.onclick=()=>openTeacherLive(b.dataset.room));
  }catch(e){el.innerHTML='<div class="error">'+esc(e.message)+'</div>';}
}
async function openTeacherLive(roomId){
  const s=await getDoc(doc(db,"liveRooms",roomId)); if(!s.exists()){alert("Live room नहीं मिला");return;}
  const x=s.data(); const pw=prompt("Class Room Password"); if(pw===null)return;
  if(pw!==String(x.roomPassword||"")){alert("Wrong class room password");return;}
  currentLiveRoom=roomId;
  shell('<section class="hero-title"><h1>🔴 '+esc(x.roomTitle||"Live Class")+'</h1><p>'+esc(x.schedule||"")+'</p></section><div class="live-layout"><div class="live-main"><video id="localVideo" autoplay muted playsinline></video><div class="live-controls"><button id="startBtn" class="primary-btn">START LIVE</button><button id="endBtn" class="danger-btn" disabled>END LIVE</button></div><div id="liveMsg" class="muted">Camera और microphone allow करें।</div></div><div class="live-side"><h3>Students</h3><div id="count">0 connected</div></div></div>','Live');
  document.getElementById("startBtn").onclick=startTeacherLive;
  document.getElementById("endBtn").onclick=stopTeacherLive;
  try{
    liveStream=await navigator.mediaDevices.getUserMedia({video:true,audio:true});
    document.getElementById("localVideo").srcObject=liveStream;
  }catch(e){document.getElementById("liveMsg").textContent=e.message;}
}
async function startTeacherLive(){
  if(!liveStream||!currentLiveRoom)return;
  await updateDoc(doc(db,"liveRooms",currentLiveRoom),{liveActive:true,status:"live",startedAt:new Date()});
  document.getElementById("startBtn").disabled=true; document.getElementById("endBtn").disabled=false;
  liveChunks=[];
  try{liveRecorder=new MediaRecorder(liveStream,{mimeType:"video/webm;codecs=vp8,opus"});}catch(e){liveRecorder=new MediaRecorder(liveStream);}
  liveRecorder.ondataavailable=e=>{if(e.data.size)liveChunks.push(e.data);}; liveRecorder.start(1000);
  listenStudents();
}
function listenStudents(){
  const q=query(collection(db,"liveRooms",currentLiveRoom,"participants"),where("status","==","joining"));
  onSnapshot(q,s=>s.docChanges().forEach(ch=>{if(ch.type==="added")acceptStudent(ch.doc.id,ch.doc.data());}));
}
async function acceptStudent(pid,data){
  if(livePC.has(pid))return;
  const pc=new RTCPeerConnection({iceServers:[{urls:"stun:stun.l.google.com:19302"}]});
  livePC.set(pid,pc); liveStream.getTracks().forEach(t=>pc.addTrack(t,liveStream));
  pc.onicecandidate=e=>{if(e.candidate)addDoc(collection(db,"liveRooms",currentLiveRoom,"participants",pid,"teacherCandidates"),e.candidate.toJSON());};
  await pc.setRemoteDescription(new RTCSessionDescription(data.offer));
  const ans=await pc.createAnswer(); await pc.setLocalDescription(ans);
  await updateDoc(doc(db,"liveRooms",currentLiveRoom,"participants",pid),{answer:{type:ans.type,sdp:ans.sdp},status:"connected"});
  onSnapshot(collection(db,"liveRooms",currentLiveRoom,"participants",pid,"studentCandidates"),s=>s.docChanges().forEach(ch=>{if(ch.type==="added")pc.addIceCandidate(new RTCIceCandidate(ch.doc.data())).catch(()=>{});}));
  const n=document.getElementById("count");if(n)n.textContent=livePC.size+" connected";
}
async function stopTeacherLive(){
  if(liveRecorder&&liveRecorder.state!=="inactive")liveRecorder.stop();
  livePC.forEach(pc=>pc.close());livePC.clear();
  if(liveStream)liveStream.getTracks().forEach(t=>t.stop());
  if(currentLiveRoom)await updateDoc(doc(db,"liveRooms",currentLiveRoom),{liveActive:false,status:"ended",endedAt:new Date()});
  document.getElementById("endBtn").disabled=true;
  document.getElementById("liveMsg").textContent="Live ended. Recording save हो रही है...";
  setTimeout(saveLiveRecording,1200);
}
async function saveLiveRecording(){
  if(!liveChunks.length){document.getElementById("liveMsg").textContent="Recording data नहीं मिला।";return;}
  const blob=new Blob(liveChunks,{type:"video/webm"}); const stamp=Date.now();
  try{
    const path="live_recordings/"+auth.currentUser.uid+"/"+currentLiveRoom+"-"+stamp+".webm";
    const fr=storageRef(storage,path); await uploadBytes(fr,blob,{contentType:"video/webm"});
    const url=await getDownloadURL(fr);
    await addDoc(collection(db,"recordings"),{roomId:currentLiveRoom,teacherUid:auth.currentUser.uid,title:"Live Class "+stamp,url,filePath:path,status:"ready",createdAt:new Date()});
    const a=document.createElement("a");a.href=URL.createObjectURL(blob);a.download="Gayan-Ganga-Live-"+stamp+".webm";a.click();
    document.getElementById("liveMsg").innerHTML="Recording saved + downloaded. <a href='"+url+"' target='_blank'>Open recording</a>";
  }catch(e){document.getElementById("liveMsg").textContent="Recording save failed: "+e.message;}
}
async function loadLiveList(){
  const el=document.getElementById("liveList"); if(!el)return;
  try{
    const s=await getDocs(query(collection(db,"liveRooms"),where("liveActive","==",true))); let h="";
    s.forEach(d=>{const x=d.data();h+='<article class="course-card"><div class="course-placeholder">🔴</div><div class="course-body"><span class="tag">LIVE NOW</span><h3>'+esc(x.roomTitle||"Live Class")+'</h3><p>'+esc(x.schedule||"")+'</p><button class="primary-btn join-live" data-room="'+d.id+'">JOIN LIVE CLASS</button></div></article>';});
    el.innerHTML=h||'<div class="empty-card">अभी कोई live class नहीं चल रही है।</div>';
    el.querySelectorAll(".join-live").forEach(b=>b.onclick=()=>joinStudentLive(b.dataset.room));
  }catch(e){el.innerHTML='<div class="error">'+esc(e.message)+'</div>';}
}
async function joinStudentLive(roomId){
  const s=await getDoc(doc(db,"liveRooms",roomId));if(!s.exists()||!s.data().liveActive){alert("Live class अभी active नहीं है");return;}
  const x=s.data();
  shell('<section class="hero-title"><h1>🔴 '+esc(x.roomTitle||"Live Class")+'</h1><p>'+esc(x.schedule||"")+'</p></section><div class="student-live"><video id="remoteVideo" autoplay playsinline controls></video><div id="studentMsg" class="muted">Teacher से connect हो रहा है...</div><button id="leaveBtn" class="danger-btn">LEAVE CLASS</button></div>','Live');
  document.getElementById("leaveBtn").onclick=()=>{livePC.get("student")?.close();livePC.delete("student");loadHome();};
  currentLiveRoom=roomId;
  const p=await addDoc(collection(db,"liveRooms",roomId,"participants"),{uid:auth.currentUser.uid,email:auth.currentUser.email,status:"joining",joinedAt:new Date()});
  const pc=new RTCPeerConnection({iceServers:[{urls:"stun:stun.l.google.com:19302"}]});livePC.set("student",pc);
  pc.addTransceiver("video",{direction:"recvonly"});pc.addTransceiver("audio",{direction:"recvonly"});
  pc.ontrack=e=>{document.getElementById("remoteVideo").srcObject=e.streams[0];document.getElementById("studentMsg").textContent="LIVE — Teacher connected.";};
  pc.onicecandidate=e=>{if(e.candidate)addDoc(collection(db,"liveRooms",roomId,"participants",p.id,"studentCandidates"),e.candidate.toJSON());};
  onSnapshot(doc(db,"liveRooms",roomId,"participants",p.id),async d=>{const a=d.data()?.answer;if(a&&!pc.currentRemoteDescription)await pc.setRemoteDescription(new RTCSessionDescription(a));});
  onSnapshot(collection(db,"liveRooms",roomId,"participants",p.id,"teacherCandidates"),s=>s.docChanges().forEach(ch=>{if(ch.type==="added")pc.addIceCandidate(new RTCIceCandidate(ch.doc.data())).catch(()=>{});}));
  const offer=await pc.createOffer();await pc.setLocalDescription(offer);
  await updateDoc(doc(db,"liveRooms",roomId,"participants",p.id),{offer:{type:offer.type,sdp:offer.sdp}});
}

function shell(content,active="Home"){
  const admin=currentRole==="admin";
  const nav=admin
    ? '<button class="nav-link '+(active==="Admin"?"active":"")+'" onclick="loadAdminNotice()">Dashboard</button><button class="nav-link" onclick="adminInvites()">Invites</button><button class="nav-link" onclick="adminLiveRooms()">Live Rooms</button><button class="nav-link" onclick="adminCourses()">Courses</button>'
    : '<button class="nav-link '+(active==="Live"?"active":"")+'" onclick="loadTeacherDashboard()">Dashboard</button><button class="nav-link" onclick="loadTeacherDashboard()">Live Classes</button>';
  root.innerHTML = '<header class="navbar"><div class="nav-left"><div class="brand-mark">GG</div>'+nav+'</div><div class="nav-center"><div class="search"><span>⌕</span><input id="searchInput" placeholder="'+(admin?"Search admin":"Search live classes")+'"></div></div><div class="nav-right"><button class="icon-btn" onclick="toggleTheme()">☾</button><button class="profile" onclick="signOut(auth)">↪</button></div></header><main class="page">'+content+'</main>';
}
function showMessage(title){shell('<div class="empty-page"><div class="big-icon">▣</div><h2>'+esc(title)+'</h2><p class="muted">यह section अभी Free system के लिए तैयार किया जा रहा है।</p><button class="primary-btn" onclick="loadHome()">BACK HOME</button></div>',title);}
function showMore(){shell('<div class="empty-page"><h2>More</h2><div class="more-grid"><button onclick="loadTests()" class="browse-card">Free Tests</button><button onclick="loadNotes()" class="browse-card">PDF / Notes</button><button onclick="showProfile()" class="browse-card">My Profile</button><button onclick="signOut(auth)" class="browse-card">Logout</button></div></div>','More');}
function showProfile(){shell('<div class="empty-page"><div class="profile-large">L</div><h2>Student Profile</h2><p class="muted">'+esc(auth.currentUser?.email||"")+'</p><button class="primary-btn" onclick="signOut(auth)">LOGOUT</button></div>');}
function toggleTheme(){document.body.classList.toggle("dark");}
function showLive(){
  if(currentRole==="teacher"){loadTeacherDashboard();return;}
  shell('<section class="hero-title"><h1>Live Classes</h1><p>Current live classes from your coaching center</p></section><div id="liveList" class="course-grid">Loading...</div>','Live');
  loadLiveList();
}
async function loadLiveList(){
  const el=document.getElementById("liveList"); if(!el)return;
  try{const snap=await getDocs(collection(db,"courses"));let h="";
    snap.forEach(d=>{const x=d.data();if(x.liveActive&&x.liveUrl)h+=courseCard(d.id,x,true);});
    el.innerHTML=h||'<div class="empty-card">अभी कोई live class नहीं चल रही है।</div>';
  }catch(e){el.innerHTML='<div class="error">'+esc(e.message)+'</div>';}
}
function courseCard(id,x,live=false){
  const image=x.imageUrl?'<img src="'+esc(x.imageUrl)+'" onerror="this.style.display=\'none\'">':'<div class="course-placeholder">🎓</div>';
  return '<article class="course-card">'+image+'<div class="course-body"><span class="tag">'+(live?"🔴 LIVE":"FREE")+'</span><h3>'+esc(x.title||"Course")+'</h3><p>'+esc(x.description||"")+'</p><button class="primary-btn" onclick="openCourse(\''+id+'\',\''+esc(x.title||"Course").replace(/'/g,"\\'")+'\')">'+(live?"OPEN LIVE":"VIEW COURSE")+'</button></div></article>';
}
async function loadHome(){
  shell(`<section class="browse-head"><div><h1>Browse</h1><p>Learn from your coaching classes</p></div><div class="arrows"><button>←</button><button>→</button></div></section><div class="browse-grid"><button onclick="filterBrowse('paid')" class="browse-card"><span>🎓</span><b>Paid Classes</b></button><button onclick="showMessage('Purchases')" class="browse-card"><span>⇩</span><b>Purchases</b></button><button onclick="showLive()" class="browse-card"><span>▣</span><b>Live</b></button><button onclick="filterBrowse('free')" class="browse-card"><span>🎓</span><b>Free Courses</b></button></div><section class="featured"><div class="section-title"><h2>Featured</h2><div class="arrows"><button>←</button><button>→</button></div></div><div id="featured" class="course-grid">Loading...</div></section><section class="quick"><div class="section-title"><h2>Free Tests</h2><button class="outline-btn" onclick="loadTests()">VIEW ALL</button></div><div id="testPreview" class="course-grid">Loading...</div></section>`);
  try{
    const snap=await getDocs(collection(db,"courses"));cachedCourses=[];
    snap.forEach(d=>{const x=d.data();cachedCourses.push({id:d.id,x});});
    renderCourses(cachedCourses.filter(a=>isFree(a.x)),"featured");
    const ts=await getDocs(collection(db,"tests"));let h="";let n=0;ts.forEach(d=>{if(n++<4){const x=d.data();h+='<article class="mini-card"><span>📝</span><div><h3>'+esc(x.title||"Free Test")+'</h3><p>'+esc(x.description||"")+'</p></div><button class="outline-btn" onclick="openTest(\''+d.id+'\')">START</button></article>';}});document.getElementById("testPreview").innerHTML=h||'<div class="empty-card">अभी कोई test उपलब्ध नहीं है।</div>';
  }catch(e){document.getElementById("featured").innerHTML='<div class="error">'+esc(e.message)+'</div>';}
}
function renderCourses(items,target){const el=document.getElementById(target);if(!el)return;el.innerHTML=items.length?items.map(a=>courseCard(a.id,a.x)).join(""):'<div class="empty-card">अभी कोई free course उपलब्ध नहीं है।</div>';}
function filterCourses(q){q=q.toLowerCase();renderCourses(cachedCourses.filter(a=>isFree(a.x)&&((a.x.title||"").toLowerCase().includes(q)||(a.x.description||"").toLowerCase().includes(q))),"featured");}
function filterBrowse(type){renderCourses(cachedCourses.filter(a=>type==="free"?isFree(a.x):!isFree(a.x)),"featured");}
async function openCourse(id,title){
  shell('<div class="inner-head"><button class="back-btn" onclick="loadHome()">←</button><div><h1>'+esc(title)+'</h1><p>Course content</p></div></div><div class="course-tabs"><button onclick="loadSubjects(\''+id+'\')">Subjects</button><button onclick="loadCourseLive(\''+id+'\')">Live</button><button onclick="loadCourseNotes(\''+id+'\')">Notes</button></div><div id="courseContent" class="course-grid">Loading...</div>');
  loadSubjects(id);
}
async function loadSubjects(id){const el=document.getElementById("courseContent");el.innerHTML="Loading...";const snap=await getDocs(collection(db,"courses",id,"subjects"));let h="";snap.forEach(s=>{const x=s.data();h+='<article class="subject-card"><div class="subject-icon">📚</div><div><h3>'+esc(x.title||"Subject")+'</h3><button class="outline-btn" onclick="openSubject(\''+id+'\',\''+s.id+'\',\''+esc(x.title||"Subject").replace(/'/g,"\\'")+'\')">OPEN SUBJECT</button></div></article>';});el.innerHTML=h||'<div class="empty-card">अभी subjects add नहीं किए गए हैं।</div>';}
async function loadCourseLive(id){const el=document.getElementById("courseContent");const d=await getDoc(doc(db,"courses",id));const x=d.data()||{};el.innerHTML=x.liveActive&&x.liveUrl?'<article class="live-card"><span class="live-dot">● LIVE</span><h2>'+esc(x.liveTitle||"Live Class")+'</h2><p>'+esc(x.liveSchedule||"")+'</p><a class="primary-btn" href="'+esc(x.liveUrl)+'" target="_blank">JOIN LIVE CLASS</a></article>':'<div class="empty-card">अभी कोई live class नहीं चल रही है।</div>';}
async function loadCourseNotes(id){const el=document.getElementById("courseContent");const snap=await getDocs(collection(db,"notes"));let h="";snap.forEach(d=>{const x=d.data();if(x.courseId===id){h+='<article class="mini-card"><span>📄</span><div><h3>'+esc(x.title||"Notes")+'</h3><p>'+esc(x.description||"")+'</p></div>'+(x.downloadUrl?'<a class="outline-btn" href="'+esc(x.downloadUrl)+'" target="_blank">OPEN PDF</a>':"")+'</article>';}});el.innerHTML=h||'<div class="empty-card">इस course के लिए अभी notes नहीं हैं।</div>';}
async function openSubject(cid,sid,title){shell('<div class="inner-head"><button class="back-btn" onclick="openCourse(\''+cid+'\',\'Course\')">←</button><div><h1>'+esc(title)+'</h1><p>Recorded video classes</p></div></div><div id="lessons" class="course-grid">Loading...</div>');const el=document.getElementById("lessons");const snap=await getDocs(collection(db,"courses",cid,"subjects",sid,"lessons"));let h="";snap.forEach(d=>{const x=d.data();h+='<article class="video-card"><div class="video-thumb">▶</div><div class="course-body"><h3>'+esc(x.title||"Video Class")+'</h3><p>'+esc(x.description||"")+'</p>'+(x.videoUrl?'<a class="primary-btn" href="'+esc(x.videoUrl)+'" target="_blank">WATCH VIDEO</a>':"")+'</div></article>';});el.innerHTML=h||'<div class="empty-card">अभी video classes नहीं हैं।</div>';}
async function loadTests(){shell('<div class="inner-head"><button class="back-btn" onclick="loadHome()">←</button><div><h1>Free Tests</h1><p>Practice and check your preparation</p></div></div><div id="allTests" class="course-grid">Loading...</div>');const el=document.getElementById("allTests");const snap=await getDocs(collection(db,"tests"));let h="";snap.forEach(d=>{const x=d.data();h+='<article class="test-card"><span>📝</span><h3>'+esc(x.title||"Test")+'</h3><p>'+esc(x.description||"")+'</p><button class="primary-btn" onclick="openTest(\''+d.id+'\')">START TEST</button></article>';});el.innerHTML=h||'<div class="empty-card">अभी कोई test उपलब्ध नहीं है।</div>';}
async function loadNotes(){shell('<div class="inner-head"><button class="back-btn" onclick="loadHome()">←</button><div><h1>PDF / Notes</h1><p>Study material</p></div></div><div id="allNotes" class="course-grid">Loading...</div>');const el=document.getElementById("allNotes");const snap=await getDocs(collection(db,"notes"));let h="";snap.forEach(d=>{const x=d.data();h+='<article class="mini-card"><span>📄</span><div><h3>'+esc(x.title||"Notes")+'</h3><p>'+esc(x.description||"")+'</p></div>'+(x.downloadUrl?'<a class="outline-btn" href="'+esc(x.downloadUrl)+'" target="_blank">OPEN PDF</a>':"")+'</article>';});el.innerHTML=h||'<div class="empty-card">अभी notes उपलब्ध नहीं हैं।</div>';}
async function openTest(testId){const qSnap=await getDocs(collection(db,"tests",testId,"questions"));let qs=[];qSnap.forEach(d=>qs.push(d.data()));let i=0,score=0;function render(){if(i>=qs.length){shell('<div class="result-card"><div class="result-icon">✓</div><h1>Test Completed</h1><h2>'+score+' / '+qs.length+'</h2><p>Your test has been completed.</p><button class="primary-btn" onclick="loadHome()">BACK HOME</button></div>');return;}const q=qs[i];shell('<div class="test-screen"><div class="test-top"><button class="back-btn" onclick="loadHome()">←</button><span>Question '+(i+1)+' of '+qs.length+'</span></div><div class="question-card"><h2>'+esc(q.question)+'</h2><label><input type="radio" name="a" value="1"> '+esc(q.option1)+'</label><label><input type="radio" name="a" value="2"> '+esc(q.option2)+'</label><label><input type="radio" name="a" value="3"> '+esc(q.option3)+'</label><label><input type="radio" name="a" value="4"> '+esc(q.option4)+'</label><button id="next" class="primary-btn">'+(i===qs.length-1?"SUBMIT":"NEXT")+'</button></div></div>');document.getElementById("next").onclick=()=>{const a=document.querySelector('input[name="a"]:checked');if(!a)return;if(a.value===String(q.answer||q.correctAnswer))score++;i++;render();};}render();}
window.filterAdminUsers=filterAdminUsers;window.promoteExistingUserToTeacher=promoteExistingUserToTeacher;window.loadAdminNotice=loadAdminNotice;window.adminTeachers=adminTeachers;window.adminInvites=adminInvites;window.createTeacherInvite=createTeacherInvite;window.adminLiveRooms=adminLiveRooms;window.createLiveRoom=createLiveRoom;window.adminCourses=adminCourses;window.createCourse=createCourse;window.adminTests=adminTests;window.adminRecordings=adminRecordings;window.adminUsers=adminUsers;window.loadHome=loadHome;window.openCourse=openCourse;window.openSubject=openSubject;window.openTest=openTest;window.loadTests=loadTests;window.loadNotes=loadNotes;window.showLive=showLive;window.showProfile=showProfile;window.showMore=showMore;window.showMessage=showMessage;window.toggleTheme=toggleTheme;window.filterCourses=filterCourses;window.filterBrowse=filterBrowse;window.loadCourseLive=loadCourseLive;window.loadCourseNotes=loadCourseNotes;window.loadSubjects=loadSubjects;
function boot(){
  try{
    login();
    const firebaseApp = initializeApp(cfg);
    auth = getAuth(firebaseApp);
    db = getFirestore(firebaseApp);
    storage = getStorage(firebaseApp);
    onAuthStateChanged(auth,async u=>{
      if(!u){currentRole="";login();return;}
      const udoc=await getDoc(doc(db,"users",u.uid));
      currentRole=udoc.exists()&&udoc.data().role||"";
      if(currentRole==="teacher"){
        loadTeacherDashboard();
      }else if(currentRole==="admin"){
        loadAdminNotice();
      }else{
        await signOut(auth);
        currentRole="";
        login();
        const msg=document.getElementById("msg");
        if(msg) msg.textContent="Student account Windows app में allowed नहीं है. Student के लिए Android app इस्तेमाल करें.";
      }
    });
  }catch(e){
    showStartupError(e);
  }
}
boot();