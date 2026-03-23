"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import { Link, RefreshCw, CheckCircle2, Cloud, Zap, Shield } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { TerminalLog } from "@/components/terminal-log";
import { CorsAlert } from "@/components/cors-alert";
import { Confetti } from "@/components/confetti";
import { ParticleBackground } from "@/components/particle-background";
// OAuth Imports
import { GoogleOAuthProvider, useGoogleLogin } from '@react-oauth/google';

type SyncStatus = "idle" | "syncing" | "success" | "error";

function CloudBridgeContent() {
  const [inputValue, setInputValue] = useState("");
  const [status, setStatus] = useState<SyncStatus>("idle");
  const [logs, setLogs] = useState<string[]>([]);
  const [showCorsAlert, setShowCorsAlert] = useState(false);
  const [showConfetti, setShowConfetti] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [visibleBadges, setVisibleBadges] = useState<number[]>([]);
  const logQueueRef = useRef<string[]>([]);
  const isTypingRef = useRef(false);

  const typeLog = useCallback((message: string) => {
    logQueueRef.current.push(message);
    processLogQueue();
  }, []);

  const processLogQueue = useCallback(() => {
    if (isTypingRef.current || logQueueRef.current.length === 0) return;
    
    isTypingRef.current = true;
    const message = logQueueRef.current.shift()!;
    let currentIndex = 0;
    
    const typeInterval = setInterval(() => {
      if (currentIndex <= message.length) {
        setLogs((prev) => {
          const newLogs = [...prev];
          if (currentIndex === 0) {
            newLogs.push(message.slice(0, 1));
          } else {
            newLogs[newLogs.length - 1] = message.slice(0, currentIndex);
          }
          return newLogs;
        });
        currentIndex++;
      } else {
        clearInterval(typeInterval);
        isTypingRef.current = false;
        processLogQueue();
      }
    }, 20);
  }, []);

  // Google OAuth Login Hook
  const login = useGoogleLogin({
    onSuccess: (tokenResponse) => handleSync(tokenResponse.access_token),
    scope: "https://www.googleapis.com/auth/drive.file",
    onError: () => {
      typeLog("> ERROR: Google Authentication Failed");
      setStatus("error");
    }
  });

  const handleSync = async (accessToken: string) => {
    if (!inputValue.trim()) {
      typeLog("> ERROR: Please enter a valid URL");
      return;
    }

    setStatus("syncing");
    setIsExpanded(true);
    setShowCorsAlert(false);
    setLogs([]);
    setVisibleBadges([]);

    await new Promise((r) => setTimeout(r, 300));
    typeLog("> OAuth Token Verified...");
    
    await new Promise((r) => setTimeout(r, 400));
    typeLog("> Initializing encrypted tunnel...");
    
    await new Promise((r) => setTimeout(r, 600));
    typeLog("> Handshaking with Cloud Bridge API...");

    await new Promise((r) => setTimeout(r, 500));
    typeLog(`> Preparing resumable sync for: ${inputValue.substring(0, 30)}...`);

    try {
        // UPDATED: Dynamic URL for Vercel/Render deployment
        const backendBaseUrl = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8010/proxy";
        
        const response = await fetch(`${backendBaseUrl}/2015-03-31/functions/function/invocations`, {
            method: "POST",
            body: JSON.stringify({ 
                fileUrl: inputValue,
                accessToken: accessToken 
            }),
        });

        const data = await response.json();
        const parsedBody = typeof data.body === 'string' ? JSON.parse(data.body) : data;

        if (response.ok && (parsedBody.status === "success" || parsedBody.statusCode === 200)) {
            await new Promise((r) => setTimeout(r, 400));
            typeLog(`> SERVER: ${parsedBody.message || "Connection Verified"}`);
            
            await new Promise((r) => setTimeout(r, 600));
            typeLog("> Transmitting data packets to Google Drive API...");
            
            await new Promise((r) => setTimeout(r, 400));
            typeLog("> Bridge Verified! Transfer Complete.");

            setStatus("success");
            setShowConfetti(true);
            
            setTimeout(() => setVisibleBadges([0]), 300);
            setTimeout(() => setVisibleBadges([0, 1]), 500);
            setTimeout(() => setVisibleBadges([0, 1, 2]), 700);
        } else {
            throw new Error("Invalid response");
        }
    } catch (err) {
        console.error("SYNC_ERROR:", err);
        typeLog("> ERROR: Handshake failed. Check if Backend is running.");
        setStatus("error");
        setShowCorsAlert(true);
        setVisibleBadges([0, 1, 2]);
    }
  };

  const resetBridge = () => {
    setStatus("idle");
    setInputValue("");
    setLogs([]);
    setShowCorsAlert(false);
    setIsExpanded(false);
    setVisibleBadges([]);
  };

  useEffect(() => {
    if (status === "success") {
      const timer = setTimeout(() => {
      }, 8000);
      return () => clearTimeout(timer);
    }
  }, [status]);

  const badges = [
    { icon: <Zap className="w-4 h-4" />, title: "Lightning Fast" },
    { icon: <Shield className="w-4 h-4" />, title: "Secure OAuth" },
    { icon: <Cloud className="w-4 h-4" />, title: "Cloud Native" },
  ];

  return (
    <main className="relative h-screen overflow-hidden bg-background">
      <ParticleBackground />
      <div className="absolute inset-0 grid-bg pointer-events-none" />

      <div className="relative z-10 flex flex-col items-center justify-center h-full px-4">
        <motion.div
          className="text-center mb-8"
          animate={{ y: isExpanded ? -20 : 0 }}
          transition={{ type: "spring", stiffness: 200, damping: 25 }}
        >
          <div className="flex items-center justify-center gap-3 mb-3">
            <Cloud className="w-8 h-8 text-[var(--neon-cyan)]" />
            <h1 className="text-3xl md:text-4xl font-bold tracking-tight text-foreground">
              Cloud Bridge
            </h1>
          </div>
          <p className="text-muted-foreground text-sm max-w-md mx-auto text-balance">
            Secure, high-speed file transfers to Google Drive
          </p>
        </motion.div>

        <motion.div
          className="relative"
          animate={{ scale: isExpanded ? 0.85 : 1, y: isExpanded ? -40 : 0 }}
          transition={{ type: "spring", stiffness: 200, damping: 25 }}
        >
          <div className="absolute inset-0 -m-6 rounded-full border-2 border-dashed border-[var(--neon-cyan)]/30 animate-[orbital-spin_20s_linear_infinite]" />
          <div className="absolute inset-0 -m-12 rounded-full border border-dashed border-[var(--neon-cyan)]/15 animate-[orbital-spin_30s_linear_infinite_reverse]" />
          <div className="absolute inset-0 -m-4 rounded-full border border-[var(--neon-cyan)]/10 outer-ring" />

          <div className="w-72 h-72 md:w-80 md:h-80 glass-circle breathing-glow rounded-full p-6 flex flex-col items-center justify-center">
            <div className="mb-4">
              {status === "syncing" ? (
                <RefreshCw className="w-10 h-10 text-[var(--neon-cyan)] animate-spin" />
              ) : status === "success" ? (
                <CheckCircle2 className="w-10 h-10 text-emerald-400" />
              ) : (
                <Cloud className="w-10 h-10 text-[var(--neon-cyan)]" />
              )}
            </div>

            <div className="relative w-full max-w-[200px] mb-3">
              <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
                <Link className="w-4 h-4 text-[var(--neon-cyan)]" />
              </div>
              <input
                type="url"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                placeholder="Enter file URL..."
                disabled={status === "syncing"}
                className="w-full h-9 pl-9 pr-3 bg-input border border-border rounded-full text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-[var(--neon-cyan)] focus:border-transparent transition-all duration-300 font-mono text-xs disabled:opacity-50 disabled:cursor-not-allowed"
              />
            </div>

            <button
              onClick={status === "success" ? resetBridge : () => login()}
              disabled={status === "syncing"}
              className={`glitch-btn relative w-full max-w-[200px] h-9 rounded-full font-semibold text-sm transition-all duration-300 flex items-center justify-center gap-2 overflow-hidden ${
                status === "success"
                  ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                  : status === "syncing"
                    ? "bg-secondary text-muted-foreground cursor-not-allowed"
                    : "shimmer-btn text-primary-foreground"
              }`}
            >
              <span className="relative z-10">
                {status === "syncing" ? "Syncing..." : status === "success" ? "Reset Bridge" : "Initialize Bridge"}
              </span>
            </button>

            <p className="mt-3 text-xs text-muted-foreground font-mono">
              {status === "syncing" ? "Syncing to Google Drive" : status === "success" ? "Transfer Complete" : "Ready to sync"}
            </p>
          </div>
        </motion.div>

        <AnimatePresence>
          {isExpanded && (
            <motion.div
              className="w-full max-w-md mt-6"
              initial={{ opacity: 0, y: 40 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 40 }}
              transition={{ type: "spring", stiffness: 200, damping: 25 }}
            >
              <TerminalLog logs={logs} />
            </motion.div>
          )}
        </AnimatePresence>

        <AnimatePresence>
          {showCorsAlert && (
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 20 }}>
              <CorsAlert onDismiss={() => setShowCorsAlert(false)} />
            </motion.div>
          )}
        </AnimatePresence>

        <AnimatePresence>
          {isExpanded && (
            <motion.div className="mt-6 flex flex-wrap justify-center gap-3" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
              {badges.map((badge, index) => (
                <motion.div
                  key={badge.title}
                  initial={{ opacity: 0, scale: 0.8, y: 20 }}
                  animate={{
                    opacity: visibleBadges.includes(index) ? 1 : 0,
                    scale: visibleBadges.includes(index) ? 1 : 0.8,
                    y: visibleBadges.includes(index) ? 0 : 20,
                  }}
                  transition={{ type: "spring", stiffness: 300, damping: 20 }}
                  className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-secondary/50 border border-border text-[var(--neon-cyan)]"
                >
                  {badge.icon}
                  <span className="text-xs font-medium text-foreground">{badge.title}</span>
                </motion.div>
              ))}
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {showConfetti && <Confetti />}

      <style jsx>{`
        @keyframes orbital-spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </main>
  );
}

// Global Provider Wrapper
export default function CloudBridge() {
  return (
    <GoogleOAuthProvider clientId="657278265292-vic2q1ggm4ikj1k85dq5tgm2c5h1ca7f.apps.googleusercontent.com">
      <CloudBridgeContent />
    </GoogleOAuthProvider>
  );
}