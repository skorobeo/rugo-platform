interface LinePoint {
  label: string
  values: number[]
}

const COLORS = ['#2f6bff', '#21a179', '#e5484d']

export function LineChart({ points, series }: { points: LinePoint[]; series: string[] }) {
  const width = 720
  const height = 240
  const pad = { top: 18, right: 16, bottom: 28, left: 34 }
  const innerW = width - pad.left - pad.right
  const innerH = height - pad.top - pad.bottom
  const max = Math.max(1, ...points.flatMap((point) => point.values))
  const x = (index: number) => pad.left + (points.length <= 1 ? innerW / 2 : (index / (points.length - 1)) * innerW)
  const y = (value: number) => pad.top + innerH - (value / max) * innerH
  const grid = [0, 0.25, 0.5, 0.75, 1]

  return (
    <div className="chart">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="График">
        {grid.map((fraction) => {
          const lineY = pad.top + innerH * fraction
          return (
            <g key={fraction}>
              <line x1={pad.left} x2={width - pad.right} y1={lineY} y2={lineY} className="chart__grid" />
              <text x={pad.left - 8} y={lineY + 4} textAnchor="end" className="chart__axis">
                {Math.round(max * (1 - fraction))}
              </text>
            </g>
          )
        })}
        {series.map((_, seriesIndex) => {
          const path = points
            .map((point, index) => `${index === 0 ? 'M' : 'L'}${x(index).toFixed(1)},${y(point.values[seriesIndex] ?? 0).toFixed(1)}`)
            .join(' ')
          return (
            <path
              key={seriesIndex}
              d={path}
              fill="none"
              stroke={COLORS[seriesIndex % COLORS.length]}
              strokeWidth={2.5}
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          )
        })}
        {points.map((point, index) => (
          <text key={point.label} x={x(index)} y={height - 8} textAnchor="middle" className="chart__axis">
            {point.label}
          </text>
        ))}
      </svg>
      <div className="chart__legend">
        {series.map((item, index) => (
          <span key={item}>
            <i style={{ background: COLORS[index % COLORS.length] }} />
            {item}
          </span>
        ))}
      </div>
    </div>
  )
}

export function Donut({ data }: { data: { label: string; value: number }[] }) {
  const total = data.reduce((sum, item) => sum + item.value, 0)
  if (total === 0) {
    return <div className="chart-empty">Нет данных</div>
  }
  const radius = 54
  const circumference = 2 * Math.PI * radius
  return (
    <div className="donut">
      <svg viewBox="0 0 140 140" role="img" aria-label="Распределение">
        {data.map((item, index) => {
          const length = (item.value / total) * circumference
          const offset = data
            .slice(0, index)
            .reduce((sum, previous) => sum + (previous.value / total) * circumference, 0)
          const dash = `${length} ${circumference - length}`
          return (
            <circle
              key={item.label}
              cx="70"
              cy="70"
              r={radius}
              fill="none"
              stroke={COLORS[index % COLORS.length]}
              strokeWidth="18"
              strokeDasharray={dash}
              strokeDashoffset={-offset}
              transform="rotate(-90 70 70)"
            />
          )
        })}
        <text x="70" y="66" textAnchor="middle" className="donut__value">{total}</text>
        <text x="70" y="84" textAnchor="middle" className="donut__label">всего</text>
      </svg>
      <div className="donut__legend">
        {data.map((item, index) => (
          <div key={item.label} className="donut__row">
            <span className="donut__dot" style={{ background: COLORS[index % COLORS.length] }} />
            <span className="donut__name">{item.label}</span>
            <span className="donut__count">{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export function Bars({ data }: { data: { label: string; value: number }[] }) {
  const max = Math.max(1, ...data.map((item) => item.value))
  return (
    <div className="bars">
      {data.length === 0 ? <div className="chart-empty">Нет данных</div> : null}
      {data.map((item) => (
        <div className="bars__row" key={item.label}>
          <span className="bars__label">{item.label}</span>
          <span className="bars__track">
            <span className="bars__fill" style={{ width: `${(item.value / max) * 100}%` }} />
          </span>
          <span className="bars__count">{item.value}</span>
        </div>
      ))}
    </div>
  )
}
