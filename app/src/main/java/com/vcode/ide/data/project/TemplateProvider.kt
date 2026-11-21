package com.vcode.ide.data.project

import com.vcode.ide.domain.model.ExtensionItem
import com.vcode.ide.domain.model.ProjectTemplate

object TemplateProvider {
    val templates: List<ProjectTemplate> = listOf(
        ProjectTemplate(
            id = "web-starter",
            name = "Web App (HTML/CSS/JS)",
            subtitle = "Interactive dashboard with Live Preview & JS console",
            badge = "WEB",
            files = mapOf(
                "index.html" to """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                      <meta charset="UTF-8" />
                      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                      <title>VCode Web Studio</title>
                      <link rel="stylesheet" href="css/styles.css" />
                    </head>
                    <body>
                      <main class="shell">
                        <header class="hero">
                          <span class="badge">LIVE ON ANDROID</span>
                          <h1>VCode Web Workspace</h1>
                          <p>Edit HTML, CSS, or JavaScript and inspect changes instantly across mobile, tablet, and desktop viewports.</p>
                        </header>

                        <section class="grid">
                          <article class="card">
                            <h2>Interactive Counter</h2>
                            <p class="metric" id="counterValue">0</p>
                            <div class="actions">
                              <button id="decBtn" type="button">-1</button>
                              <button id="incBtn" type="button" class="primary">+1 Increment</button>
                            </div>
                          </article>

                          <article class="card">
                            <h2>Viewport Telemetry</h2>
                            <p id="viewportReadout" class="mono">Calculating...</p>
                            <button id="logBtn" type="button">Emit Console Log</button>
                          </article>
                        </section>
                      </main>
                      <script src="js/app.js"></script>
                    </body>
                    </html>
                """.trimIndent(),
                "css/styles.css" to """
                    :root {
                      --bg: #0f172a;
                      --surface: #1e293b;
                      --border: #334155;
                      --text: #f8fafc;
                      --muted: #94a3b8;
                      --accent: #38bdf8;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                      font-family: system-ui, -apple-system, sans-serif;
                      background: radial-gradient(circle at top right, #172554, var(--bg));
                      color: var(--text);
                      min-height: 100vh;
                      padding: 20px;
                    }
                    .shell { max-width: 900px; margin: 0 auto; display: grid; gap: 18px; }
                    .hero { background: var(--surface); border: 1px solid var(--border); border-radius: 14px; padding: 20px; }
                    .badge { display: inline-block; font-size: 11px; font-weight: 700; letter-spacing: 0.08em; color: var(--accent); margin-bottom: 8px; }
                    .hero h1 { font-size: clamp(1.35rem, 3vw, 2rem); margin-bottom: 8px; }
                    .hero p { color: var(--muted); line-height: 1.5; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
                    .card { background: var(--surface); border: 1px solid var(--border); border-radius: 14px; padding: 18px; display: grid; gap: 12px; }
                    .metric { font-size: 2.2rem; font-weight: 800; color: var(--accent); }
                    .mono { font-family: monospace; color: #cbd5e1; font-size: 0.9rem; }
                    .actions { display: flex; gap: 10px; }
                    button { background: #334155; color: var(--text); border: 1px solid #475569; border-radius: 8px; padding: 9px 14px; font-weight: 600; cursor: pointer; }
                    button.primary { background: #0284c7; border-color: #38bdf8; }
                """.trimIndent(),
                "js/app.js" to """
                    let count = 0;
                    const counterEl = document.getElementById('counterValue');
                    const viewportEl = document.getElementById('viewportReadout');
                    function updateReadout() {
                      if (viewportEl) {
                        viewportEl.textContent = window.innerWidth + 'px × ' + window.innerHeight + 'px';
                      }
                    }
                    document.getElementById('incBtn')?.addEventListener('click', () => {
                      count += 1;
                      counterEl.textContent = String(count);
                      console.log('Counter incremented to', count);
                    });
                    document.getElementById('decBtn')?.addEventListener('click', () => {
                      count -= 1;
                      counterEl.textContent = String(count);
                      console.log('Counter decremented to', count);
                    });
                    document.getElementById('logBtn')?.addEventListener('click', () => {
                      console.info('Viewport check:', window.innerWidth + 'x' + window.innerHeight);
                    });
                    window.addEventListener('resize', updateReadout);
                    updateReadout();
                    console.log('VCode Web Workspace initialized');
                """.trimIndent(),
                "data/config.json" to """
                    {
                      "appName": "VCode Web Studio",
                      "version": "1.0.0",
                      "features": ["live-preview", "responsive-viewport", "git", "terminal"]
                    }
                """.trimIndent(),
                "README.md" to "# VCode Web Workspace\n\nEdit `index.html`, `css/styles.css`, and `js/app.js` and preview live.\n"
            )
        ),
        ProjectTemplate(
            id = "multi-lang",
            name = "Polyglot Code Lab",
            subtitle = "Kotlin, Java, Python, C++, SQL & TypeScript samples",
            badge = "LAB",
            files = mapOf(
                "src/Main.kt" to """
                    package demo
                    data class Task(val id: Int, val title: String, val done: Boolean = false)
                    fun main() {
                        val tasks = listOf(
                            Task(1, "Configure Sora TextMate", true),
                            Task(2, "Initialize JGit repo", false)
                        )
                        tasks.forEach { println("[#${'$'}{it.id}] ${'$'}{it.title}") }
                    }
                """.trimIndent(),
                "src/app.ts" to """
                    interface Metric { label: string; value: number; }
                    function summarize(items: Metric[]): number {
                      return items.reduce((acc, item) => acc + item.value, 0);
                    }
                    console.log("Total metric:", summarize([{ label: "CPU", value: 32 }, { label: "Mem", value: 64 }]));
                """.trimIndent(),
                "scripts/analyze.py" to """
                    def fibonacci(n: int) -> list[int]:
                        seq = [0, 1]
                        while len(seq) < n:
                            seq.append(seq[-1] + seq[-2])
                        return seq[:n]

                    if __name__ == "__main__":
                        print("Fibonacci(10):", fibonacci(10))
                """.trimIndent(),
                "native/engine.cpp" to """
                    #include <iostream>
                    #include <vector>
                    int main() {
                        std::vector<int> values = {2, 4, 8, 16};
                        int sum = 0;
                        for (int v : values) sum += v;
                        std::cout << "Sum: " << sum << std::endl;
                        return 0;
                    }
                """.trimIndent(),
                "db/schema.sql" to "CREATE TABLE IF NOT EXISTS projects (id TEXT PRIMARY KEY, name TEXT NOT NULL);\nSELECT id, name FROM projects;\n",
                "notes.md" to "# Polyglot Code Lab\n\nSwitch files in Explorer to inspect TextMate highlighting.\n"
            )
        ),
        ProjectTemplate(
            id = "node-rhino",
            name = "JavaScript Console App",
            subtitle = "Runnable directly inside VCode Terminal via `node main.js`",
            badge = "JS",
            files = mapOf(
                "main.js" to """
                    function primeFactors(n) {
                      const factors = [];
                      let d = 2;
                      while (n >= 2) {
                        if (n % d === 0) { factors.push(d); n = n / d; } else { d++; }
                      }
                      return factors;
                    }
                    console.log("Prime factors of 13195:", JSON.stringify(primeFactors(13195)));
                """.trimIndent(),
                "package.json" to "{\n  \"name\": \"vcode-js-console\",\n  \"version\": \"1.0.0\",\n  \"main\": \"main.js\"\n}\n"
            )
        ),
        ProjectTemplate(
            id = "blank",
            name = "Empty Project",
            subtitle = "Clean workspace folder with README.md",
            badge = "NEW",
            files = mapOf("README.md" to "# New VCode Project\n\nCreate files from Explorer or Command Palette.\n")
        )
    )

    val builtInExtensions: List<ExtensionItem> = listOf(
        ExtensionItem("ext.textmate.essentials", "TextMate Language Pack", "VCode Core", "1.4.0", "Syntax grammars & auto-closing rules for 15 languages.", "Languages"),
        ExtensionItem("ext.prettier.formatter", "Code Formatter", "VCode Tools", "2.1.0", "Instant formatting for JSON, HTML, XML, CSS, JS, Kotlin, Java, C++.", "Formatters"),
        ExtensionItem("ext.emmet.snippets", "HTML & Code Snippets", "VCode Snippets", "1.2.3", "Smart auto-completion keywords across Web, Kotlin, Java, Python.", "Snippets"),
        ExtensionItem("ext.git.lens", "Git Graph & Diff", "JGit Embedded", "5.13.5", "Local repository initialization, staging, diffs, branches, and commit history.", "SCM"),
        ExtensionItem("ext.material.icons", "VS Code File Icons", "VCode Design", "1.0.8", "Language-coded badges in Explorer and Editor Tabs.", "Themes"),
        ExtensionItem("ext.live.viewport", "Responsive Viewport Lab", "VCode Web", "1.1.0", "Interactive Mobile, Tablet, Desktop, and drag-resizable WebView testing.", "Preview")
    )
}
