import { useState, useEffect, useCallback } from 'react'
import TopBar from './components/TopBar'
import Sidebar from './components/Sidebar'
import ProblemDescription from './components/ProblemDescription'
import CodeEditor from './components/CodeEditor'
import Terminal from './components/Terminal'
import AddProblemModal from './components/AddProblemModal'
import './App.css'

export default function App() {
  const [problems, setProblems]       = useState([])
  const [selected, setSelected]       = useState(null)
  const [code, setCode]               = useState('')
  const [stdin, setStdin]             = useState('')
  const [result, setResult]           = useState(null)
  const [running, setRunning]         = useState(false)
  const [showAddModal, setShowAddModal] = useState(false)
  const [dirty, setDirty]             = useState(false)

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
      const data = await res.json()
      setResult(data)
    } catch (e) {
      setResult({ success: false, runtimeError: 'Network error: ' + e.message })
    } finally {
      setRunning(false)
    }
  }, [code, stdin, running])

  const resetCode = async () => {
    if (!selected) return
    const res = await fetch(`/api/problems/${selected.id}/reset`, { method: 'POST' })
    const data = await res.json()
    setCode(data.code)
    setDirty(false)
    setResult(null)
  }

  const addProblem = async (p) => {
    const res = await fetch('/api/problems', {
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

  // Keyboard shortcut: Ctrl+Enter to run
  useEffect(() => {
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault()
        runCode()
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [runCode])

  return (
    <div className="app">
      <TopBar />
      <div className="workspace">
        <Sidebar
          problems={problems}
          selected={selected}
          onSelect={selectProblem}
          onAdd={() => setShowAddModal(true)}
          onDelete={deleteProblem}
        />
        <main className="main">
          {selected ? (
            <>
              <ProblemDescription problem={selected} dirty={dirty} />
              <div className="editor-terminal">
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
                        {running ? (
                          <><span className="spinner" /> Running…</>
                        ) : (
                          <>▶ Run</>
                        )}
                      </button>
                    </div>
                  </div>
                  <CodeEditor value={code} onChange={handleCodeChange} />
                </div>
                <div className="terminal-pane">
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
