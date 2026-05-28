import { useEffect, useRef } from 'react'
import './Terminal.css'

export default function Terminal({ result, running, stdin, onStdinChange, inputType }) {
  const outputRef = useRef(null)
  const inputRef  = useRef(null)

  useEffect(() => {
    if (outputRef.current) {
      outputRef.current.scrollTop = outputRef.current.scrollHeight
    }
  }, [result])

  const copyOutput = () => {
    if (result?.output) navigator.clipboard.writeText(result.output)
  }

  return (
    <div className="terminal-wrap">
      <div className="terminal-header">
        <span className="terminal-title">
          <span className="terminal-icon">▸</span> Terminal
        </span>
        {result && (
          <div className="terminal-stats">
            {result.success && (
              <>
                <span className="stat compile">⚙ {result.compileTimeMs}ms compile</span>
                <span className="stat exec">⚡ {result.executionTimeMs}ms exec</span>
              </>
            )}
            <button className="btn-copy" onClick={copyOutput} title="Copy output">⎘</button>
          </div>
        )}
      </div>

      {/* ── Output ── */}
      <div className="terminal-output" ref={outputRef}>
        {!result && !running && (
          <div className="terminal-placeholder">
            <span className="terminal-prompt">$</span>
            <span className="terminal-blink">_</span>
            <span className="ph-text"> Click ▶ Run or press Ctrl+Enter to execute</span>
          </div>
        )}

        {running && (
          <div className="terminal-loading">
            <div className="loading-dots"><span /><span /><span /></div>
            <span>Compiling &amp; running…</span>
          </div>
        )}

        {result && !running && (
          <div className={`terminal-result ${result.success ? 'success' : 'error'}`}>
            {result.compileError && (
              <div className="error-block">
                <div className="error-label">✗ Compile Error</div>
                <pre className="error-text">{result.compileError}</pre>
              </div>
            )}
            {result.runtimeError && (
              <div className="error-block">
                <div className="error-label">✗ Runtime Error</div>
                <pre className="error-text">{result.runtimeError}</pre>
              </div>
            )}
            {result.output && result.output !== '(no output)' && (
              <pre className="output-text">{result.output}</pre>
            )}
            {result.output === '(no output)' && (
              <span className="no-output">Program executed with no output.</span>
            )}
            {result.success && (
              <div className="success-bar">
                <span className="success-icon">✓</span> Execution successful
              </div>
            )}
          </div>
        )}
      </div>

      {/* ── Inline stdin — looks like a real terminal input line ── */}
      <div
        className={`terminal-input-area ${inputType === 'STDIN' && !stdin.trim() ? 'input-warn' : ''}`}
        onClick={() => inputRef.current?.focus()}
      >
        <div className="input-area-header">
          <span className="input-area-label">
            <span className="input-prompt-char">›</span> stdin
          </span>
          {inputType === 'STDIN' && !stdin.trim() && (
            <span className="input-warn-badge">⚠ empty — program will time out</span>
          )}
          {inputType !== 'STDIN' && (
            <span className="input-optional-badge">optional</span>
          )}
        </div>
        <div className="input-line-wrap">
          <span className="input-gutter-char">$</span>
          <textarea
            ref={inputRef}
            className="terminal-stdin-input"
            value={stdin}
            onChange={e => onStdinChange(e.target.value)}
            placeholder={inputType === 'STDIN' ? 'one value per line…' : 'no input needed'}
            rows={3}
            spellCheck={false}
          />
        </div>
      </div>
    </div>
  )
}
