import { useState } from 'react'
import './Sidebar.css'

const PARTS = ['Part 2', 'Part 3', 'Part 4']

export default function Sidebar({ problems, selected, onSelect, onAdd, onDelete }) {
  const [collapsed, setCollapsed] = useState({})
  const [hoverId, setHoverId]     = useState(null)

  const toggle = (part) =>
    setCollapsed(c => ({ ...c, [part]: !c[part] }))

  const byPart = (part) => problems.filter(p => p.part === part)

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <span className="sidebar-title">Problems</span>
        <span className="sidebar-count">{problems.length}</span>
      </div>

      <div className="sidebar-scroll">
        {PARTS.map(part => {
          const items = byPart(part)
          const isOpen = !collapsed[part]
          return (
            <div key={part} className="part-group">
              <button
                className="part-header"
                onClick={() => toggle(part)}
              >
                <span className={`chevron ${isOpen ? 'open' : ''}`}>›</span>
                <span className="part-label">{part}</span>
                <span className="part-badge">{items.length}</span>
              </button>
              {isOpen && (
                <div className="part-items">
                  {items.map(p => (
                    <div
                      key={p.id}
                      className={`problem-item ${selected?.id === p.id ? 'active' : ''}`}
                      onClick={() => onSelect(p)}
                      onMouseEnter={() => setHoverId(p.id)}
                      onMouseLeave={() => setHoverId(null)}
                    >
                      <div className="problem-item-content">
                        <span className="problem-qnum">{p.questionNumber}</span>
                        <span className="problem-title">{p.title}</span>
                      </div>
                      <div className="problem-meta">
                        {p.inputType === 'STDIN' && (
                          <span className="badge-stdin" title="Requires stdin input">⌨</span>
                        )}
                        {p.id > 14 && hoverId === p.id && (
                          <button
                            className="btn-delete"
                            title="Delete problem"
                            onClick={e => { e.stopPropagation(); onDelete(p.id) }}
                          >×</button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )
        })}

        {/* User-added problems not in default parts */}
        {(() => {
          const extra = problems.filter(p => !PARTS.includes(p.part))
          if (!extra.length) return null
          return (
            <div className="part-group">
              <button className="part-header" onClick={() => toggle('Custom')}>
                <span className={`chevron ${!collapsed['Custom'] ? 'open' : ''}`}>›</span>
                <span className="part-label">Custom</span>
                <span className="part-badge">{extra.length}</span>
              </button>
              {!collapsed['Custom'] && (
                <div className="part-items">
                  {extra.map(p => (
                    <div
                      key={p.id}
                      className={`problem-item ${selected?.id === p.id ? 'active' : ''}`}
                      onClick={() => onSelect(p)}
                      onMouseEnter={() => setHoverId(p.id)}
                      onMouseLeave={() => setHoverId(null)}
                    >
                      <div className="problem-item-content">
                        <span className="problem-qnum">{p.questionNumber}</span>
                        <span className="problem-title">{p.title}</span>
                      </div>
                      <div className="problem-meta">
                        {p.inputType === 'STDIN' && (
                          <span className="badge-stdin">⌨</span>
                        )}
                        {hoverId === p.id && (
                          <button
                            className="btn-delete"
                            onClick={e => { e.stopPropagation(); onDelete(p.id) }}
                          >×</button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )
        })()}
      </div>

      <div className="sidebar-footer">
        <button className="btn-add-problem" onClick={onAdd}>
          <span>+</span> Add New Problem
        </button>
      </div>
    </aside>
  )
}
