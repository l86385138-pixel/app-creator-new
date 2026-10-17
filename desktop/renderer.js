// REBUILD WINDOWS ORIGINAL INSTALLER
// STORAGE-FREE ADMIN PATCH: offers use Firestore text, marketing uses external HTTPS links, withdrawals store transaction references.
const { initializeApp } = require('firebase/app');
const { getAuth, signInWithEmailAndPassword, onAuthStateChanged, signOut } = require('firebase/auth');
const { getFirestore, collection, getDocs, getDoc, doc, addDoc, updateDoc, deleteDoc, query, where, serverTimestamp, runTransaction, onSnapshot } = require('firebase/firestore');
const { getStorage, ref: storageRef, uploadBytes, getDownloadURL } = require('firebase/storage');
const cfg = require('./firebase-config');
const fb = initializeApp(cfg); const auth=getAuth(fb); const db=getFirestore(fb); const storage = getStorage(fb); const root=document.getElementById('app');
const esc=v=>String(v==null?'':v).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const css='<style>
*{box-sizing:border-box}html,body{margin:0;min-height:100%;font-family:Arial,sans-serif;background:#f5f6fb;color:#202331}button,input,textarea,select{font:inherit}
.top{min-height:72px;background:#fff;border-bottom:1px solid #e2e5ec;display:flex;align-items:center;padding:12px 24px;gap:18px;position:sticky;top:0;z-index:10;box-shadow:0 2px 8px #18223b08}
.brand{font-size:24px;font-weight:900;color:#6d3abe;white-space:nowrap}.spacer{flex:1;min-width:12px}.top .muted{white-space:nowrap}
.wrap{max-width:1600px;margin:0 auto;padding:20px}.app-layout{display:grid;grid-template-columns:220px minmax(0,1fr);gap:20px;align-items:start}
.nav{display:flex;flex-direction:column;gap:8px;background:#fff;border:1px solid #e3e6ed;border-radius:14px;padding:12px;position:sticky;top:92px}
.btn{padding:10px 13px;border:0;border-radius:9px;cursor:pointer;font-weight:700;line-height:1.25;transition:background .15s ease}.nav .btn{width:100%;text-align:left;margin:0;white-space:normal}.top .btn{width:auto;flex:0 0 auto;margin:0}
.primary{background:#6d3abe;color:#fff}.primary:hover{background:#592ca2}.secondary{background:#eef0f5;color:#303545}.secondary:hover{background:#e2e5ed}.danger{background:#fee2e2;color:#a11}.success{background:#dcfce7;color:#166534}
.panel,.card{background:#fff;border:1px solid #e1e4e9;border-radius:14px;padding:18px;margin-bottom:16px;box-shadow:0 2px 8px #18223b05}.content{min-width:0}.content h1{margin:2px 0 16px;font-size:27px}.content h2{margin-top:0}
.grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}
.input,.select,.textarea{width:100%;padding:11px;border:1px solid #ccd2dc;border-radius:9px;background:#fff;min-width:0}.textarea{min-height:95px;resize:vertical}.full{grid-column:1/-1}
.tablebox{overflow:auto}.table{width:100%;border-collapse:collapse;min-width:850px;background:#fff}.table th,.table td{padding:10px;border-bottom:1px solid #eee;text-align:left;vertical-align:top}.table th{background:#f7f7fb;font-size:13px;color:#505568}
.thumb{width:50px;height:50px;object-fit:cover;border-radius:9px}.muted{color:#737783;font-size:13px}.login{max-width:420px;margin:40px auto;background:#fff;padding:30px;border-radius:18px;border:1px solid #ddd}
@media(max-width:1100px){.grid{grid-template-columns:repeat(2,minmax(0,1fr))}.app-layout{grid-template-columns:190px minmax(0,1fr)}}
@media(max-width:800px){.top{padding:12px 14px;gap:10px;flex-wrap:wrap}.brand{font-size:20px}.wrap{padding:12px}.app-layout{grid-template-columns:1fr}.nav{position:static;display:grid;grid-template-columns:repeat(3,minmax(0,1fr));padding:8px}.nav .btn{text-align:center;padding:10px 6px}.form{grid-template-columns:1fr}.full{grid-column:auto}}
@media(max-width:520px){.grid{grid-template-columns:1fr}.nav{grid-template-columns:repeat(2,minmax(0,1fr))}.top .muted{font-size:12px}.content h1{font-size:23px}}
</style>';
let current='dashboard';
let withdrawalsUnsubscribe=null;
const nav=[['dashboard','📊 Dashboard'],['users','👤 Users'],['projects','📁 Projects'],['gigs','💼 Gigs'],['services','🛠 Services'],['categories','🗂 Categories'],['workers','👥 Workers'],['applications','📋 Applications'],['leads','🎯 Leads'],['tasks','✅ Tasks'],['withdrawals','💰 Withdrawals'],['trainings','🎓 Training'],['marketing','📣 Marketing'],['offers','🖼 Offers']];
function login(msg=''){root.innerHTML=css+'<div class="login"><div class="brand">GIGBIZ</div><h1>Admin Control Center</h1><p class="muted">Windows Admin App — Admin only</p><input id="email" class="input" value="klav0628@gmail.com" placeholder="Admin email"><input id="pass" class="input" type="password" placeholder="Admin password"><br><button class="btn primary" onclick="doLogin()">LOGIN</button><p id="err" style="color:#b42318">'+esc(msg)+'</p></div>';}
window.doLogin=async()=>{const emailEl=document.getElementById('email'),passEl=document.getElementById('pass'),errEl=document.getElementById('err');try{errEl.textContent='Signing in...';await signInWithEmailAndPassword(auth,emailEl.value.trim(),passEl.value)}catch(e){errEl.textContent=e.message}};
function shell(body){let n=nav.map(x=>'<button class="btn '+(current===x[0]?'primary':'secondary')+'" onclick="go(\\''+x[0]+'\\')">'+x[1]+'</button>').join('');root.innerHTML=css+'<div class="top"><div class="brand">GIGBIZ ADMIN</div><div class="spacer"></div><span class="muted">'+esc(auth.currentUser?.email||'')+'</span><button class="btn danger" onclick="logout()">Logout</button></div><div class="wrap"><div class="app-layout"><aside class="nav" aria-label="Admin navigation">'+n+'</aside><main class="content">'+body+'</main></div></div>';}
window.logout=()=>signOut(auth);
window.go=async id=>{current=id;try{await (({dashboard,users,projects,gigs,services,categories,workers,applications,leads,tasks,withdrawals,trainings,marketing,offers}[id])||dashboard)()}catch(e){console.error(e);alert('Module error: '+(e?.message||e))}};
async function admin(){if(!auth.currentUser)return login();const u=await getDoc(doc(db,'users',auth.currentUser.uid));if(!u.exists()||u.data().role!=='admin'){await signOut(auth);return login('This account is not an Admin. Firestore users/'+auth.currentUser.uid+' must have role = admin.')}dashboard();}
async function count(c){try{return(await getDocs(collection(db,c))).size}catch(e){return 0}}
async function dashboard(){current='dashboard';const a=[['users','👥','Users'],['projects','📁','Projects'],['gigs','💼','Gigs'],['services','🛠','Services'],['applications','📋','Applications'],['leads','🎯','Leads'],['tasks','✅','Tasks'],['withdrawals','💰','Withdrawals']];let h='';for(const x of a)h+='<div class="card"><div>'+x[1]+'</div><div class="muted">'+x[2]+'</div><h2>'+await count(x[0])+'</h2></div>';shell('<h1>Gigbiz Admin Dashboard</h1><p class="muted">Manage workers, projects, gigs, services, leads, tasks and withdrawals.</p><div class="grid">'+h+'</div>')}
const val=id=>document.getElementById(id)?.value?.trim()||'';
async function upload(){return ''; // Storage-free mode: logos are optional and not uploaded.
}
function form(fields,save){let h='<div class="panel"><h2>Add New</h2><div class="form">';fields.forEach(f=>{let id=f[0],lab=f[1],type=f[2]||'text';if(type==='textarea')h+='<div class="full"><label class="muted">'+lab+'</label><textarea id="'+id+'" class="textarea"></textarea></div>';else h+='<div><label class="muted">'+lab+'</label><input id="'+id+'" class="input" type="'+type+'"></div>'});h+='<div class="full"><label class="muted">Logo / image</label><input id="logoFile" class="input" type="file" accept="image/*"></div></div><br><button class="btn primary" onclick="'+save+'()">SAVE</button></div>';return h}
function table(title,headers,rows){let h='<div class="panel"><h2>'+title+'</h2><div class="tablebox"><table class="table"><tr>'+headers.map(x=>'<th>'+x+'</th>').join('')+'</tr>';rows.forEach(r=>h+='<tr>'+r.map(x=>'<td>'+x+'</td>').join('')+'</tr>');return h+'</table></div></div>'}
async function users(){current='users';let s=await getDocs(collection(db,'users')),rows=[];s.forEach(d=>{let x=d.data(),self=d.id===auth.currentUser.uid;rows.push(['<b>'+esc(x.name||'User')+'</b>',esc(x.email||''),esc(x.phone||''),'<b>'+esc(x.role||'worker')+'</b>',esc(x.status||'active'),self?'Current Admin':'<button class="btn success" onclick="userStatus(\''+d.id+'\',\'active\')">Active</button> <button class="btn secondary" onclick="userStatus(\''+d.id+'\',\'blocked\')">Block</button> <button class="btn danger" onclick="userDelete(\''+d.id+'\')">Delete</button> <button class="btn secondary" onclick="resetWalletPin(\''+d.id+'\')">Reset Wallet PIN</button>']);});shell('<h1>Users</h1><p class="muted">Manage worker/customer accounts. Your current admin account cannot be deleted here.</p>'+table('All Users',['User','Email','Mobile','Role','Status','Actions'],rows))}
window.userStatus=async(id,status)=>{try{await updateDoc(doc(db,'users',id),{status,updatedAt:serverTimestamp()});users()}catch(e){alert('User update failed: '+e.message)}};
window.resetWalletPin=async(id)=>{if(id===auth.currentUser.uid)return alert('You cannot reset your own admin Wallet PIN here.');if(!confirm('Reset this user Wallet PIN? The old PIN cannot be viewed. The user will set a new PIN at next withdrawal.'))return;try{await updateDoc(doc(db,'users',id),{withdrawPinSalt:'',withdrawPinHash:'',withdrawPinUpdatedAt:serverTimestamp()});alert('Wallet PIN reset. Ask the user to open Withdraw and set a new 6-digit PIN.');users()}catch(e){alert('PIN reset failed: '+e.message)}};
window.userDelete=async(id)=>{if(id===auth.currentUser.uid)return alert('Current admin cannot be deleted.');if(!confirm('Delete this user?'))return;try{await deleteDoc(doc(db,'users',id));users()}catch(e){alert('User delete failed: '+e.message)}};
async function projects(){
 current='projects';
 const cats=await getDocs(collection(db,'categories'));let opts='<option value="">Choose category</option>';
 cats.forEach(d=>{const x=d.data();opts+='<option value="'+d.id+'">'+esc(x.name||'Category')+'</option>';});
 const snap=await getDocs(collection(db,'projects'));const rows=[];
 snap.forEach(d=>{const x=d.data();rows.push([esc(x.title||''),esc(x.categoryName||x.category||''),esc(x.client||''),esc(x.workerId||''),esc(x.applyUrl||''),'<button class="btn danger" data-id="'+d.id+'" onclick="deleteProjectBtn(this)">Delete</button>']);});
 shell('<h1>Projects</h1><div class="panel"><h2>Add Project</h2><div class="form"><input id="title" class="input" placeholder="Project title"><select id="projectCategoryId" class="input">'+opts+'</select><textarea id="description" class="textarea full" placeholder="Description"></textarea><input id="client" class="input" placeholder="Client"><input id="workerId" class="input" placeholder="Worker ID (optional)"><input id="link" class="input" placeholder="Project link"><input id="applyUrl" class="input" placeholder="Customer final URL after submit"></div><br><button class="btn primary" onclick="addProject()">ADD PROJECT</button></div>'+table('All Projects',['Project','Category','Client','Worker','Final URL','Action'],rows));
}
window.addProject=async()=>{try{const title=val('title'),categoryId=val('projectCategoryId');if(!title)throw new Error('Project title is required.');if(!categoryId)throw new Error('Please choose a category.');const cat=await getDoc(doc(db,'categories',categoryId));if(!cat.exists())throw new Error('Selected category was not found.');const x=cat.data();await addDoc(collection(db,'projects'),{title,categoryId,categoryName:x.name||'',description:val('description'),client:val('client'),workerId:val('workerId'),link:val('link'),applyUrl:val('applyUrl'),status:'active',createdAt:serverTimestamp(),createdBy:auth.currentUser.uid});alert('Project added under '+(x.name||'selected category'));projects();}catch(e){alert('Project save failed: '+e.message);}};
window.deleteProjectBtn=async el=>{const id=el.getAttribute('data-id');if(!confirm('Delete this project?'))return;try{await deleteDoc(doc(db,'projects',id));projects();}catch(e){alert('Delete failed: '+e.message);}};
async function gigs(){current='gigs';let s=await getDocs(collection(db,'gigs')),rows=[];s.forEach(d=>{let x=d.data();rows.push([x.logoUrl?'<img class="thumb" src="'+esc(x.logoUrl)+'">':'—','<b>'+esc(x.title)+'</b>', '₹'+esc(x.pay||'0'),esc(x.location||''),esc(x.applyUrl||''),'<button class="btn danger" onclick="del(\'gigs\',\''+d.id+'\')">Delete</button>'])});shell('<h1>Gigs / Jobs</h1>'+form([['title','Gig title'],['description','Description','textarea'],['pay','Pay'],['location','Location'],['category','Category'],['serviceId','Service ID'],['applyUrl','Customer final URL after submit']],'addGig')+table('All Gigs',['Logo','Gig','Pay','Location','Final URL','Action'],rows))}
window.addGig=async()=>{try{let logoUrl=await upload();await addDoc(collection(db,'gigs'),{title:val('title'),description:val('description'),pay:val('pay'),location:val('location'),category:val('category'),serviceId:val('serviceId'),applyUrl:val('applyUrl'),logoUrl,status:'active',createdAt:serverTimestamp(),createdBy:auth.currentUser.uid});alert('Gig added');gigs()}catch(e){alert(e.message)}};
async function services(){
 current='services';
 const cats=await getDocs(collection(db,'categories'));let opts='<option value="">Choose category</option>';
 cats.forEach(d=>{const x=d.data();opts+='<option value="'+d.id+'">'+esc(x.name||'Category')+'</option>';});
 const snap=await getDocs(collection(db,'services'));const rows=[];
 snap.forEach(d=>{const x=d.data();rows.push([esc(x.name||''),esc(x.categoryName||x.category||''),esc(x.price||''),esc(x.status||'active'),'<button class="btn success" data-id="'+d.id+'" data-status="active" onclick="setServiceStatusBtn(this)">Active</button> <button class="btn secondary" data-id="'+d.id+'" data-status="blocked" onclick="setServiceStatusBtn(this)">Block</button> <button class="btn danger" data-id="'+d.id+'" onclick="deleteServiceBtn(this)">Delete</button>']);});
 shell('<h1>Services</h1><div class="panel"><h2>Add Service</h2><div class="form"><input id="name" class="input" placeholder="Service name"><select id="serviceCategoryId" class="input">'+opts+'</select><textarea id="description" class="textarea full" placeholder="Description"></textarea><input id="price" class="input" placeholder="Price / earning"></div><br><button class="btn primary" onclick="addService()">ADD SERVICE</button></div>'+table('All Services',['Service','Category','Price','Status','Actions'],rows));
}
window.addService=async()=>{try{const name=val('name'),categoryId=val('serviceCategoryId');if(!name)throw new Error('Service name is required.');if(!categoryId)throw new Error('Please choose a category.');const cat=await getDoc(doc(db,'categories',categoryId));if(!cat.exists())throw new Error('Selected category was not found.');const x=cat.data();await addDoc(collection(db,'services'),{name,categoryId,categoryName:x.name||'',description:val('description'),price:val('price'),status:'active',createdAt:serverTimestamp(),createdBy:auth.currentUser.uid});alert('Service added under '+(x.name||'selected category'));services();}catch(e){alert('Service save failed: '+e.message);}};
window.setServiceStatusBtn=el=>serviceStatus(el.getAttribute('data-id'),el.getAttribute('data-status'));
window.deleteServiceBtn=async el=>{const id=el.getAttribute('data-id');if(!confirm('Delete this service?'))return;try{await deleteDoc(doc(db,'services',id));services();}catch(e){alert('Delete failed: '+e.message);}};
async function categories(){
 current='categories';
 const snap=await getDocs(collection(db,'categories')); const rows=[];
 snap.forEach(d=>{const x=d.data(); rows.push([esc(x.icon||'📂'),esc(x.name||''),esc(x.description||''),'<button class="btn danger" data-del="'+d.id+'" onclick="deleteCategoryFromButton(this)">Delete</button>']);});
 shell('<h1>Categories</h1><div class="panel"><h2>Add Category</h2><div class="form"><input id="categoryName" class="input" placeholder="Category name"><input id="categoryIcon" class="input" placeholder="Icon (optional)"><textarea id="categoryDescription" class="textarea full" placeholder="Description (optional)"></textarea></div><br><button class="btn primary" onclick="addCategory()">ADD CATEGORY</button></div>'+table('All Categories',['Icon','Category','Description','Action'],rows));
}
window.addCategory=async()=>{try{const name=val('categoryName');if(!name)throw new Error('Category name is required.');await addDoc(collection(db,'categories'),{name,icon:val('categoryIcon')||'📂',description:val('categoryDescription'),status:'active',createdAt:serverTimestamp(),createdBy:auth.currentUser.uid});alert('Category added.');categories();}catch(e){alert('Category save failed: '+e.message);}};
window.deleteCategoryFromButton=async el=>{const id=el.getAttribute('data-del');if(!confirm('Delete this category?'))return;try{await deleteDoc(doc(db,'categories',id));categories();}catch(e){alert('Delete failed: '+e.message);}};
async function workers(){current='workers';let s=await getDocs(query(collection(db,'users'),where('role','==','worker'))),rows=[];s.forEach(d=>{let x=d.data();rows.push(['<b>'+esc(x.name||'Worker')+'</b>',esc(x.phone||''),esc(x.email||''),esc(x.status||'active'),'<button class="btn success" onclick="workerStatus(\''+d.id+'\',\'active\')">Active</button> <button class="btn danger" onclick="workerStatus(\''+d.id+'\',\'blocked\')">Block</button>'])});shell('<h1>Workers</h1>'+table('Worker Accounts',['Worker','Mobile','Email','Status','Actions'],rows))}
window.workerStatus=async(id,status)=>{try{await updateDoc(doc(db,'users',id),{status});workers()}catch(e){alert(e.message)}};
async function applications(){current='applications';let s=await getDocs(collection(db,'applications')),rows=[];s.forEach(d=>{let x=d.data();rows.push([esc(x.workerEmail||x.workerId||''),esc(x.gigTitle||x.gigId||''),esc(x.status||'pending'),'<button class="btn success" onclick="appStatus(\''+d.id+'\',\'approved\')">Approve</button> <button class="btn danger" onclick="appStatus(\''+d.id+'\',\'rejected\')">Reject</button>'])});shell('<h1>Applications</h1>'+table('Applications',['Worker','Gig','Status','Actions'],rows))}
window.appStatus=async(id,status)=>{try{await updateDoc(doc(db,'applications',id),{status,updatedAt:serverTimestamp()});applications()}catch(e){alert(e.message)}};
async function leads(){
 current='leads';
 const s=await getDocs(collection(db,'leads'));
 let rows=[];
 s.forEach(d=>{
   const x=d.data();
   const st=x.status||'interview_pending';
   const id=d.id;
   const actions='<button class="btn secondary" onclick="leadStatus(\''+id+'\',\'interview_pending\')">Interview Pending</button> '+
     '<button class="btn primary" onclick="verifyLead(\''+id+'\')">Verify & Credit</button> '+
     '<button class="btn success" onclick="leadStatus(\''+id+'\',\'complete\')">Complete</button> '+
     '<button class="btn danger" onclick="leadStatus(\''+id+'\',\'reject\')">Reject</button>';
   rows.push([
     esc(x.customerName||x.name||''),
     esc(x.phone||x.mobile||''),
     esc(x.pincode||''),
     esc(x.workerId||''),
     '₹'+esc(x.taskAmount||'0'),
     '<b>'+esc(st)+'</b>',
     actions
   ]);
 });
 shell('<h1>Leads</h1><p class="muted">Interview Pending → Verify → Complete / Reject. Verify credits the worker wallet once.</p>'+
   table('Customer Leads',['Name','Mobile','Pincode','Worker','Task Amount','Status','Actions'],rows));
}
window.leadStatus=async(id,status)=>{
 try{
   await updateDoc(doc(db,'leads',id),{status,updatedAt:serverTimestamp(),updatedBy:auth.currentUser.uid});
   leads();
 }catch(e){alert('Lead update failed: '+e.message);}
};
window.verifyLead=async(id)=>{
 try{
  const preview=(await getDoc(doc(db,'leads',id))).data();
  if(!preview)throw new Error('Lead not found.');
  if(preview.walletCredited===true)throw new Error('This lead is already credited.');
  const amount=Number(preview.taskAmount||0);
  if(!Number.isFinite(amount)||amount<=0)throw new Error('Invalid task amount.');
  const memberId=String(preview.memberId||'').trim();
  let workerMargin=amount, memberEarning=0;
  if(memberId){
    const proposed=prompt('Approved payout ₹'+amount+'\\nEnter worker margin in ₹. Remaining amount goes to the assigned member.',String(Math.round(amount/2*100)/100));
    if(proposed===null)return;
    workerMargin=Number(proposed);
    if(!Number.isFinite(workerMargin)||workerMargin<0||workerMargin>amount)throw new Error('Worker margin must be between 0 and ₹'+amount+'.');
    memberEarning=Math.round((amount-workerMargin)*100)/100;
  }
  if(!confirm('Approve lead?\\nWorker margin: ₹'+workerMargin+'\\nMember earning: ₹'+memberEarning+'\\nTotal: ₹'+amount))return;
  await runTransaction(db,async(t)=>{
   const lr=doc(db,'leads',id), leadSnap=await t.get(lr), lead=leadSnap.data();
   if(!lead)throw new Error('Lead not found.');
   if(lead.walletCredited===true)throw new Error('This lead is already credited.');
   const workerId=lead.workerId;
   if(!workerId)throw new Error('Worker ID missing.');
   const ur=doc(db,'users',workerId), userSnap=await t.get(ur), user=userSnap.data();
   if(!user)throw new Error('Worker account not found.');
   const actual=Number(lead.taskAmount||0);
   if(actual!==amount)throw new Error('Lead payout changed. Refresh and try again.');
   t.update(ur,{wallet:Number(user.wallet||0)+workerMargin,updatedAt:serverTimestamp()});
   if(memberId){
    const mr=doc(db,'users',memberId), ms=await t.get(mr), member=ms.data();
    if(!member || member.role!=='member')throw new Error('Assigned member account is missing or is not a member login.');
    if(member.ownerId && member.ownerId!==workerId)throw new Error('This member does not belong to the lead owner.');
    t.update(mr,{wallet:Number(member.wallet||0)+memberEarning,updatedAt:serverTimestamp()});
   }
   t.update(lr,{status:'verify',walletCredited:true,workerMargin,memberEarning,creditedAt:serverTimestamp(),verifiedAt:serverTimestamp(),verifiedBy:auth.currentUser.uid,updatedAt:serverTimestamp()});
  });
  alert('Lead approved. Worker margin ₹'+workerMargin+' and member earning ₹'+memberEarning+' credited.');
  leads();
 }catch(e){alert('Verify failed: '+e.message);}
};
async function tasks(){current='tasks';let s=await getDocs(collection(db,'tasks')),rows=[];s.forEach(d=>{let x=d.data();rows.push([esc(x.title||x.name||''),esc(x.workerId||''),esc(x.memberId||'—'),esc(x.status||'pending'),'<button class="btn success" onclick="taskStatus(\''+d.id+'\',\'completed\')">Complete</button>'])});shell('<h1>Tasks / Assignments</h1><div class="panel"><input id="taskTitle" class="input" placeholder="Task title"><input id="taskWorker" class="input" placeholder="Worker UID (team owner)"><input id="taskMember" class="input" placeholder="Member login UID (optional)"><textarea id="taskDesc" class="textarea" placeholder="Task description and onboarding guidelines"></textarea><br><button class="btn primary" onclick="addTask()">ASSIGN TASK</button></div>'+table('Tasks',['Task','Worker UID','Member UID','Status','Action'],rows))}
window.addTask=async()=>{try{const title=val('taskTitle'),workerId=val('taskWorker'),memberId=val('taskMember'),description=val('taskDesc');if(!title||(!workerId&&!memberId))throw new Error('Task title and a worker or member UID are required.');await addDoc(collection(db,'tasks'),{title,workerId,memberId:memberId||'',description,status:'pending',createdAt:serverTimestamp(),createdBy:auth.currentUser.uid});tasks()}catch(e){alert('Task assignment failed: '+e.message)}};
window.taskStatus=async(id,status)=>{try{await updateDoc(doc(db,'tasks',id),{status,updatedAt:serverTimestamp()});tasks()}catch(e){alert(e.message)}};
async function withdrawals(){
 current='withdrawals';
 if(withdrawalsUnsubscribe){withdrawalsUnsubscribe();withdrawalsUnsubscribe=null;}
 shell('<h1>Withdrawal Requests</h1><p class="muted">Live updates are enabled. Approve moves a request to Processing. Upload the actual transfer receipt, then record the UTR to mark Paid. Reject returns the reserved wallet balance.</p><div id="withdrawalLiveError" class="muted"></div><div id="withdrawalTable"></div>');
 const holder=document.getElementById('withdrawalTable');
 const errorBox=document.getElementById('withdrawalLiveError');
 withdrawalsUnsubscribe=onSnapshot(collection(db,'withdrawals'),snap=>{
   if(!holder||!holder.isConnected)return;
   errorBox.textContent='';
   let rows=[];
   snap.forEach(d=>{
     const x=d.data(),status=String(x.status||'pending'),method=String(x.paymentMethod||'BANK ACCOUNT'),transfer=String(x.transferType||'NEFT');
     const bank=method==='UPI'?'<b>UPI</b><br>'+esc(x.upiId||''):'<b>'+esc(x.bankName||'')+'</b><br>A/C '+esc(x.accountNumber||'')+'<br>IFSC '+esc(x.ifscCode||x.ifsc||'');
     const proof=x.proofUrl?'<a target="_blank" rel="noreferrer" href="'+esc(x.proofUrl)+'">View/download slip</a>':'<span style="color:#b45309">No receipt uploaded</span>';
     const ref=x.paymentReference?'<br><b>UTR:</b> '+esc(x.paymentReference):'';
     const actions=(status==='pending'?'<button class="btn success" data-id="'+d.id+'" data-status="processing" onclick="wdStatus(this.dataset.id,this.dataset.status)">Approve Request</button> ':'')+
       ((status==='pending'||status==='processing')?'<button class="btn danger" data-id="'+d.id+'" data-status="rejected" onclick="wdStatus(this.dataset.id,this.dataset.status)">Reject / Return Balance</button> ':'')+
       ((status==='processing'||status==='approved')?'<button class="btn secondary" data-id="'+d.id+'" onclick="wdProof(this.dataset.id)">Upload payment slip</button> ':'')+
       ((status==='processing'||status==='approved')?'<button class="btn primary" data-id="'+d.id+'" onclick="wdCompletePayment(this.dataset.id)">Payment Complete / Done</button> ':'')+
       ((status==='processing'||status==='approved')&&x.proofUrl?'<button class="btn secondary" data-id="'+d.id+'" onclick="wdMarkPaid(this.dataset.id)">Record UTR + Mark Paid</button> ':'');
     rows.push([esc(x.workerEmail||x.workerId||''),esc(d.id),'₹'+esc(x.amount||'0'),bank,esc(status.toUpperCase())+'<br>'+esc(method)+' • '+esc(transfer),proof+ref,'<button class="btn secondary" onclick="wdDetails(\''+d.id+'\')">Full Details</button> '+(actions||'—')]);
   });
   holder.innerHTML=table('Withdrawal Requests',['Worker / UID','Request ID','Amount','Bank Details','Status','Payment Receipt / UTR','Actions'],rows);
 },err=>{
   if(errorBox)errorBox.textContent='Could not load live withdrawals: '+(err?.message||err);
 });
}
window.wdDetails=async(id)=>{try{const snap=await getDoc(doc(db,'withdrawals',id));if(!snap.exists())throw new Error('Withdrawal not found.');const x=snap.data();const method=String(x.paymentMethod||'BANK ACCOUNT');const lines=['STATUS: '+String(x.status||'pending').toUpperCase(),'Amount: ₹'+String(x.amount||0),'Request ID: '+id,'Adviser UID: '+String(x.workerId||''),'Email: '+String(x.workerEmail||'—'),'Payment method: '+method,'Transfer type: '+String(x.transferType||'—'),'Account holder: '+String(x.accountHolder||'—'),method==='UPI'?'UPI ID: '+String(x.upiId||'—'):'Bank: '+String(x.bankName||'—')+'\\nAccount: '+String(x.accountNumber||'—')+'\\nIFSC: '+String(x.ifscCode||x.ifsc||'—'),'Created: '+String(x.createdAt||'—'),'UTR: '+String(x.paymentReference||'—'),'Admin note: '+String(x.adminNote||'—'),'Payment slip: '+String(x.proofFileName||(x.proofUrl?'Uploaded':'Not uploaded'))];alert(lines.join('\\n\\n'));if(x.proofUrl&&confirm('Open uploaded payment slip?'))require('electron').shell.openExternal(x.proofUrl);}catch(e){alert('Could not load withdrawal details: '+e.message)}};
window.wdProof=async(id)=>{
 const input=document.createElement('input');input.type='file';input.accept='.jpg,.jpeg,.png,.webp,.pdf,image/jpeg,image/png,image/webp,application/pdf';
 input.onchange=async()=>{
  const file=input.files&&input.files[0];if(!file)return;
  if(file.size>8*1024*1024){alert('Receipt must be 8 MB or smaller.');return;}
  const valid=file.type==='application/pdf'||['image/jpeg','image/png','image/webp'].includes(file.type)||/\\.(pdf|jpe?g|png|webp)$/i.test(file.name);
  if(!valid){alert('Choose a JPG, PNG, WEBP or PDF receipt.');return;}
  try{
   if(!auth.currentUser)throw new Error('Admin session expired. Please sign in again.');
   const safeName=file.name.replace(/[^a-zA-Z0-9._-]/g,'_');
   const path='withdrawals/'+id+'/'+Date.now()+'_'+safeName;
   const target=storageRef(storage,path);
   await uploadBytes(target,file,{contentType:file.type||(/\\.pdf$/i.test(file.name)?'application/pdf':'image/jpeg'),customMetadata:{withdrawalId:id,uploadedBy:auth.currentUser.uid}});
   const url=await getDownloadURL(target);
   await updateDoc(doc(db,'withdrawals',id),{proofUrl:url,proofPath:path,proofFileName:file.name,proofUploadedAt:serverTimestamp(),proofUploadedBy:auth.currentUser.uid,updatedAt:serverTimestamp()});
   alert('Payment slip uploaded and linked to this withdrawal. The mobile app will update automatically.');
  }catch(e){alert('Payment slip upload failed: '+(e?.code?e.code+' — ':'')+(e?.message||e)+'\\n\\nCheck that Firebase Storage is enabled and the latest storage.rules are published.');}
 };
 document.body.appendChild(input);input.style.position='fixed';input.style.left='-10000px';input.click();
 input.addEventListener('change',()=>setTimeout(()=>input.remove(),1000),{once:true});
};
window.wdMarkPaid=async(id)=>{const ref=prompt('Enter the bank UTR / transaction reference after the transfer has actually succeeded:');if(ref===null)return;const value=ref.trim();if(value.length<6||value.length>100)return alert('Enter a valid UTR/reference (6–100 characters).');if(!confirm('Confirm the bank transfer is completed and the receipt is uploaded?'))return;try{await runTransaction(db,async(t)=>{const wr=doc(db,'withdrawals',id);const snap=await t.get(wr);if(!snap.exists())throw new Error('Withdrawal not found.');const wd=snap.data();if(!['approved','processing'].includes(wd.status))throw new Error('Approve the request first.');if(!wd.proofUrl)throw new Error('Upload the payment receipt first.');const patch={status:'paid',paymentReference:value,paymentReferenceRecordedAt:serverTimestamp(),paidAt:serverTimestamp(),paidBy:auth.currentUser.uid,updatedAt:serverTimestamp(),processedBy:auth.currentUser.uid};t.update(wr,patch);});alert('Payment reference recorded.');withdrawals()}catch(e){alert('Could not record payment: '+e.message)}};
window.wdCompletePayment=async(id)=>{if(!confirm('Confirm the bank/UPI transfer has actually been completed. The worker app will show PAYMENT COMPLETE • DONE.'))return;const reference=prompt('Enter UTR / transaction reference (optional). Leave blank if unavailable:');if(reference===null)return;const value=reference.trim();if(value&&(value.length<6||value.length>100))return alert('UTR/reference must be 6–100 characters, or leave it blank.');try{await runTransaction(db,async(t)=>{const wr=doc(db,'withdrawals',id);const snap=await t.get(wr);if(!snap.exists())throw new Error('Withdrawal not found.');const wd=snap.data();if(!['processing','approved'].includes(wd.status))throw new Error('Approve the request first.');const patch={status:'paid',paymentCompletedAt:serverTimestamp(),paymentCompletedBy:auth.currentUser.uid,paidAt:serverTimestamp(),paidBy:auth.currentUser.uid,updatedAt:serverTimestamp(),processedBy:auth.currentUser.uid};if(value){patch.paymentReference=value;patch.paymentReferenceRecordedAt=serverTimestamp();}t.update(wr,patch);});alert('Payment marked Complete / Done. The mobile app should update automatically.');withdrawals()}catch(e){alert('Could not complete payment: '+(e?.code?e.code+' — ':'')+e.message+'\\nCheck that the updated Firestore rules are published.')}};
window.wdStatus=async(id,status)=>{if(!['processing','rejected'].includes(status))return alert('Invalid status.');try{await runTransaction(db,async(t)=>{const wr=doc(db,'withdrawals',id);const snap=await t.get(wr);if(!snap.exists())throw new Error('Withdrawal not found.');const wd=snap.data();if(!['pending','processing'].includes(wd.status))throw new Error('This request is already finalized.');if(status==='processing'&&wd.status!=='pending')throw new Error('Request is already approved or processing.');const patch={status,updatedAt:serverTimestamp(),processedBy:auth.currentUser.uid};if(status==='processing')Object.assign(patch,{approvedAt:serverTimestamp(),approvedBy:auth.currentUser.uid});if(status==='rejected'){const uid=wd.workerId;const ur=doc(db,'users',uid);const us=await t.get(ur);if(!us.exists())throw new Error('Worker account not found.');const u=us.data();t.update(ur,{wallet:Number(u.wallet||0)+Number(wd.amount||0),updatedAt:serverTimestamp()});Object.assign(patch,{rejectedAt:serverTimestamp(),rejectedBy:auth.currentUser.uid});}t.update(wr,patch);});withdrawals()}catch(e){alert('Withdrawal update failed: '+e.message)}};

async function marketing(){current='marketing';let s=await getDocs(collection(db,'marketingMaterials')),rows=[];s.forEach(d=>{let x=d.data();rows.push([esc(x.title||''),esc(x.type||'Link'),esc(x.url||''),esc(x.description||''),'<a target="_blank" rel="noopener" href="'+esc(x.url||'#')+'">Open / Download</a> <button class="btn danger" onclick="del(\'marketingMaterials\',\''+d.id+'\')">Delete</button>'])});shell('<h1>Marketing Materials</h1><div class="panel"><h2>Add Material Link</h2><p class="muted">Storage-free: paste a public HTTPS link to a PDF, image, video or document.</p><div class="form"><input id="marketingTitle" class="input" placeholder="Material title"><input id="marketingType" class="input" placeholder="Type: PDF / Video / Image"><input id="marketingUrl" class="input full" placeholder="Public HTTPS link"><textarea id="marketingDescription" class="textarea full" placeholder="Description"></textarea></div><br><button class="btn primary" onclick="addMarketing()">SAVE MATERIAL</button></div>'+table('Materials',['Title','Type','Link','Description','Actions'],rows))}
window.addMarketing=async()=>{try{const title=val('marketingTitle'),url=val('marketingUrl');if(!title||!url)throw new Error('Title and URL required.');if(!/^https:\/\//i.test(url))throw new Error('URL must start with https://');await addDoc(collection(db,'marketingMaterials'),{title,type:val('marketingType')||'Link',url,description:val('marketingDescription'),status:'active',createdAt:serverTimestamp(),createdBy:auth.currentUser.uid});marketing()}catch(e){alert('Material save failed: '+e.message)}};
async function offers(){
 current='offers';
 const snap=await getDocs(collection(db,'offers')); const rows=[];
 snap.forEach(d=>{const x=d.data();const link=String(x.linkUrl||'');const open=link?'<button class="btn secondary" data-url="'+esc(link)+'" onclick="openOfferLink(this)">Open Link</button>':'—';rows.push([esc(x.title||''),esc(x.description||''),esc(link),esc(x.status||'active'),open+' <button class="btn danger" data-del="'+d.id+'" onclick="deleteOfferFromButton(this)">Delete</button>']);});
 shell('<h1>Offers</h1><div class="panel"><h2>Add Offer</h2><p class="muted">Paste a public HTTPS page link. Images are not required.</p><div class="form"><input id="offerTitle" class="input" placeholder="Offer title"><input id="offerLinkUrl" class="input" placeholder="Offer page link (https://...)"><textarea id="offerDescription" class="textarea full" placeholder="Description"></textarea></div><br><button class="btn primary" onclick="addOffer()">SAVE OFFER</button></div>'+table('All Offers',['Title','Description','Page Link','Status','Actions'],rows));
}
window.addOffer=async()=>{try{const title=val('offerTitle'),linkUrl=val('offerLinkUrl');if(!title)throw new Error('Offer title is required.');if(linkUrl&&!/^https:\/\//i.test(linkUrl))throw new Error('Offer link must start with https://');await addDoc(collection(db,'offers'),{title,description:val('offerDescription'),linkUrl,status:'active',createdAt:serverTimestamp(),createdBy:auth.currentUser.uid});alert('Offer saved.');offers();}catch(e){alert('Offer save failed: '+e.message);}};
window.openOfferLink=el=>{const u=el.getAttribute('data-url');if(u&&/^https:\/\//i.test(u))require('electron').shell.openExternal(u);else alert('Invalid HTTPS link.');};
window.deleteOfferFromButton=async el=>{const id=el.getAttribute('data-del');if(!confirm('Delete this offer?'))return;try{await deleteDoc(doc(db,'offers',id));offers();}catch(e){alert('Delete failed: '+e.message);}};
async function trainings(){current='trainings';let s=await getDocs(collection(db,'trainings')),rows=[];s.forEach(d=>{let x=d.data();rows.push([esc(x.title||''),esc(x.schedule||''),esc(x.joinUrl||''),esc(x.status||'active'),'<button class="btn danger" onclick="del(\'trainings\',\''+d.id+'\')">Delete</button>'])});shell('<h1>Scheduled Training</h1><div class="panel"><h2>Add Training Session</h2><div class="form"><input id="trainingTitle" class="input" placeholder="Training title"><input id="trainingSchedule" class="input" placeholder="Schedule e.g. Sunday 6 PM"><input id="trainingJoinUrl" class="input full" placeholder="Join URL (https://...)"><textarea id="trainingDescription" class="textarea full" placeholder="Description"></textarea></div><br><button class="btn primary" onclick="addTraining()">SAVE TRAINING</button></div>'+table('Training Sessions',['Title','Schedule','Join URL','Status','Action'],rows))}
window.addTraining=async()=>{try{const title=val('trainingTitle'),schedule=val('trainingSchedule'),joinUrl=val('trainingJoinUrl'),description=val('trainingDescription');if(!title||!schedule||!joinUrl)throw new Error('Title, schedule and join URL are required.');if(!/^https:\/\//i.test(joinUrl))throw new Error('Join URL must start with https://');await addDoc(collection(db,'trainings'),{title,schedule,joinUrl,description,status:'active',createdAt:serverTimestamp(),createdBy:auth.currentUser.uid});trainings()}catch(e){alert('Training save failed: '+e.message)}};
window.del=async(c,id)=>{if(!confirm('Delete this record?'))return;try{await deleteDoc(doc(db,c,id));go(current)}catch(e){alert(e.message)}};
login('Checking sign-in status…');
onAuthStateChanged(auth,u=>{if(u){admin().catch(e=>{console.error('Admin startup failed:',e);login('Could not open Admin: '+(e?.message||e));});}else{login();}});