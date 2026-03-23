"use client";

import { useEffect, useRef } from "react";

interface TerminalLogProps {
  logs: string[];
}

export function TerminalLog({ logs }: TerminalLogProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [logs]);

  return (
    <div className="rounded-xl bg-[#0a1214] border border-[var(--neon-cyan)]/20 overflow-hidden">
      {/* Terminal Header */}
      <div className="flex items-center gap-2 px-4 py-2 bg-[#0d1618] border-b border-[var(--neon-cyan)]/10">
        <div className="w-3 h-3 rounded-full bg-red-500/70" />
        <div className="w-3 h-3 rounded-full bg-yellow-500/70" />
        <div className="w-3 h-3 rounded-full bg-green-500/70" />
        <span className="ml-2 text-xs text-muted-foreground font-mono">
          bridge-terminal
        </span>
      </div>

      {/* Terminal Content */}
      <div
        ref={containerRef}
        className="p-4 h-36 overflow-y-auto scrollbar-thin scrollbar-thumb-[var(--neon-cyan)]/20 scrollbar-track-transparent"
      >
        {logs.map((log, index) => (
          <div
            key={index}
            className={`terminal-text text-sm leading-relaxed ${
              log.includes("ERROR")
                ? "text-red-400"
                : log.includes("verified") || log.includes("complete")
                  ? "text-emerald-400"
                  : ""
            }`}
            style={{
              animation: "fadeIn 0.3s ease-out",
            }}
          >
            {log}
          </div>
        ))}
        <div className="terminal-text text-sm animate-pulse">_</div>
      </div>

      <style jsx>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(5px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `}</style>
    </div>
  );
}
