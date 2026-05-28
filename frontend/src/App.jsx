import { useState, useEffect, useCallback, useRef } from 'react'
import TopBar from './components/TopBar'
import Sidebar from './components/Sidebar'
import ProblemDescription from './components/ProblemDescription'
import CodeEditor from './components/CodeEditor'
import Terminal from './components/Terminal'
import AddProblemModal from './components/AddProblemModal'
import './App.css'

const MIN_H = 100
const MAX_H = 620
const DEFAULT_H = 280

export default function App() {
  const [problems, setProblems]         = useState([])
  const [selected, setSelected]         = useState(null)
  const [code, setCode]                 = useState('')
  const [stdin, setStdin]               = useState('')
  const [result, setResult]             = useState(null)
  const [running, setRunning]           = useState(false)
  const [showAddModal, setShowAddModal] = useState(false)
  const [dirty, setDirty]               = useState(false)
  const [sidebarOpen, setSidebarOpen]   = useState(true)
  const [terminalH, setTerminalH]       = useState(DEFAULT_H)
  const [isDragging, setIsDragging]     = useState(false)  // overlay while dragging

  const dragRef = useRef({ active: false, startY: 0, startH: 0 })

  // ── Drag-to-resize terminal ────────────────────────────────────────────────
  const onResizeStart = (e) => {
    e.preventDefault()
    dragRef.current = { active: true, startY: e.clientY, startH: terminalH }
    setIsDragging(true)
  }

  useEffect(() => {
    const onMove = (e) => {
      if (!dragRef.current.active) return
      const delta = dragRef.current.startY - e.clientY   // up = bigger
      const newH  = Math.min(MAX_H, Math.max(MIN_H, dragRef.current.startH + delta))
      setTerminalH(newH)
    }
    const onUp = () => {
      if (!dragRef.current.active) return
      dragRef.current.active = false
      setIsDragging(false)
    }
    window.addEventListener('mousemove', onMove)
    window.addEventListener('mouseup', onUp)
    return () => {
      window.removeEventListener('mousemove', onMove)
      window.removeEventListener('mouseup', onUp)
    }
  }, [])

  // ── Load problems ──────────────────────────────────────────────────────────
  useEffect(() => {
    fetch('/api/problems')
      .then(r => r.json())
      .then(data => {
        setProblems(data)
        if (data.length > 0) selectProblem(data[0])
      })
  }, [])

  const selectProblem = (p) => {
    setSelected(p)
    setCode(p.code)
    setStdin(p.defaultInput || '')
    setResult(null)
    setDirty(false)
  }

  const handleCodeChange = (val) => {
    setCode(val)
    setDirty(val !== selected?.code)
  }

  const runCode = useCallback(async () => {
    if (!code.trim() || running) return
    setRunning(true)
    setResult(null)
    try {
      const res = await fetch('/api/compile', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code, stdin }),
      })
      setResult(await res.json())
    } catch (e) {
      setResult({ success: false, runtimeError: 'Network error: ' + e.message })
    } finally {
      setRunning(false)
    }
  }, [code, stdin, running])

  const resetCode = async () => {
    if (!selected) return
    const res  = await fetch(`/api/problems/${selected.id}/reset`, { method: 'POST' })
    const data = await res.json()
    setCode(data.code)
    setDirty(false)
    setResult(null)
  }

  const addProblem = async (p) => {
    const res  = await fetch('/api/problems', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(p),
    })
    const newP = await res.json()
    setProblems(prev => [...prev, newP])
    setShowAddModal(false)
    selectProblem(newP)
  }

  const deleteProblem = async (id) => {
    await fetch(`/api/problems/${id}`, { method: 'DELETE' })
    const updated = problems.filter(p => p.id !== id)
    setProblems(updated)
    if (selected?.id === id) {
      if (updated.length > 0) selectProblem(updated[0])
      else { setSelected(null); setCode(''); setResult(null) }
    }
  }

  useEffect(() => {
    const h = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); runCode() }
    }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [runCode])

  return (
    <div className="app">
      {/* Full-screen drag overlay — blocks Monaco from stealing mouse events */}
      {isDragging && <div className="drag-overlay" />}

      <TopBar sidebarOpen={sidebarOpen} onToggleSidebar={() => setSidebarOpen(v => !v)} />

      <div className="workspace">
        {sidebarOpen && (
          <Sidebar
            problems={problems}
            selected={selected}
            onSelect={selectProblem}
            onAdd={() => setShowAddModal(true)}
            onDelete={deleteProblem}
            onClose={() => setSidebarOpen(false)}
          />
        )}

        <main className="main">
          {selected ? (
            <>
              <ProblemDescription problem={selected} dirty={dirty} />

              <div className="editor-terminal">

                {/* ── Code Editor ── */}
                <div className="editor-pane">
                  <div className="pane-header">
                    <span className="pane-title">
                      <span className="dot dot-red" />
                      <span className="dot dot-yellow" />
                      <span className="dot dot-green" />
                      &nbsp;&nbsp;{selected.questionNumber}.java
                    </span>
                    <div className="pane-actions">
                      {dirty && (
                        <button className="btn-ghost" onClick={resetCode} title="Reset to original">
                          ↩ Reset
                        </button>
                      )}
                      <button
                        className={`btn-run ${running ? 'running' : ''}`}
                        onClick={runCode}
                        disabled={running}
                        title="Run (Ctrl+Enter)"
                      >
                        {running
                          ? <><span className="spinner" /> Running…</>
                          : <>▶&nbsp;Run</>}
                      </button>
                    </div>
                  </div>
                  <CodeEditor value={code} onChange={handleCodeChange} />
                </div>

                {/* ── Drag Handle ── */}
                <div
                  className={`resize-handle ${isDragging ? 'dragging' : ''}`}
                  onMouseDown={onResizeStart}
                  title="Drag up / down to resize terminal"
                />

                {/* ── Terminal ── */}
                <div className="terminal-pane" style={{ height: terminalH }}>
                  <Terminal
                    result={result}
                    running={running}
                    stdin={stdin}
                    onStdinChange={setStdin}
                    inputType={selected.inputType}
                  />
                </div>

              </div>
            </>
          ) : (
            <div className="empty-state">
              <div className="empty-icon">☕</div>
              <h2>Java Assignment Compiler</h2>
              <p>Select a problem from the sidebar to begin</p>
            </div>
          )}
        </main>
      </div>

      {showAddModal && (
        <AddProblemModal
          onAdd={addProblem}
          onClose={() => setShowAddModal(false)}
          nextId={problems.length + 1}
        />
      )}
    </div>
  )
}
