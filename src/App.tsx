import React, { useState } from 'react';
import { Check, Copy, Download, ShieldCheck } from 'lucide-react';
import { Navbar } from './components/Navbar';
import { IdePlayground } from './components/IdePlayground';
import { LegalDocType, LegalModal } from './components/LegalModal';

type LangCategory = 'All' | 'Web' | 'Mobile' | 'Systems';

interface SupportedLanguage {
  name: string;
  extension: string;
  scope: string;
  category: Exclude<LangCategory, 'All'>;
}

const LANGUAGES: SupportedLanguage[] = [
  { name: 'HTML', extension: '.html', scope: 'text.html.basic', category: 'Web' },
  { name: 'CSS', extension: '.css', scope: 'source.css', category: 'Web' },
  { name: 'JavaScript', extension: '.js', scope: 'source.js', category: 'Web' },
  { name: 'TypeScript', extension: '.ts', scope: 'source.ts', category: 'Web' },
  { name: 'JSON', extension: '.json', scope: 'source.json', category: 'Web' },
  { name: 'Kotlin', extension: '.kt', scope: 'source.kotlin', category: 'Mobile' },
  { name: 'Java', extension: '.java', scope: 'source.java', category: 'Mobile' },
  { name: 'XML', extension: '.xml', scope: 'text.xml', category: 'Mobile' },
  { name: 'Python', extension: '.py', scope: 'source.python', category: 'Systems' },
  { name: 'C', extension: '.c', scope: 'source.c', category: 'Systems' },
  { name: 'C++', extension: '.cpp', scope: 'source.cpp', category: 'Systems' },
  { name: 'PHP', extension: '.php', scope: 'source.php', category: 'Web' },
  { name: 'SQL', extension: '.sql', scope: 'source.sql', category: 'Systems' },
  { name: 'Markdown', extension: '.md', scope: 'text.html.markdown', category: 'Web' },
  { name: 'Shell Script', extension: '.sh', scope: 'source.shell', category: 'Systems' },
];

const STACK_ROWS = [
  { layer: 'UI & Adaptive Layout', module: 'Jetpack Compose + Material 3 WindowSizeClass', detail: 'Edge-to-edge phone and tablet workbench' },
  { layer: 'Code Editor Engine', module: 'Sora CodeEditor 0.23.7 + TextMate', detail: '15 grammars, folding, minimap, bracket matching' },
  { layer: 'Shell & JS Runtime', module: '/system/bin/sh + Mozilla Rhino 1.7.15', detail: 'Multi-session PTY-style terminal and JS execution' },
  { layer: 'Source Control', module: 'Eclipse JGit 5.13.5', detail: 'Repository init, status, diff, branch, and commit' },
  { layer: 'Persistence & Storage', module: 'Room 2.6 + DataStore + Android SAF', detail: 'Project history, recent tabs, ZIP export' },
  { layer: 'Splash & Packaging', module: 'Core SplashScreen 1.0.1 + APK v2/v3', detail: 'com.vcode.ide · v1.0.0 (Build 100)' },
];

export default function App() {
  const [langFilter, setLangFilter] = useState<LangCategory>('All');
  const [copiedCmd, setCopiedCmd] = useState<string | null>(null);
  const [legalDoc, setLegalDoc] = useState<LegalDocType>(null);
  const [heroImgFailed, setHeroImgFailed] = useState(false);
  const [editorImgFailed, setEditorImgFailed] = useState(false);
  const [previewImgFailed, setPreviewImgFailed] = useState(false);

  const visibleLanguages =
    langFilter === 'All' ? LANGUAGES : LANGUAGES.filter((l) => l.category === langFilter);

  const scrollToRelease = () => {
    document.getElementById('release')?.scrollIntoView({ behavior: 'smooth' });
  };

  const copyText = async (key: string, value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setCopiedCmd(key);
      setTimeout(() => setCopiedCmd(null), 1800);
    } catch {
      setCopiedCmd(key);
      setTimeout(() => setCopiedCmd(null), 1800);
    }
  };

  return (
    <div id="top" className="min-h-screen bg-[#0B0E14] text-[#F1F5F9]">
      <Navbar onGetApkClick={scrollToRelease} />

      <main>
        {/* Hero Split */}
        <section className="mx-auto max-w-[1200px] px-6 pt-16 pb-14">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-stretch">
            <div className="lg:col-span-6 flex flex-col justify-center">
              <div className="flex flex-wrap items-center gap-2 text-xs text-[#94A3B8] mb-5">
                <span>Vcode Studio</span>
                <span aria-hidden="true">·</span>
                <span>Android 8.0+ (API 26)</span>
                <span aria-hidden="true">·</span>
                <span>Release v1.0.0 (Build 100)</span>
              </div>

              <h1
                className="font-display font-bold tracking-tight text-white leading-[1.08]"
                style={{ fontSize: 'clamp(2.25rem, 4vw, 3.6rem)', textWrap: 'balance' }}
              >
                Write, run, and preview code natively on Android
              </h1>

              <p className="mt-5 max-w-xl text-base leading-relaxed text-[#94A3B8]">
                VCode brings a Visual Studio Code workbench to phones and tablets in Kotlin and Jetpack Compose, combining Sora TextMate syntax highlighting, an integrated shell terminal, Eclipse JGit, and responsive WebView preview inside a 16 MB signed APK.
              </p>

              <div className="mt-8 flex flex-wrap items-center gap-4">
                <button
                  type="button"
                  onClick={scrollToRelease}
                  className="inline-flex items-center gap-2 px-5 py-2.5 text-sm font-semibold text-white bg-[#007ACC] hover:bg-[#0066AD] rounded-lg transition-colors cursor-pointer whitespace-nowrap shrink-0"
                >
                  <Download className="h-4 w-4" />
                  <span>Get Signed APK (v1.0.0)</span>
                </button>
                <a
                  href="#playground"
                  className="px-4 py-2.5 text-sm font-medium text-[#CBD5E1] hover:text-white underline underline-offset-4 decoration-[#334155] hover:decoration-[#007ACC] transition-colors whitespace-nowrap shrink-0"
                >
                  Try Interactive Preview
                </a>
              </div>
            </div>

            <div className="lg:col-span-6 flex items-stretch">
              <div className="w-full overflow-hidden rounded-xl border border-[#242C3A] bg-[#12161F]">
                {!heroImgFailed ? (
                  <img
                    src="/src/assets/images/vcode_banner_1795255712750.jpg"
                    alt="VCode native Android IDE running on a tablet and smartphone side by side"
                    referrerPolicy="no-referrer"
                    onError={() => setHeroImgFailed(true)}
                    className="h-full w-full object-cover aspect-video lg:aspect-auto"
                  />
                ) : (
                  <div className="flex h-full min-h-[280px] flex-col items-center justify-center bg-gradient-to-br from-[#0F172A] to-[#1E293B] p-8 text-center">
                    <span className="font-display text-lg font-semibold text-white">VCode Android IDE</span>
                    <span className="mt-1 text-xs text-[#94A3B8]">Tablet and Phone Workbench Preview</span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* 4-Column Quantified Strip */}
          <div className="mt-12 grid grid-cols-2 lg:grid-cols-4 border-y border-[#1E2532] divide-y lg:divide-y-0 lg:divide-x divide-[#1E2532]">
            <div className="py-5 px-4">
              <div className="font-display text-2xl font-bold text-white tabular-nums">15</div>
              <div className="mt-1 text-xs text-[#94A3B8]">TextMate language grammars</div>
            </div>
            <div className="py-5 px-4">
              <div className="font-display text-2xl font-bold text-white tabular-nums">6</div>
              <div className="mt-1 text-xs text-[#94A3B8]">Built-in dark and light IDE themes</div>
            </div>
            <div className="py-5 px-4">
              <div className="font-display text-2xl font-bold text-white tabular-nums">API 26–35</div>
              <div className="mt-1 text-xs text-[#94A3B8]">Android 8.0 through Android 15</div>
            </div>
            <div className="py-5 px-4">
              <div className="font-display text-2xl font-bold text-white tabular-nums">16 MB</div>
              <div className="mt-1 text-xs text-[#94A3B8]">Signed APK · zero telemetry</div>
            </div>
          </div>
        </section>

        {/* Capabilities Bento */}
        <section id="capabilities" className="mx-auto max-w-[1200px] px-6 py-16">
          <div className="max-w-2xl mb-10">
            <h2 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-white" style={{ textWrap: 'balance' }}>
              Desktop workbench ergonomics tuned for Android touch and hardware keyboards
            </h2>
            <p className="mt-3 text-sm sm:text-base text-[#94A3B8] leading-relaxed">
              Every core subsystem executes locally on device, from Sora TextMate tokenization and JGit commits to Storage Access Framework file import and ZIP export.
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            <article className="lg:col-span-7 rounded-xl border border-[#242C3A] bg-[#12161F] overflow-hidden flex flex-col">
              <div className="p-6">
                <h3 className="font-display text-lg font-semibold text-white">
                  01. Multi-Tab Code Editor &amp; File Explorer
                </h3>
                <p className="mt-2 text-sm text-[#94A3B8] leading-relaxed">
                  Powered by Sora CodeEditor 0.23.7 with TextMate scopes, line numbers, auto-indentation, bracket matching, minimap rail, find and replace, code folding, and automatic saving via Android Storage Access Framework.
                </p>
              </div>
              <div className="mt-auto border-t border-[#1E2532] bg-[#0B0E14]">
                {!editorImgFailed ? (
                  <img
                    src="/src/assets/images/vcode_editor_tablet_1795255713524.jpg"
                    alt="VCode tablet landscape multi-tab code editor and file explorer"
                    referrerPolicy="no-referrer"
                    onError={() => setEditorImgFailed(true)}
                    className="w-full aspect-video object-cover"
                  />
                ) : (
                  <div className="flex aspect-video items-center justify-center text-xs text-[#94A3B8]">
                    Multi-Tab Sora CodeEditor View
                  </div>
                )}
              </div>
            </article>

            <div className="lg:col-span-5 flex flex-col gap-6">
              <article className="rounded-xl border border-[#242C3A] bg-[#12161F] p-6">
                <h3 className="font-display text-lg font-semibold text-white">
                  02. Integrated Android Shell &amp; JavaScript Runtime
                </h3>
                <p className="mt-2 text-sm text-[#94A3B8] leading-relaxed">
                  Run multi-session <code className="text-[#38BDF8]">/system/bin/sh</code> commands with ANSI color parsing, command history, Ctrl+C interrupt, resizable panels, plus embedded Eclipse JGit and Mozilla Rhino <code className="text-[#38BDF8]">js</code> execution.
                </p>
              </article>

              <article className="rounded-xl border border-[#242C3A] bg-[#12161F] overflow-hidden flex-1 flex flex-col">
                <div className="p-6">
                  <h3 className="font-display text-lg font-semibold text-white">
                    03. Split-Screen WebView &amp; Device Viewport
                  </h3>
                  <p className="mt-2 text-sm text-[#94A3B8] leading-relaxed">
                    Inspect HTML, CSS, and JavaScript locally over <code className="text-[#38BDF8]">https://vcode.local/</code> across Mobile, Tablet, Desktop, and custom pixel viewports with live console capture.
                  </p>
                </div>
                <div className="mt-auto border-t border-[#1E2532] bg-[#0B0E14]">
                  {!previewImgFailed ? (
                    <img
                      src="/src/assets/images/vcode_preview_terminal_1795255713302.jpg"
                      alt="VCode split-screen HTML live preview with viewport switcher and shell terminal"
                      referrerPolicy="no-referrer"
                      onError={() => setPreviewImgFailed(true)}
                      className="w-full aspect-video object-cover"
                    />
                  ) : (
                    <div className="flex aspect-video items-center justify-center text-xs text-[#94A3B8]">
                      Split-Screen WebView Preview &amp; Terminal
                    </div>
                  )}
                </div>
              </article>
            </div>
          </div>

          {/* 15 Supported Languages Filter */}
          <div className="mt-12 rounded-xl border border-[#242C3A] bg-[#12161F] p-6">
            <div className="flex flex-wrap items-center justify-between gap-4 pb-5 border-b border-[#1E2532]">
              <div>
                <h3 className="font-display text-base font-semibold text-white">
                  04. Supported TextMate Language Grammars
                </h3>
                <p className="mt-1 text-xs text-[#94A3B8]">
                  Filter the 15 bundled syntax highlighters and bracket configurations.
                </p>
              </div>
              <div className="flex items-center gap-1 rounded-lg bg-[#0B0E14] p-1 border border-[#1E2532]">
                {(['All', 'Web', 'Mobile', 'Systems'] as LangCategory[]).map((cat) => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => setLangFilter(cat)}
                    className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                      langFilter === cat
                        ? 'bg-[#007ACC] text-white'
                        : 'text-[#94A3B8] hover:text-white'
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            </div>

            <div className="mt-5 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
              {visibleLanguages.map((lang) => (
                <div
                  key={lang.name}
                  className="rounded-lg border border-[#1E2532] bg-[#0B0E14] px-3.5 py-3"
                >
                  <div className="flex items-baseline justify-between gap-2">
                    <span className="text-sm font-semibold text-white">{lang.name}</span>
                    <span className="font-mono text-xs text-[#38BDF8]">{lang.extension}</span>
                  </div>
                  <div className="mt-1 font-mono text-[11px] text-[#64748B] truncate">
                    {lang.scope}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Interactive Browser Playground */}
        <section id="playground" className="mx-auto max-w-[1200px] px-6 py-16 border-t border-[#1E2532]">
          <div className="max-w-2xl mb-8">
            <h2 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-white" style={{ textWrap: 'balance' }}>
              Interactive workbench preview
            </h2>
            <p className="mt-2 text-sm sm:text-base text-[#94A3B8]">
              Edit code, switch between all six VCode editor color themes, resize the device viewport, and test sample commands directly in your browser.
            </p>
          </div>
          <IdePlayground />
        </section>

        {/* Architecture Table */}
        <section id="architecture" className="mx-auto max-w-[1200px] px-6 py-16 border-t border-[#1E2532]">
          <div className="max-w-2xl mb-8">
            <h2 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-white">
              Native Android architecture
            </h2>
            <p className="mt-2 text-sm sm:text-base text-[#94A3B8]">
              Structured around MVVM unidirectional state flows, KSP Room compilation, and verified TextMate assets.
            </p>
          </div>

          <div className="overflow-x-auto rounded-xl border border-[#242C3A] bg-[#12161F]">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#1E2532] text-xs text-[#94A3B8]">
                  <th className="py-3.5 px-5 font-semibold">Layer</th>
                  <th className="py-3.5 px-5 font-semibold">Library / Component</th>
                  <th className="py-3.5 px-5 font-semibold">Responsibility</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1E2532] text-sm">
                {STACK_ROWS.map((row) => (
                  <tr key={row.layer} className="hover:bg-[#161B26] transition-colors">
                    <td className="py-3.5 px-5 font-medium text-white whitespace-nowrap">{row.layer}</td>
                    <td className="py-3.5 px-5 font-mono text-xs text-[#38BDF8]">{row.module}</td>
                    <td className="py-3.5 px-5 text-[#94A3B8]">{row.detail}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        {/* Release & GitHub Actions Section */}
        <section id="release" className="mx-auto max-w-[1200px] px-6 py-16 border-t border-[#1E2532]">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            <div className="lg:col-span-6">
              <h2 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-white" style={{ textWrap: 'balance' }}>
                Signed release APK &amp; GitHub Actions pipeline
              </h2>
              <p className="mt-3 text-sm sm:text-base text-[#94A3B8] leading-relaxed">
                Compiled with Android SDK 35 and verified via <code className="text-[#38BDF8]">apksigner</code> under APK Signature Scheme v2 and v3. Push or tag <code className="text-[#38BDF8]">v1.0.0</code> on GitHub to trigger automated artifact builds and GitHub Releases.
              </p>

              <div className="mt-6 space-y-3 text-xs text-[#94A3B8] font-mono tabular-nums">
                <div className="flex items-center gap-2 text-[#CBD5E1]">
                  <ShieldCheck className="h-4 w-4 text-[#38BDF8] shrink-0" />
                  <span>Application ID: com.vcode.ide · Version 1.0.0 (100)</span>
                </div>
                <div>SHA-256 Signer: 6f880eba2ac3c1afab35a788f5a4632b6228e2391cd85b1db5d1a82cb3677e8d</div>
                <div>APK Output Path: app/build/outputs/apk/release/app-release.apk</div>
              </div>
            </div>

            <div className="lg:col-span-6 rounded-xl border border-[#242C3A] bg-[#12161F] p-6 space-y-4">
              <div>
                <div className="flex items-center justify-between text-xs text-[#94A3B8] mb-1.5">
                  <span>Build signed release APK</span>
                  <button
                    type="button"
                    onClick={() => copyText('build', './gradlew assembleRelease')}
                    className="inline-flex items-center gap-1 text-[#38BDF8] hover:underline cursor-pointer"
                  >
                    {copiedCmd === 'build' ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
                    <span>{copiedCmd === 'build' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <pre className="rounded-lg border border-[#1E2532] bg-[#0B0E14] px-3.5 py-2.5 font-mono text-xs text-[#E2E8F0] overflow-x-auto">
                  ./gradlew assembleRelease
                </pre>
              </div>

              <div>
                <div className="flex items-center justify-between text-xs text-[#94A3B8] mb-1.5">
                  <span>Run unit tests</span>
                  <button
                    type="button"
                    onClick={() => copyText('test', './gradlew testReleaseUnitTest')}
                    className="inline-flex items-center gap-1 text-[#38BDF8] hover:underline cursor-pointer"
                  >
                    {copiedCmd === 'test' ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
                    <span>{copiedCmd === 'test' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <pre className="rounded-lg border border-[#1E2532] bg-[#0B0E14] px-3.5 py-2.5 font-mono text-xs text-[#E2E8F0] overflow-x-auto">
                  ./gradlew testReleaseUnitTest
                </pre>
              </div>

              <div>
                <div className="flex items-center justify-between text-xs text-[#94A3B8] mb-1.5">
                  <span>GitHub Actions workflow file</span>
                  <button
                    type="button"
                    onClick={() => copyText('ci', '.github/workflows/android-release.yml')}
                    className="inline-flex items-center gap-1 text-[#38BDF8] hover:underline cursor-pointer"
                  >
                    {copiedCmd === 'ci' ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
                    <span>{copiedCmd === 'ci' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <pre className="rounded-lg border border-[#1E2532] bg-[#0B0E14] px-3.5 py-2.5 font-mono text-xs text-[#E2E8F0] overflow-x-auto">
                  .github/workflows/android-release.yml
                </pre>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Quiet Footer */}
      <footer className="border-t border-[#1E2532] bg-[#0B0E14]">
        <div className="mx-auto flex max-w-[1200px] flex-wrap items-center justify-between gap-6 px-6 py-8 text-xs text-[#94A3B8]">
          <div className="flex items-center gap-3">
            <span className="font-display font-semibold text-white">Vcode Studio</span>
            <span aria-hidden="true">·</span>
            <span>Native Android IDE v1.0.0</span>
          </div>

          <div className="flex flex-wrap items-center gap-6">
            <a href="#capabilities" className="hover:text-white transition-colors">
              Capabilities
            </a>
            <a href="#playground" className="hover:text-white transition-colors">
              Playground
            </a>
            <button
              type="button"
              onClick={() => setLegalDoc('license')}
              className="hover:text-white transition-colors cursor-pointer"
            >
              Apache 2.0 License
            </button>
            <button
              type="button"
              onClick={() => setLegalDoc('privacy')}
              className="hover:text-white transition-colors cursor-pointer"
            >
              Privacy Policy
            </button>
          </div>
        </div>
      </footer>

      <LegalModal doc={legalDoc} onClose={() => setLegalDoc(null)} />
    </div>
  );
}
