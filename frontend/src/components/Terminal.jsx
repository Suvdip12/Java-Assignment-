import { useState, useEffect, useRef, useCallback } from 'react'
import './Terminal.css'

export default function Terminal({ code, runTrigger, problemId, onRunningChange }) {
  const [isRunning,  setIsRunning]  = useState(false)
  const [stats, setStats]           = useState({ compileMs: null, execMs: null })

  // All terminal text lives in a ref so stream callbacks never go stale.
  // A separate display-state is set from the ref to trigger React renders.
  const contentRef  = useRef('')
  const inputRef    = useRef('')
  const wsRef       = useRef(null)
  const termRef     = useRef(null)
  const isRunRef    = useRef(false)

  const [displayContent,  setDisplayContent]  = useState('')
  const [displayInput,    setDisplayInput]    = useState('')

  const flushContent = () => setDisplayContent(contentRef.current)
  const flushInput   = () => setDisplayInput(inputRef.current)

  const appendContent = useCallback((text) => {
    contentRef.current += text
    flushContent()
  }, [])

  // Auto-scroll on new output
  useEffect(() => {
    if (termRef.current) termRef.current.scrollTop = termRef.current.scrollHeight
  }, [displayContent, displayInput])

  useEffect(() => { onRunningChange?.(isRunning) }, [isRunning])

  // Clear terminal when user switches problem
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

  // Trigger run
  useEffect(() => {
    if (runTrigger > 0) startSession()
  }, [runTrigger])

  function closeWs() {
    if (wsRef.current) {
      wsRef.current.onclose = null
      wsRef.current.onerror = null
      wsRef.current.onmessage = null
      wsRef.current.close()
      wsRef.current = null
    }
  }

  function startSession() {
    closeWs()
    contentRef.current = ''
    inputRef.current   = ''
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
          if (msg.killed) {
            appendContent('\n[Killed]\n')
          } else if (msg.timeout) {
            appendContent('\n[⏱ Timed out — check for infinite loops or missing input]\n')
          } else {
            appendContent('\n[Exited with code ' + msg.code + ']\n')
          }
          isRunRef.current = false
          inputRef.current = ''
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

    ws.onclose = () => {
      isRunRef.current = false
      setIsRunning(false)
    }
    ws.onerror = () => {
      appendContent('\n[Connection error — is the server running?]\n')
      isRunRef.current = false
      setIsRunning(false)
    }
  }

  // Keyboard handler — uses refs so it never needs to be recreated
  const handleKeyDown = useCallback((e) => {
    if (!isRunRef.current) return

    if (e.key === 'Enter') {
      e.preventDefault()
      const line = inputRef.current
      // Echo typed input into the terminal history (with newline)
      contentRef.current += line + '\n'
      flushContent()
      inputRef.current = ''
      flushInput()
      wsRef.current?.send(JSON.stringify({ type: 'input', data: line + '\n' }))

    } else if (e.key === 'Backspace') {
      e.preventDefault()
      if (inputRef.current.length > 0) {
        inputRef.current = inputRef.current.slice(0, -1)
        flushInput()
      }

    } else if (e.ctrlKey && e.key === 'c') {
      e.preventDefault()
      contentRef.current += '^C\n'
      flushContent()
      inputRef.current = ''
      flushInput()
      wsRef.current?.send(JSON.stringify({ type: 'kill' }))
      isRunRef.current = false
      setIsRunning(false)
      closeWs()

    } else if (e.key.length === 1 && !e.ctrlKey && !e.metaKey) {
      e.preventDefault()
      inputRef.current += e.key
      flushInput()
    }
  }, [])   // stable — reads only from refs

  return (
    <div className="terminal-wrap" onClick={() => termRef.current?.focus()}>

      {/* ── Header ── */}
      <div className="terminal-header">
        <span className="terminal-title">
          <span className="terminal-icon">▸</span>
          Terminal
          {isRunning && <span className="terminal-live-dot" title="Process running" />}
        </span>
        <div className="terminal-stats">
          {stats.compileMs != null && <span className="stat compile">⚙ {stats.compileMs}ms compile</span>}
          {stats.execMs    != null && <span className="stat exec">⚡ {stats.execMs}ms exec</span>}
          {displayContent  && (
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
        tabIndex={0}
        onKeyDown={handleKeyDown}
        title={isRunning ? 'Type your input here, press Enter to send' : ''}
      >
        {!displayContent && !isRunning && (
          <div className="terminal-placeholder">
            <span className="terminal-prompt">$</span>
            <span className="terminal-blink">_</span>
            <span className="ph-text"> Click ▶ Run or press Ctrl+Enter to execute</span>
          </div>
        )}

        {/* All output as a single pre block, then the live typed input inline */}
        <pre className="terminal-text">
          {displayContent}
          {isRunning && <span className="terminal-live-input">{displayInput}</span>}
          {isRunning && <span className="terminal-cursor blink-cursor">█</span>}
        </pre>

        {isRunning && (
          <div className="terminal-input-hint">
            type &amp; press <kbd>Enter</kbd> to send · <kbd>Ctrl+C</kbd> to kill
          </div>
        )}
      </div>

    </div>
  )
}
