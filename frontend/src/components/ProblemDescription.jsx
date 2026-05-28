import './ProblemDescription.css'

export default function ProblemDescription({ problem, dirty }) {
  return (
    <div className="problem-desc">
      <div className="desc-left">
        <div className="desc-breadcrumb">
          <span className="bc-part">{problem.part}</span>
          <span className="bc-sep">›</span>
          <span className="bc-qnum">{problem.questionNumber}</span>
        </div>
        <h1 className="desc-title">{problem.title}</h1>
        <p className="desc-body">{problem.description}</p>
      </div>
      <div className="desc-right">
        <div className="desc-badges">
          <span className={`badge-type ${problem.inputType === 'STDIN' ? 'stdin' : 'none'}`}>
            {problem.inputType === 'STDIN' ? '⌨ Interactive' : '⚡ Auto-run'}
          </span>
          {dirty && <span className="badge-dirty">● Modified</span>}
        </div>
        <p className="desc-hint">Press <kbd>Ctrl</kbd>+<kbd>Enter</kbd> to run</p>
      </div>
    </div>
  )
}
