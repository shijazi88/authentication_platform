const { app, BrowserWindow, shell } = require("electron");
const { spawn } = require("node:child_process");
const path = require("node:path");
const http = require("node:http");

const isDev = !app.isPackaged;
const DAEMON_PORT = 9876;

let daemonProcess = null;

// Where the SecuGen capture daemon lives.
//   dev       — sibling project's debug build
//   prod      — bundled by electron-builder under process.resourcesPath
function resolveDaemonPath() {
  if (isDev) {
    return path.resolve(
      __dirname,
      "..",
      "..",
      "sanad-secugen-capture",
      "bin",
      "Debug",
      "net8.0-windows",
      "sanad-secugen-capture.exe",
    );
  }
  return path.join(
    process.resourcesPath,
    "sanad-secugen-capture",
    "sanad-secugen-capture.exe",
  );
}

// Probe before spawning so we don't double-launch when the user already has
// the daemon running in a terminal (common during development).
function isDaemonAlive() {
  return new Promise((resolve) => {
    const req = http.get(
      { host: "127.0.0.1", port: DAEMON_PORT, path: "/info", timeout: 1500 },
      (res) => {
        resolve(res.statusCode === 200);
        res.resume();
      },
    );
    req.on("error", () => resolve(false));
    req.on("timeout", () => {
      req.destroy();
      resolve(false);
    });
  });
}

async function startDaemon() {
  // SecuGen FDx SDK is Windows-only. On other platforms, the simulator
  // still runs (file upload + sample fingerprint), but device capture is
  // unavailable. No need to attempt a spawn that will always fail.
  if (process.platform !== "win32") {
    console.log("[daemon] non-Windows platform, skipping (SecuGen is Windows-only)");
    return;
  }
  if (await isDaemonAlive()) {
    console.log("[daemon] already running on :" + DAEMON_PORT + ", skipping spawn");
    return;
  }
  const exe = resolveDaemonPath();
  console.log("[daemon] launching", exe);
  try {
    daemonProcess = spawn(exe, [], {
      cwd: path.dirname(exe),
      detached: false,
      stdio: ["ignore", "pipe", "pipe"],
      windowsHide: true,
    });
    daemonProcess.stdout?.on("data", (d) =>
      console.log("[daemon]", String(d).trim()),
    );
    daemonProcess.stderr?.on("data", (d) =>
      console.error("[daemon ERR]", String(d).trim()),
    );
    daemonProcess.on("exit", (code, signal) => {
      console.log("[daemon] exited", { code, signal });
      daemonProcess = null;
    });
    daemonProcess.on("error", (err) => {
      console.error("[daemon] spawn error:", err.message);
      daemonProcess = null;
    });
  } catch (err) {
    console.error("[daemon] failed to spawn:", err);
  }
}

function stopDaemon() {
  if (daemonProcess && !daemonProcess.killed) {
    console.log("[daemon] stopping");
    try {
      daemonProcess.kill();
    } catch (err) {
      console.error("[daemon] kill error:", err);
    }
    daemonProcess = null;
  }
}

function createWindow() {
  const win = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 1000,
    minHeight: 700,
    title: "Sanad Bank Simulator",
    backgroundColor: "#f8fafc",
    titleBarStyle: "hiddenInset",
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: false,
    },
  });

  if (isDev) {
    win.loadURL("http://localhost:5173");
  } else {
    win.loadFile(path.join(__dirname, "../dist/index.html"));
  }

  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: "deny" };
  });
}

app.whenReady().then(async () => {
  await startDaemon();
  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("before-quit", stopDaemon);
app.on("window-all-closed", () => {
  stopDaemon();
  if (process.platform !== "darwin") app.quit();
});
