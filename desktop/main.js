const { app, BrowserWindow, session } = require("electron");
const path = require("path");

function createWindow(){
  const win = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 900,
    minHeight: 650,
    backgroundColor: "#f7f9fc",
    webPreferences: {
      contextIsolation: false,
      nodeIntegration: true
    }
  });
  win.loadFile(path.join(__dirname,"index.html"));
}

app.whenReady().then(()=>{
  session.defaultSession.setPermissionRequestHandler((webContents, permission, callback) => {
    callback(permission === "media" || permission === "camera" || permission === "microphone");
  });
  createWindow();
  app.on("activate",()=>{ if(BrowserWindow.getAllWindows().length===0) createWindow(); });
});
app.on("window-all-closed",()=>{ if(process.platform!=="darwin") app.quit(); });