import { useState } from 'react'
import './AddProblemModal.css'

const STARTER = `public class MyProgram {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}`

export default function AddProblemModal({ onAdd, onClose, nextId }) {
  const [form, setForm] = useState({
    part: 'Part 2',
    questionNumber: `Q${nextId}`,
    title: '',
    description: '',
    code: STARTER,
    inputType: 'NONE',
    defaultInput: '',
  })
  const [error, setError] = useState('')

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const submit = () => {
    if (!form.title.trim()) { setError('Title is required'); return }
    if (!form.code.trim())  { setError('Code is required'); return }
    onAdd(form)
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">Add New Problem</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        <div className="modal-body">
          <div className="form-row">
            <div className="form-group half">
              <label>Part</label>
              <select value={form.part} onChange={e => set('part', e.target.value)}>
                <option>Part 2</option>
                <option>Part 3</option>
                <option>Part 4</option>
                <option>Custom</option>
              </select>
            </div>
            <div className="form-group half">
              <label>Question No.</label>
              <input
                value={form.questionNumber}
                onChange={e => set('questionNumber', e.target.value)}
                placeholder="e.g. Q7"
              />
            </div>
          </div>

          <div className="form-group">
            <label>Title *</label>
            <input
              value={form.title}
              onChange={e => set('title', e.target.value)}
              placeholder="e.g. Fibonacci Series"
            />
          </div>

          <div className="form-group">
            <label>Description</label>
            <textarea
              value={form.description}
              onChange={e => set('description', e.target.value)}
              placeholder="Describe what this program does…"
              rows={3}
            />
          </div>

          <div className="form-group">
            <label>Java Code *</label>
            <textarea
              className="code-input"
              value={form.code}
              onChange={e => set('code', e.target.value)}
              rows={10}
              spellCheck={false}
            />
          </div>

          <div className="form-row">
            <div className="form-group half">
              <label>Input Type</label>
              <select value={form.inputType} onChange={e => set('inputType', e.target.value)}>
                <option value="NONE">No Input</option>
                <option value="STDIN">Scanner (stdin)</option>
              </select>
            </div>
            {form.inputType === 'STDIN' && (
              <div className="form-group half">
                <label>Default Input</label>
                <input
                  value={form.defaultInput}
                  onChange={e => set('defaultInput', e.target.value)}
                  placeholder="e.g. 5 (one per line)"
                />
              </div>
            )}
          </div>

          {error && <p className="form-error">⚠ {error}</p>}
        </div>

        <div className="modal-footer">
          <button className="btn-cancel" onClick={onClose}>Cancel</button>
          <button className="btn-submit" onClick={submit}>Add Problem</button>
        </div>
      </div>
    </div>
  )
}
