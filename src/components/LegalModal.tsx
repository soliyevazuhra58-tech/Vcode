import React, { useEffect } from 'react';
import { X } from 'lucide-react';

export type LegalDocType = 'license' | 'privacy' | null;

interface LegalModalProps {
  doc: LegalDocType;
  onClose: () => void;
}

export function LegalModal({ doc, onClose }: LegalModalProps) {
  useEffect(() => {
    if (!doc) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [doc, onClose]);

  if (!doc) return null;

  const isLicense = doc === 'license';

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="legal-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-2xl rounded-xl border border-[#242C3A] bg-[#12161F] p-6 text-[#E2E8F0] shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between gap-4 border-b border-[#1E2532] pb-4">
          <h2 id="legal-modal-title" className="font-display text-lg font-bold text-white">
            {isLicense ? 'Apache License 2.0' : 'VCode Privacy Policy'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal"
            className="flex h-10 w-10 items-center justify-center rounded-lg border border-[#242C3A] text-[#94A3B8] hover:bg-[#1A202E] hover:text-white transition-colors cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="mt-4 max-h-[65vh] overflow-y-auto space-y-4 text-sm leading-relaxed text-[#94A3B8]">
          {isLicense ? (
            <>
              <p className="text-[#CBD5E1]">
                Copyright 2026 VCode Contributors — Licensed under the Apache License, Version 2.0.
              </p>
              <p>
                You may use, modify, and distribute VCode source files and compiled APKs in compliance
                with the Apache 2.0 License terms located in <code className="text-[#38BDF8]">LICENSE</code> at
                the repository root.
              </p>
              <p>
                Core Android dependencies include Jetpack Compose, AndroidX Room, DataStore Preferences,
                Sora CodeEditor, Eclipse JGit, and Mozilla Rhino, each distributed under their respective
                open-source licenses.
              </p>
            </>
          ) : (
            <>
              <p className="text-[#CBD5E1]">
                Effective November 21, 2026 — VCode is engineered as an offline-first native Android IDE.
              </p>
              <div>
                <h3 className="font-semibold text-white">01. Zero Analytics or Telemetry</h3>
                <p className="mt-1">
                  VCode does not collect usage telemetry, advertising identifiers, or behavioral analytics,
                  and never uploads your source code or terminal sessions to external servers.
                </p>
              </div>
              <div>
                <h3 className="font-semibold text-white">02. Local Persistence &amp; SAF Access</h3>
                <p className="mt-1">
                  Projects, recent tabs, and preferences remain inside app-private storage, local Room
                  SQLite (<code className="text-[#38BDF8]">vcode_ide.db</code>), and DataStore. Shared
                  storage access relies strictly on Android Storage Access Framework pickers you authorize.
                </p>
              </div>
              <div>
                <h3 className="font-semibold text-white">03. Local Preview &amp; Terminal</h3>
                <p className="mt-1">
                  Live HTML/CSS/JS preview serves local files exclusively via <code className="text-[#38BDF8]">https://vcode.local/</code>{' '}
                  inside Android WebView without opening external network ports.
                </p>
              </div>
            </>
          )}
        </div>

        <div className="mt-6 flex justify-end border-t border-[#1E2532] pt-4">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-xs font-semibold text-white bg-[#007ACC] hover:bg-[#0066AD] rounded-lg transition-colors cursor-pointer"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
