import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Monitor, Maximize2, RefreshCw, RotateCw, Smartphone, Tablet, Terminal, Trash2 } from 'lucide-react';

interface IdeTheme {
  id: string;
  name: string;
  bg: string;
  sidebar: string;
  text: string;
  muted: string;
  accent: string;
  border: string;
}

const IDE_THEMES: IdeTheme[] = [
  { id: 'dark-plus', name: 'Dark+', bg: '#1E1E1E', sidebar: '#252526', text: '#D4D4D4', muted: '#858585', accent: '#007ACC', border: '#333333' },
  { id: 'one-dark', name: 'One Dark Pro', bg: '#282C34', sidebar: '#21252B', text: '#ABB2BF', muted: '#5C6370', accent: '#61AFEF', border: '#323842' },
  { id: 'dracula', name: 'Dracula', bg: '#282A36', sidebar: '#21222C', text: '#F8F8F2', muted: '#6272A4', accent: '#BD93F9', border: '#383A4B' },
  { id: 'monokai', name: 'Monokai', bg: '#272822', sidebar: '#1E1F1C', text: '#F8F8F2', muted: '#75715E', accent: '#A6E22E', border: '#3A3B34' },
  { id: 'light-plus', name: 'Light+', bg: '#FFFFFF', sidebar: '#F3F3F3', text: '#1E293B', muted: '#64748B', accent: '#007ACC', border: '#E2E8F0' },
  { id: 'github-light', name: 'GitHub Light', bg: '#FFFFFF', sidebar: '#F6F8FA', text: '#24292F', muted: '#57606A', accent: '#0969DA', border: '#D0D7DE' },
];

type ViewportPreset = 'fit' | 'mobile' | 'tablet' | 'desktop' | 'custom';
type TabFile = 'index.html' | 'styles.css' | 'app.js' | 'MainActivity.kt';

interface ConsoleMessageItem {
  id: number;
  level: 'log' | 'warn' | 'error';
  message: string;
}

const PRESET_DIMS: Record<Exclude<ViewportPreset, 'fit' | 'custom'>, { w: number; h: number }> = {
  mobile: { w: 390, h: 844 },
  tablet: { w: 768, h: 1024 },
  desktop: { w: 1280, h: 800 },
};

const INITIAL_FILES: Record<TabFile, string> = {
  'index.html': `<main class="shell">
  <header class="topbar">
    <strong>Vcode Studio</strong>
    <span id="bpLabel">Mobile Layout</span>
  </header>
  <section class="grid">
    <article class="card">
      <h1>Responsive WebView</h1>
      <p>Switch between Mobile (390px), Tablet (768px), and Desktop (1280px) to trigger real CSS media breakpoints.</p>
      <button id="counterBtn">Clicked 0 times</button>
    </article>
    <aside class="panel">
      <h2>Viewport Breakpoint</h2>
      <p id="sizeReadout">Measuring viewport...</p>
    </aside>
  </section>
</main>`,
  'styles.css': `* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  background: #0f172a;
  color: #f8fafc;
  font-family: system-ui, -apple-system, sans-serif;
  padding: 16px;
}
.shell { max-width: 1120px; margin: 0 auto; }
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 10px;
  margin-bottom: 16px;
  font-size: 13px;
}
.grid { display: grid; grid-template-columns: 1fr; gap: 16px; }
.card, .panel {
  padding: 20px;
  border-radius: 12px;
  background: #1e293b;
  border: 1px solid #334155;
}
h1 { margin: 0 0 8px; font-size: 20px; }
h2 { margin: 0 0 8px; font-size: 15px; color: #38bdf8; }
p { margin: 0; font-size: 14px; line-height: 1.5; color: #cbd5e1; }
button {
  margin-top: 14px;
  padding: 10px 16px;
  border: 0;
  border-radius: 8px;
  background: #007acc;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}
@media (min-width: 768px) {
  .grid { grid-template-columns: 1.4fr 1fr; }
}
@media (min-width: 1024px) {
  body { padding: 28px; }
  h1 { font-size: 26px; }
}`,
  'app.js': `let count = 0;
const btn = document.getElementById('counterBtn');
const bp = document.getElementById('bpLabel');
const readout = document.getElementById('sizeReadout');

function updateViewportInfo() {
  const w = window.innerWidth;
  const h = window.innerHeight;
  const mode = w >= 1024 ? 'Desktop (>=1024px)' : w >= 768 ? 'Tablet (>=768px)' : 'Mobile (<768px)';
  if (bp) bp.textContent = mode;
  if (readout) readout.textContent = w + 'px × ' + h + 'px CSS viewport';
}
updateViewportInfo();
window.addEventListener('resize', updateViewportInfo);

if (btn) {
  btn.addEventListener('click', () => {
    count += 1;
    btn.textContent = 'Clicked ' + count + (count === 1 ? ' time' : ' times');
    console.log('Button click count:', count);
  });
}`,
  'MainActivity.kt': `class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      VCodeApp(viewModel = viewModel())
    }
  }
}`,
};

export function IdePlayground() {
  const [selectedThemeId, setSelectedThemeId] = useState<string>('dark-plus');
  const [activeFile, setActiveFile] = useState<TabFile>('index.html');
  const [files, setFiles] = useState<Record<TabFile, string>>(INITIAL_FILES);
  const [viewport, setViewport] = useState<ViewportPreset>('mobile');
  const [customWidth, setCustomWidth] = useState<number>(480);
  const [customHeight, setCustomHeight] = useState<number>(720);
  const [isLandscape, setIsLandscape] = useState<boolean>(false);
  const [zoomToFit, setZoomToFit] = useState<boolean>(true);
  const [refreshCount, setRefreshCount] = useState<number>(0);
  const [stageSize, setStageSize] = useState<{ w: number; h: number }>({ w: 480, h: 380 });
  const stageRef = useRef<HTMLDivElement | null>(null);

  const [consoleLogs, setConsoleLogs] = useState<ConsoleMessageItem[]>([]);
  const [terminalInput, setTerminalInput] = useState<string>('');
  const [terminalLines, setTerminalLines] = useState<string[]>([
    '$ vcode --version',
    'Vcode Studio v1.0.0 (Build 100) · Android API 26-35',
    '$ git status --short',
    'M index.html  M styles.css',
  ]);

  const theme = IDE_THEMES.find((t) => t.id === selectedThemeId) ?? IDE_THEMES[0];

  useEffect(() => {
    const el = stageRef.current;
    if (!el) return;
    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        if (width > 0 && height > 0) {
          setStageSize({ w: width, h: height });
        }
      }
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    const onMsg = (ev: MessageEvent) => {
      if (ev.data && ev.data.__vcodeConsole) {
        const level = (ev.data.level || 'log') as 'log' | 'warn' | 'error';
        const message = String(ev.data.message || '');
        setConsoleLogs((prev) => [...prev.slice(-24), { id: Date.now() + Math.random(), level, message }]);
      }
    };
    window.addEventListener('message', onMsg);
    return () => window.removeEventListener('message', onMsg);
  }, []);

  const previewSrcDoc = useMemo(() => {
    const consoleBridge = `<script>(function(){const send=(lvl,args)=>{try{parent.postMessage({__vcodeConsole:true,level:lvl,message:Array.from(args).map(a=>typeof a==='object'?JSON.stringify(a):String(a)).join(' ')},'*');}catch(e){}};const oLog=console.log,oWarn=console.warn,oErr=console.error;console.log=function(){send('log',arguments);oLog.apply(console,arguments);};console.warn=function(){send('warn',arguments);oWarn.apply(console,arguments);};console.error=function(){send('error',arguments);oErr.apply(console,arguments);};window.addEventListener('error',function(e){send('error',[e.message]);});})();</script>`;
    return `<!doctype html><html><head><meta charset="utf-8" /><meta name="viewport" content="width=device-width, initial-scale=1.0" />${consoleBridge}<style>${files['styles.css']}</style></head><body>${files['index.html']}<script>${files['app.js']}</script></body></html>`;
  }, [files]);

  const deviceDims = useMemo(() => {
    if (viewport === 'fit') {
      return { w: Math.max(280, Math.round(stageSize.w)), h: Math.max(320, Math.round(stageSize.h)) };
    }
    const base = viewport === 'custom' ? { w: customWidth, h: customHeight } : PRESET_DIMS[viewport];
    return isLandscape ? { w: base.h, h: base.w } : base;
  }, [viewport, customWidth, customHeight, isLandscape, stageSize]);

  const scale = useMemo(() => {
    if (viewport === 'fit' || !zoomToFit) return 1;
    const availW = Math.max(160, stageSize.w - 24);
    const availH = Math.max(200, stageSize.h - 24);
    return Math.min(availW / deviceDims.w, availH / deviceDims.h, 1);
  }, [viewport, zoomToFit, stageSize, deviceDims]);

  const selectPreset = (preset: ViewportPreset) => {
    setViewport(preset);
    if (preset !== 'fit' && preset !== 'custom') {
      setCustomWidth(PRESET_DIMS[preset].w);
      setCustomHeight(PRESET_DIMS[preset].h);
    }
  };

  const handleRunCommand = (e: React.FormEvent) => {
    e.preventDefault();
    const cmd = terminalInput.trim();
    if (!cmd) return;
    let output = `sh: executed '${cmd}'`;
    if (cmd === 'help') {
      output = 'Available commands: vcode --version, ls, git status, clear, reset';
    } else if (cmd === 'vcode --version') {
      output = 'Vcode Studio v1.0.0 (Build 100)';
    } else if (cmd === 'ls') {
      output = Object.keys(files).join('   ');
    } else if (cmd === 'git status') {
      output = 'On branch main · 4 tracked files · clean working tree';
    } else if (cmd === 'clear') {
      setTerminalLines([]);
      setTerminalInput('');
      return;
    } else if (cmd === 'reset') {
      setFiles(INITIAL_FILES);
      setConsoleLogs([]);
      output = 'Workspace files reset to initial template.';
    }
    setTerminalLines((prev) => [...prev.slice(-6), `$ ${cmd}`, output]);
    setTerminalInput('');
  };

  return (
    <div className="rounded-xl border border-[#242C3A] bg-[#12161F] overflow-hidden">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#1E2532] px-4 py-3">
        <div className="flex flex-wrap items-center gap-1.5" role="tablist" aria-label="Editor tabs">
          {(Object.keys(INITIAL_FILES) as TabFile[]).map((file) => {
            const active = activeFile === file;
            return (
              <button
                key={file}
                type="button"
                role="tab"
                aria-selected={active}
                onClick={() => setActiveFile(file)}
                className={`px-3 py-1.5 text-xs font-mono rounded-md transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                  active
                    ? 'bg-[#007ACC] text-white font-semibold'
                    : 'bg-[#19202D] text-[#94A3B8] hover:text-white'
                }`}
              >
                {file}
              </button>
            );
          })}
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <label className="flex items-center gap-2 text-xs text-[#94A3B8]">
            <span>Theme</span>
            <select
              value={selectedThemeId}
              onChange={(e) => setSelectedThemeId(e.target.value)}
              className="rounded-md border border-[#2A3444] bg-[#0B0E14] px-2.5 py-1.5 text-xs text-[#F1F5F9] focus:outline-none focus:ring-2 focus:ring-[#007ACC]"
            >
              {IDE_THEMES.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>

          <div className="flex flex-wrap items-center gap-1 rounded-md border border-[#242C3A] bg-[#0B0E14] p-1">
            <button
              type="button"
              onClick={() => selectPreset('fit')}
              className={`flex items-center gap-1 px-2.5 py-1 text-xs rounded transition-colors cursor-pointer whitespace-nowrap shrink-0 ${
                viewport === 'fit' ? 'bg-[#007ACC] text-white' : 'text-[#94A3B8] hover:text-white'
              }`}
            >
              <Maximize2 className="h-3.5 w-3.5" />
              <span>Fit</span>
            </button>
            <button
              type="button"
              onClick={() => selectPreset('mobile')}
              className={`flex items-center gap-1 px-2.5 py-1 text-xs rounded transition-colors cursor-pointer whitespace-nowrap shrink-0 ${
                viewport === 'mobile' ? 'bg-[#007ACC] text-white' : 'text-[#94A3B8] hover:text-white'
              }`}
            >
              <Smartphone className="h-3.5 w-3.5" />
              <span>Mobile</span>
            </button>
            <button
              type="button"
              onClick={() => selectPreset('tablet')}
              className={`flex items-center gap-1 px-2.5 py-1 text-xs rounded transition-colors cursor-pointer whitespace-nowrap shrink-0 ${
                viewport === 'tablet' ? 'bg-[#007ACC] text-white' : 'text-[#94A3B8] hover:text-white'
              }`}
            >
              <Tablet className="h-3.5 w-3.5" />
              <span>Tablet</span>
            </button>
            <button
              type="button"
              onClick={() => selectPreset('desktop')}
              className={`flex items-center gap-1 px-2.5 py-1 text-xs rounded transition-colors cursor-pointer whitespace-nowrap shrink-0 ${
                viewport === 'desktop' ? 'bg-[#007ACC] text-white' : 'text-[#94A3B8] hover:text-white'
              }`}
            >
              <Monitor className="h-3.5 w-3.5" />
              <span>Desktop</span>
            </button>
          </div>

          {viewport !== 'fit' && (
            <div className="flex flex-wrap items-center gap-1.5">
              <input
                type="number"
                aria-label="Custom viewport width"
                min={240}
                max={2000}
                value={deviceDims.w}
                onChange={(e) => {
                  const val = Math.max(240, Math.min(2000, Number(e.target.value) || 240));
                  setIsLandscape(false);
                  setViewport('custom');
                  setCustomWidth(val);
                  setCustomHeight(deviceDims.h);
                }}
                className="w-16 rounded-md border border-[#242C3A] bg-[#0B0E14] px-2 py-1 font-mono text-xs text-white tabular-nums focus:outline-none focus:ring-2 focus:ring-[#007ACC]"
              />
              <span className="text-xs text-[#64748B]">×</span>
              <input
                type="number"
                aria-label="Custom viewport height"
                min={240}
                max={2000}
                value={deviceDims.h}
                onChange={(e) => {
                  const val = Math.max(240, Math.min(2000, Number(e.target.value) || 240));
                  setIsLandscape(false);
                  setViewport('custom');
                  setCustomWidth(deviceDims.w);
                  setCustomHeight(val);
                }}
                className="w-16 rounded-md border border-[#242C3A] bg-[#0B0E14] px-2 py-1 font-mono text-xs text-white tabular-nums focus:outline-none focus:ring-2 focus:ring-[#007ACC]"
              />
              <button
                type="button"
                onClick={() => setIsLandscape((v) => !v)}
                className="flex items-center gap-1.5 rounded-md border border-[#242C3A] bg-[#0B0E14] px-2.5 py-1 text-xs text-[#CBD5E1] hover:border-[#007ACC] transition-colors cursor-pointer whitespace-nowrap shrink-0"
              >
                <RotateCw className="h-3.5 w-3.5" />
                <span>{isLandscape ? 'Landscape' : 'Portrait'}</span>
              </button>
              <button
                type="button"
                onClick={() => setZoomToFit((z) => !z)}
                className="rounded-md border border-[#242C3A] bg-[#0B0E14] px-2.5 py-1 font-mono text-xs text-[#38BDF8] hover:border-[#007ACC] transition-colors cursor-pointer whitespace-nowrap shrink-0 tabular-nums"
              >
                {zoomToFit ? `${Math.round(scale * 100)}% Fit` : '100% Scroll'}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Split Editor + Scaled Device Live Preview */}
      <div className="grid grid-cols-1 lg:grid-cols-2 divide-y lg:divide-y-0 lg:divide-x divide-[#1E2532]">
        <div
          className="flex flex-col justify-between p-4"
          style={{ backgroundColor: theme.bg, color: theme.text }}
        >
          <div className="flex items-center justify-between text-xs pb-2 mb-2 border-b" style={{ borderColor: theme.border, color: theme.muted }}>
            <span className="font-mono">{activeFile}</span>
            <span>{theme.name}</span>
          </div>
          <textarea
            aria-label={`Code editor for ${activeFile}`}
            value={files[activeFile]}
            onChange={(e) => setFiles((prev) => ({ ...prev, [activeFile]: e.target.value }))}
            spellCheck={false}
            rows={14}
            className="w-full resize-none bg-transparent font-mono text-xs leading-6 focus:outline-none"
            style={{ color: theme.text }}
          />
          <div className="flex items-center justify-between pt-2 mt-2 border-t text-xs" style={{ borderColor: theme.border, color: theme.muted }}>
            <span className="tabular-nums">Lines: {files[activeFile].split('\n').length}</span>
            <button
              type="button"
              onClick={() => setFiles(INITIAL_FILES)}
              className="underline underline-offset-4 hover:opacity-80 cursor-pointer"
              style={{ color: theme.accent }}
            >
              Reset sample code
            </button>
          </div>
        </div>

        <div className="flex flex-col bg-[#090C11]">
          <div className="flex items-center justify-between border-b border-[#1E2532] px-4 py-2 text-xs text-[#94A3B8] font-mono tabular-nums">
            <span>
              {viewport === 'fit' ? 'Responsive Fit' : `${deviceDims.w} × ${deviceDims.h}px`}
            </span>
            <div className="flex items-center gap-3">
              <span>{viewport === 'fit' ? '100%' : `${Math.round(scale * 100)}% scale`}</span>
              <button
                type="button"
                aria-label="Reload preview"
                onClick={() => setRefreshCount((c) => c + 1)}
                className="inline-flex items-center gap-1 text-[#CBD5E1] hover:text-white cursor-pointer"
              >
                <RefreshCw className="h-3 w-3" />
                <span>Reload</span>
              </button>
            </div>
          </div>

          <div
            ref={stageRef}
            className={`relative flex-1 min-h-[380px] p-3 flex ${
              viewport === 'fit' || zoomToFit
                ? 'overflow-hidden items-center justify-center'
                : 'overflow-auto items-start justify-start'
            }`}
          >
            {viewport === 'fit' ? (
              <div className="h-full w-full min-h-[350px] overflow-hidden rounded-lg border border-[#242C3A] bg-[#0F172A]">
                <iframe
                  key={`fit-${refreshCount}`}
                  title="Vcode Studio Live WebView Preview"
                  srcDoc={previewSrcDoc}
                  sandbox="allow-scripts"
                  className="h-full w-full min-h-[350px] border-0 bg-white"
                />
              </div>
            ) : (
              <div
                className="shrink-0 overflow-hidden rounded-lg border border-[#242C3A] bg-[#0F172A] shadow-lg"
                style={{
                  width: `${Math.round(deviceDims.w * scale)}px`,
                  height: `${Math.round(deviceDims.h * scale)}px`,
                }}
              >
                <iframe
                  key={`dev-${refreshCount}`}
                  title="Vcode Studio Device Viewport Preview"
                  srcDoc={previewSrcDoc}
                  sandbox="allow-scripts"
                  style={{
                    width: `${deviceDims.w}px`,
                    height: `${deviceDims.h}px`,
                    transform: `scale(${scale})`,
                    transformOrigin: 'top left',
                  }}
                  className="border-0 bg-white"
                />
              </div>
            )}
          </div>

          {/* Live WebView Console Strip */}
          <div className="border-t border-[#1E2532] bg-[#0D1017] px-3 py-2">
            <div className="flex items-center justify-between text-[11px] text-[#94A3B8] mb-1">
              <span className="font-mono">Preview Console ({consoleLogs.length})</span>
              <button
                type="button"
                onClick={() => setConsoleLogs([])}
                className="inline-flex items-center gap-1 text-[#64748B] hover:text-white cursor-pointer"
              >
                <Trash2 className="h-3 w-3" />
                <span>Clear</span>
              </button>
            </div>
            <div className="max-h-16 overflow-y-auto space-y-0.5 font-mono text-[11px]">
              {consoleLogs.length === 0 ? (
                <div className="text-[#64748B]">Interact with the preview above to capture console output.</div>
              ) : (
                consoleLogs.map((log) => (
                  <div
                    key={log.id}
                    className={
                      log.level === 'error'
                        ? 'text-red-400'
                        : log.level === 'warn'
                          ? 'text-amber-300'
                          : 'text-[#CBD5E1]'
                    }
                  >
                    [{log.level}] {log.message}
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Interactive Shell Strip */}
      <div className="border-t border-[#1E2532] bg-[#0B0E14] p-4">
        <div className="mb-2 flex items-center gap-2 text-xs font-medium text-[#94A3B8]">
          <Terminal className="h-3.5 w-3.5 text-[#38BDF8]" />
          <span>Integrated Shell Simulation</span>
          <span aria-hidden="true">·</span>
          <span>Try help, ls, git status, or clear</span>
        </div>
        <div className="space-y-1 font-mono text-xs text-[#CBD5E1] mb-3 max-h-28 overflow-y-auto">
          {terminalLines.map((line, i) => (
            <div key={i} className={line.startsWith('$') ? 'text-[#38BDF8]' : 'text-[#94A3B8]'}>
              {line}
            </div>
          ))}
        </div>
        <form onSubmit={handleRunCommand} className="flex items-center gap-2">
          <input
            type="text"
            value={terminalInput}
            onChange={(e) => setTerminalInput(e.target.value)}
            placeholder="Type a command (e.g. help)"
            className="flex-1 rounded-md border border-[#242C3A] bg-[#12161F] px-3 py-2 font-mono text-xs text-white placeholder:text-[#64748B] focus:outline-none focus:ring-2 focus:ring-[#007ACC]"
          />
          <button
            type="submit"
            className="px-4 py-2 text-xs font-semibold text-white bg-[#1D2636] hover:bg-[#007ACC] rounded-md transition-colors cursor-pointer whitespace-nowrap shrink-0"
          >
            Run
          </button>
        </form>
      </div>
    </div>
  );
}
