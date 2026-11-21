import React from 'react';

interface NavbarProps {
  onGetApkClick: () => void;
}

export function Navbar({ onGetApkClick }: NavbarProps) {
  return (
    <header className="sticky top-0 z-40 border-b border-[#1E2532] bg-[#0B0E14]/90 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-[1200px] items-center justify-between gap-8 px-6">
        <a
          href="#top"
          className="font-display text-base font-bold tracking-tight text-[#F8FAFC] whitespace-nowrap shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#007ACC] rounded"
        >
          Vcode Studio
        </a>

        <nav
          aria-label="Primary navigation"
          className="hidden md:flex items-center gap-7 text-sm font-medium text-[#94A3B8]"
        >
          <a
            href="#capabilities"
            className="hover:text-[#F8FAFC] hover:underline underline-offset-4 decoration-[#007ACC] transition-colors whitespace-nowrap shrink-0"
          >
            Capabilities
          </a>
          <a
            href="#playground"
            className="hover:text-[#F8FAFC] hover:underline underline-offset-4 decoration-[#007ACC] transition-colors whitespace-nowrap shrink-0"
          >
            Playground
          </a>
          <a
            href="#architecture"
            className="hover:text-[#F8FAFC] hover:underline underline-offset-4 decoration-[#007ACC] transition-colors whitespace-nowrap shrink-0"
          >
            Architecture
          </a>
          <a
            href="#release"
            className="hover:text-[#F8FAFC] hover:underline underline-offset-4 decoration-[#007ACC] transition-colors whitespace-nowrap shrink-0"
          >
            Release
          </a>
        </nav>

        <div className="flex items-center shrink-0">
          <button
            type="button"
            onClick={onGetApkClick}
            className="px-4 py-2 text-xs font-semibold text-white bg-[#007ACC] hover:bg-[#0066AD] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#38BDF8]"
          >
            Get Release APK
          </button>
        </div>
      </div>
    </header>
  );
}
