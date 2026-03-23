"use client";

import { AlertTriangle, X } from "lucide-react";

interface CorsAlertProps {
  onDismiss: () => void;
}

export function CorsAlert({ onDismiss }: CorsAlertProps) {
  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 w-full max-w-md px-4 z-50 animate-in slide-in-from-bottom-4 fade-in duration-300">
      <div className="rounded-xl p-4 border border-[var(--neon-cyan)]/30 bg-[var(--neon-cyan)]/5 backdrop-blur-xl">
        <div className="flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-[var(--neon-cyan)] shrink-0 mt-0.5" />
          <div className="flex-1">
            <h4 className="font-semibold text-[var(--neon-cyan-bright)] text-sm mb-1">
              CORS Policy Blocked
            </h4>
            <p className="text-muted-foreground text-xs leading-relaxed">
              The backend container must be running in OrbStack for this request
              to succeed. Make sure your Lambda function container is active on
              port 9000.
            </p>
          </div>
          <button
            onClick={onDismiss}
            className="shrink-0 text-muted-foreground hover:text-foreground transition-colors"
            aria-label="Dismiss alert"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
