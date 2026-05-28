import Editor from '@monaco-editor/react'

const THEME_DEF = {
  base: 'vs-dark',
  inherit: true,
  rules: [
    { token: 'keyword',        foreground: 'ff79c6', fontStyle: 'bold' },
    { token: 'type',           foreground: '8be9fd' },
    { token: 'string',         foreground: 'f1fa8c' },
    { token: 'comment',        foreground: '6272a4', fontStyle: 'italic' },
    { token: 'number',         foreground: 'bd93f9' },
    { token: 'delimiter',      foreground: 'f8f8f2' },
    { token: 'identifier',     foreground: 'f8f8f2' },
    { token: 'annotation',     foreground: '50fa7b' },
  ],
  colors: {
    'editor.background':              '#1e1e2e',
    'editor.foreground':              '#f8f8f2',
    'editorLineNumber.foreground':    '#44475a',
    'editorLineNumber.activeForeground': '#f8f8f2',
    'editor.lineHighlightBackground': '#2d2d44',
    'editorCursor.foreground':        '#f8f8f2',
    'editor.selectionBackground':     '#44475a',
    'editorIndentGuide.background':   '#2d2d44',
    'editorGutter.background':        '#1e1e2e',
    'scrollbarSlider.background':     '#44475a55',
    'scrollbarSlider.hoverBackground':'#44475a88',
  }
}

export default function CodeEditor({ value, onChange }) {
  const handleMount = (editor, monaco) => {
    monaco.editor.defineTheme('dracula-custom', THEME_DEF)
    monaco.editor.setTheme('dracula-custom')
  }

  return (
    <div style={{ flex: 1, overflow: 'hidden' }}>
      <Editor
        height="100%"
        language="java"
        value={value}
        onChange={onChange}
        theme="dracula-custom"
        onMount={handleMount}
        options={{
          fontSize: 13.5,
          fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
          fontLigatures: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          lineNumbers: 'on',
          glyphMargin: false,
          folding: true,
          renderLineHighlight: 'line',
          tabSize: 4,
          insertSpaces: true,
          wordWrap: 'off',
          smoothScrolling: true,
          cursorBlinking: 'smooth',
          cursorSmoothCaretAnimation: 'on',
          padding: { top: 12, bottom: 12 },
          scrollbar: {
            verticalScrollbarSize: 6,
            horizontalScrollbarSize: 6,
          },
          suggest: { showIcons: true },
          quickSuggestions: true,
          bracketPairColorization: { enabled: true },
          renderWhitespace: 'none',
          stickyScroll: { enabled: false },
        }}
      />
    </div>
  )
}
