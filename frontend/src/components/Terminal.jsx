import { useState, useEffect, useRef, useCallback } from 'react'
import './Terminal.css'

export default function Terminal({ code, runTrigger, problemId, onRunningChange }) {
  const [isRunning,  setIsRunning]  = useState(false)
  const [stats,      setStats]      = useState({ compileMs: null, execMs: null })
  const [fontSize,   setFontSize]   = useState(13)

  const zoomIn  = () => setFontSize(s => Math.min(26, s + 1))
  const zoomOut = () => setFontSize(s => Math.max(10, s - 1))

  // All fast-changing data lives in refs so stream callbacks never go stale.
  const contentRef = useRef('')
  const inputRef   = useRef('')
  const wsRef      = useRef(null)
  const isRunRef   = useRef(false)

  // Two display states trigger React renders for content and current input line
  const [displayContent, setDisplayContent] = useState('')
  const [displayInput,   setDisplayInput]   = useState('')

  const termRef   = useRef(null)   // scrollable output div
  const ghostRef  = useRef(null)   // hidden <textarea> — the real input for mobile keyboard

  const flushContent = () => setDisplayContent(contentRef.current)
  const flushInput   = () => setDisplayInput(inputRef.current)

  const appendContent = useCallback((text) => {
    contentRef.current += text
    flushContent()
  }, [])

  // Auto-scroll to bottom on new output
  useEffect(() => {
    if (termRef.current) termRef.current.scrollTop = termRef.current.scrollHeight
  }, [displayContent, displayInput])

  useEffect(() => { onRunningChange?.(isRunning) }, [isRunning])

  // Clear terminal when problem is switched
  useEffect(() => {
    closeWs()
    contentRef.current = ''
    inputRef.current   = ''
    flushContent()
    flushInput()
    isRunRef.current = false
    setIsRunning(false)
    setStats({ compileMs: null, execMs: null })
  }, [problemId])

  // Trigger a new run
  useEffect(() => {
    if (runTrigger > 0) startSession()
  }, [runTrigger])

  function closeWs() {
    if (wsRef.current) {
      wsRef.current.onclose   = null
      wsRef.current.onerror   = null
      wsRef.current.onmessage = null
      wsRef.current.close()
      wsRef.current = null
    }
  }

  function focusGhost() {
    // Focuses the hidden textarea → shows keyboard on mobile
    ghostRef.current?.focus()
  }

  function startSession() {
    closeWs()
    contentRef.current = ''
    inputRef.current   = ''
    if (ghostRef.current) ghostRef.current.value = ''
    flushContent()
    flushInput()
    isRunRef.current = true
    setIsRunning(true)
    setStats({ compileMs: null, execMs: null })

    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const ws    = new WebSocket(`${proto}//${location.host}/ws/terminal`)
    wsRef.current = ws

    ws.onopen = () => ws.send(JSON.stringify({ type: 'run', code }))

    ws.onmessage = ({ data }) => {
      const msg = JSON.parse(data)
      switch (msg.type) {
        case 'compiled':
          setStats(s => ({ ...s, compileMs: msg.ms }))
          break
        case 'output':
        case 'stderr':
          appendContent(msg.data)
          break
        case 'compile_error':
          contentRef.current = '✗ Compile Error\n\n' + msg.data
          flushContent()
          isRunRef.current = false
          setIsRunning(false)
          break
        case 'exit':
          setStats(s => ({ ...s, execMs: msg.execMs }))
          appendContent(
            msg.killed   ? '\n[Killed]\n'
            : msg.timeout ? '\n[⏱ Timed out — infinite loop or missing input?]\n'
            :               '\n[Exited with code ' + msg.code + ']\n'
          )
          isRunRef.current = false
          inputRef.current = ''
          if (ghostRef.current) ghostRef.current.value = ''
          flushInput()
          setIsRunning(false)
          break
        case 'error':
          appendContent('\n[Error: ' + msg.data + ']\n')
          isRunRef.current = false
          setIsRunning(false)
          break
      }
    }

    ws.onclose = () => { isRunRef.current = false; setIsRunning(false) }
    ws.onerror = () => {
      appendContent('\n[Connection error — is the server running?]\n')
      isRunRef.current = false
      setIsRunning(false)
    }

    // Do NOT auto-focus here — programmatic focus on iOS Safari triggers
    // auto-zoom even at font-size 16px. User taps the terminal to type.
  }

  // ── Ghost textarea handlers ────────────────────────────────────────────────

  const sendLine = useCallback(() => {
    const line = inputRef.current
    contentRef.current += line + '\n'
    flushContent()
    inputRef.current = ''
    if (ghostRef.current) ghostRef.current.value = ''
    flushInput()
    wsRef.current?.send(JSON.stringify({ type: 'input', data: line + '\n' }))
  }, [])

  const killProcess = useCallback(() => {
    contentRef.current += '^C\n'
    flushContent()
    inputRef.current = ''
    if (ghostRef.current) ghostRef.current.value = ''
    flushInput()
    wsRef.current?.send(JSON.stringify({ type: 'kill' }))
    isRunRef.current = false
    setIsRunning(false)
    closeWs()
  }, [])

  // onInput fires for every character change — works on mobile AND desktop
  const handleGhostInput = useCallback((e) => {
    if (!isRunRef.current) return
    const val = e.target.value

    // Some mobile keyboards insert "\n" when Enter is tapped
    if (val.includes('\n')) {
      const lines  = val.split('\n')
      const toSend = lines[0]
      contentRef.current += toSend + '\n'
      flushContent()
      inputRef.current = lines[lines.length - 1]  // text after the newline
      e.target.value   = inputRef.current
      flushInput()
      wsRef.current?.send(JSON.stringify({ type: 'input', data: toSend + '\n' }))
    } else {
      inputRef.current = val
      flushInput()
    }
  }, [])

  // onKeyDown for desktop Enter / Ctrl+C; mobile may skip this for some keys
  const handleGhostKeyDown = useCallback((e) => {
    if (!isRunRef.current) return
    if (e.key === 'Enter') {
      e.preventDefault()
      sendLine()
    } else if (e.ctrlKey && e.key === 'c') {
      e.preventDefault()
      killProcess()
    }
  }, [sendLine, killProcess])

  return (
    <div className="terminal-wrap">

      {/* ── Header ── */}
      <div className="terminal-header">
        <span className="terminal-title">
          <span className="terminal-icon">▸</span>
          Terminal
          {isRunning && <span className="terminal-live-dot" title="Process running" />}
        </span>
        <div className="terminal-stats">
          {stats.compileMs != null && <span className="stat compile">⚙ {stats.compileMs}ms</span>}
          {stats.execMs    != null && <span className="stat exec">⚡ {stats.execMs}ms</span>}
          <div className="term-zoom-btns">
            <button className="btn-term-zoom" onClick={e => { e.stopPropagation(); zoomOut() }} title="Decrease font">A−</button>
            <button className="btn-term-zoom" onClick={e => { e.stopPropagation(); zoomIn()  }} title="Increase font">A+</button>
          </div>
          {displayContent && (
            <button
              className="btn-copy"
              title="Copy output"
              onClick={e => { e.stopPropagation(); navigator.clipboard.writeText(displayContent) }}
            >⎘</button>
          )}
        </div>
      </div>

      {/* ── Output + live input ── */}
      <div
        className="terminal-output"
        ref={termRef}
      >
        {!displayContent && !isRunning && (
          <div className="terminal-placeholder">
            <span className="terminal-prompt">$</span>
            <span className="terminal-blink">_</span>
            <span className="ph-text"> Click ▶ Run or press Ctrl+Enter</span>
          </div>
        )}

        <pre className="terminal-text" style={{ fontSize }}>
          {displayContent}
          {isRunning && <span className="terminal-live-input">{displayInput}</span>}
          {isRunning && <span className="terminal-cursor blink-cursor">█</span>}
        </pre>

        {isRunning && (
          <div className="terminal-input-hint">
            tap here to type · <kbd>Enter</kbd> to send · <kbd>Ctrl+C</kbd> to kill
          </div>
        )}

        {/*
          Ghost textarea — invisible but is a REAL input element.
          Mobile browsers open the keyboard when a real element is focused.
          fontSize 16px prevents iOS Safari from auto-zooming on focus.
        */}
        {/*
          Ghost textarea: when running it becomes a full transparent overlay so
          the user's tap lands DIRECTLY on a real input element.
          Native tap-to-focus = iOS never zooms.
          Programmatic .focus() from an onClick handler = iOS zooms — avoided.
          touch-action:pan-y lets vertical scroll propagate to .terminal-output.
        */}
        <textarea
          ref={ghostRef}
          className={`terminal-ghost-input${isRunning ? ' ghost-active' : ''}`}
          onInput={handleGhostInput}
          onKeyDown={handleGhostKeyDown}
          autoCapitalize="none"
          autoCorrect="off"
          autoComplete="off"
          spellCheck={false}
          rows={1}
          aria-hidden="true"
          tabIndex={-1}
        />
      </div>

    </div>
  )
}
