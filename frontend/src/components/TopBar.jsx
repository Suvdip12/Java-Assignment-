import './TopBar.css'

export default function TopBar({ sidebarOpen, onToggleSidebar }) {
  return (
    <header className="topbar">
      <div className="topbar-left">
        <button
          className="sidebar-toggle"
          onClick={onToggleSidebar}
          title={sidebarOpen ? 'Close sidebar' : 'Open sidebar'}
        >
          {sidebarOpen ? '☰' : '☰'}
        </button>
        <div className="topbar-logo">
          <span className="logo-icon">☕</span>
          <div>
            <span className="logo-title">Java Assignment Compiler</span>
            <span className="logo-sub">Live Online IDE</span>
          </div>
        </div>
        <div className="topbar-divider" />
        <div className="topbar-university">
          <span className="university-name">University of Kalyani</span>
          <span className="university-sub">Subject: Java Programming Lab &nbsp;·&nbsp; Paper Code: IT592</span>
        </div>
      </div>
      <div className="topbar-right">
        <div className="student-card">
          <div className="student-avatar">SM</div>
          <div className="student-info">
            <span className="student-name">Suvadip Mahato</span>
            <span className="student-roll">Roll No: 90/INT/230026 &nbsp;·&nbsp; 2025–2026</span>
          </div>
        </div>
      </div>
    </header>
  )
}
