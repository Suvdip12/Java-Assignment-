import { useState, useEffect, useCallback, useRef } from 'react'
import TopBar from './components/TopBar'
import Sidebar from './components/Sidebar'
import ProblemDescription from './components/ProblemDescription'
import CodeEditor from './components/CodeEditor'
import Terminal from './components/Terminal'
import AddProblemModal from './components/AddProblemModal'
import './App.css'

const MIN_H    = 100
const MAX_H    = 9999
const DEFAULT_H = 300

export default function App() {
  const [problems, setProblems]         = useState([])
  const [selected, setSelected]         = useState(null)
  const [code, setCode]                 = useState('')
  const [running, setRunning]           = useState(false)
  const [runTrigger, setRunTrigger]     = useState(0)
  const [showAddModal, setShowAddModal] = useState(false)
  const [dirty, setDirty]               = useState(false)
  const [saving, setSaving]             = useState(false)
  const [saveFlash, setSaveFlash]       = useState(false)
  const [sidebarOpen, setSidebarOpen]   = useState(true)
  const [terminalH, setTerminalH]       = useState(DEFAULT_H)
  const [editorFontSize, setEditorFontSize] = useState(13.5)

  const editorZoomIn  = () => setEditorFontSize(s => Math.min(28, s + 1))
  const editorZoomOut = () => setEditorFontSize(s => Math.max(10, s - 1))

  const dragRef = useRef({ active: false, startY: 0, startH: 0 })

  // ── Drag-to-resize (pointer capture) ─────────────────────────────────────
  const onHandlePointerDown = (e) => {
    e.preventDefault()
    e.currentTarget.setPointerCapture(e.pointerId)
    dragRef.current = { active: true, startY: e.clientY, startH: terminalH }
  }
  const onHandlePointerMove = (e) => {
    if (!dragRef.current.active) return
    const delta = dragRef.current.startY - e.clientY
    setTerminalH(Math.min(MAX_H, Math.max(MIN_H, dragRef.current.startH + delta)))
  }
  const onHandlePointerUp = () => { dragRef.current.active = false }

  // ── Load problems ─────────────────────────────────────────────────────────
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
    setDirty(false)
  }

  const handleCodeChange = (val) => {
    setCode(val)
    setDirty(val !== selected?.code)
  }

  const runCode = useCallback(() => {
    if (running) return
    setRunTrigger(t => t + 1)
  }, [running])

  const saveCode = useCallback(async () => {
    if (!selected || !dirty || saving) return
    setSaving(true)
    try {
      await fetch(`/api/problems/${selected.id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code }),
      })
      setSelected(prev => ({ ...prev, code }))
      setDirty(false)
      setSaveFlash(true)
      setTimeout(() => setSaveFlash(false), 1500)
    } finally {
      setSaving(false)
    }
  }, [selected, dirty, saving, code])

  const downloadCode = () => {
    if (!selected) return
    const filename = (selected.questionNumber || 'Main') + '.java'
    const blob = new Blob([code], { type: 'text/plain' })
    const url  = URL.createObjectURL(blob)
    const a    = document.createElement('a')
    a.href = url; a.download = filename; a.click()
    URL.revokeObjectURL(url)
  }

  const resetCode = async () => {
    if (!selected) return
    const res  = await fetch(`/api/problems/${selected.id}/reset`, { method: 'POST' })
    const data = await res.json()
    setCode(data.code)
    setSelected(prev => ({ ...prev, code: data.code }))
    setDirty(false)
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
      else { setSelected(null); setCode('') }
    }
  }

  useEffect(() => {
    const h = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); runCode() }
      if ((e.ctrlKey || e.metaKey) && e.key === 's')     { e.preventDefault(); saveCode() }
    }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [runCode, saveCode])

  return (
    <div className="app">
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
                      <button
                        className="btn-ghost btn-download"
                        onClick={downloadCode}
                        title="Download as .java file"
                      >⬇ Download</button>
                      {dirty && (
                        <button
                          className={`btn-save ${saveFlash ? 'save-flash' : ''}`}
                          onClick={saveCode}
                          disabled={saving}
                          title="Save changes (Ctrl+S)"
                        >
                          {saving ? '…' : saveFlash ? '✓ Saved' : '💾 Save'}
                        </button>
                      )}
                      {dirty && (
                        <button className="btn-ghost" onClick={resetCode} title="Reset to original">
                          ↩ Reset
                        </button>
                      )}
                      <div className="zoom-btns">
                        <button className="btn-zoom" onClick={editorZoomOut} title="Decrease font size">A−</button>
                        <button className="btn-zoom" onClick={editorZoomIn}  title="Increase font size">A+</button>
                      </div>
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
                  <CodeEditor value={code} onChange={handleCodeChange} fontSize={editorFontSize} />
                </div>

                {/* ── Drag Handle ── */}
                <div
                  className="resize-handle"
                  onPointerDown={onHandlePointerDown}
                  onPointerMove={onHandlePointerMove}
                  onPointerUp={onHandlePointerUp}
                  onPointerCancel={onHandlePointerUp}
                  title="Drag to resize terminal"
                />

                {/* ── Interactive Terminal ── */}
                <div className="terminal-pane" style={{ height: terminalH }}>
                  <Terminal
                    code={code}
                    runTrigger={runTrigger}
                    problemId={selected.id}
                    onRunningChange={setRunning}
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
