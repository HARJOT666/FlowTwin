import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link, useLocation } from 'react-router-dom';
import { 
  Activity, Clock, Users, Bed, AlertTriangle, CheckCircle, 
  BarChart3, LayoutDashboard, BrainCircuit, History, Server,
  ChevronRight, TrendingUp, Info, ActivitySquare
} from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { api, mockScenarioBaseline } from './apiService';
import type { TwinMetrics, ScenarioResult, ForecastData } from './apiService';
import './index.css';

// --- MAIN LAYOUT COMPONENT ---
function Layout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const [lastUpdate, setLastUpdate] = useState(new Date());
  
  useEffect(() => {
    const interval = setInterval(() => setLastUpdate(new Date()), 12000);
    return () => clearInterval(interval);
  }, []);

  const NavItem = ({ to, icon: Icon, label }: { to: string, icon: any, label: string }) => {
    const active = location.pathname === to;
    return (
      <Link to={to} className={`nav-item ${active ? 'active' : ''}`}>
        <Icon size={18} />
        {label}
      </Link>
    );
  };

  return (
    <div className="app-layout">
      {/* Sidebar */}
      <nav className="sidebar">
        <div style={{ padding: '24px 16px', display: 'flex', alignItems: 'center', gap: '12px', borderBottom: '1px solid var(--border-subtle)' }}>
          <ActivitySquare color="var(--accent-primary)" size={24} />
          <span style={{ fontSize: '18px', fontWeight: 600, color: 'var(--text-primary)' }}>FlowTwin</span>
        </div>
        
        <div className="nav-group-label">Overview</div>
        <NavItem to="/" icon={LayoutDashboard} label="Dashboard" />
        
        <div className="nav-group-label">Operations</div>
        <NavItem to="/flow" icon={Activity} label="Live Flow" />
        <NavItem to="/scenarios" icon={Clock} label="Scenarios" />
        
        <div className="nav-group-label">Intelligence</div>
        <NavItem to="/intelligence" icon={BrainCircuit} label="AI Insights" />
        <NavItem to="/forecast" icon={BarChart3} label="Forecast" />
        
        <div className="nav-group-label">System</div>
        <NavItem to="/history" icon={History} label="History" />
        <NavItem to="/system" icon={Server} label="Status" />

        <div style={{ marginTop: 'auto', padding: '16px', borderTop: '1px solid var(--border-subtle)' }}>
          <div style={{ fontSize: '12px', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--status-healthy)' }}></span>
            Live Connection
          </div>
          <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px', paddingLeft: '16px' }}>
            Updated {lastUpdate.toLocaleTimeString()}
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="main-content">
        {children}
      </main>
    </div>
  );
}

// --- DASHBOARD PAGE ---
function Dashboard() {
  const [metrics, setMetrics] = useState<TwinMetrics | null>(null);
  const [forecast, setForecast] = useState<ForecastData[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([api.getTwinMetrics(), api.getForecast()]).then(([m, f]) => {
      setMetrics(m);
      setForecast(f);
      setLoading(false);
    });
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
        <div>
          <h1 className="page-title">Operations Dashboard</h1>
          <p className="text-secondary">Loading real-time twin state...</p>
        </div>
        <div className="dashboard-grid">
           <div className="card skeleton" style={{ gridColumn: 'span 3', height: '120px' }}></div>
           <div className="card skeleton" style={{ gridColumn: 'span 3', height: '120px' }}></div>
           <div className="card skeleton" style={{ gridColumn: 'span 3', height: '120px' }}></div>
           <div className="card skeleton" style={{ gridColumn: 'span 3', height: '120px' }}></div>
        </div>
      </div>
    );
  }

  if (!metrics) return <div>Backend Unavailable</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '40px' }}>
      
      {/* Header */}
      <div>
        <h1 className="page-title">Operations Dashboard</h1>
        <p className="text-secondary">Real-time overview of emergency department patient flow.</p>
      </div>

      {/* KPI Overview */}
      <section className="dashboard-grid">
        <KPICard label="Total Patients" value={metrics.patients} unit="" trend="↑ 4% from last hour" status="neutral" />
        <KPICard label="Average Wait" value={metrics.avgWaitMin} unit="min" trend="↑ 12% from last hour" status="warning" />
        <KPICard label="P90 Wait Time" value={metrics.p90WaitMin} unit="min" trend="Critical threshold breached" status="critical" />
        <div className="card" style={{ gridColumn: 'span 3' }}>
          <div className="text-label">Queue Pressure</div>
          <div className="text-kpi" style={{ marginTop: '8px' }}>
            <span className={`badge ${metrics.queuePressure.toLowerCase()}`}>{metrics.queuePressure}</span>
          </div>
          <div className="text-secondary" style={{ marginTop: 'auto' }}>Elevated across 2 zones</div>
        </div>
      </section>

      {/* Live Patient Flow */}
      <section className="card">
        <div className="card-header">
          <h2 className="section-title">Live Patient Flow</h2>
          <span className="badge info">Real-Time</span>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '24px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-sm)' }}>
          <FlowStage name="Arrival" count={12} icon={<Users size={18}/>} />
          <FlowArrow />
          <FlowStage name="Triage" count={24} icon={<Activity size={18}/>} isBottleneck />
          <FlowArrow />
          <FlowStage name="Waiting" count={45} icon={<Clock size={18}/>} />
          <FlowArrow />
          <FlowStage name="Treatment" count={32} icon={<CheckCircle size={18}/>} />
          <FlowArrow />
          <FlowStage name="Bed" count={11} icon={<Bed size={18}/>} />
        </div>
      </section>

      {/* Two-Column: Bottleneck Intelligence & Forecast */}
      <section className="dashboard-grid">
        {/* Intelligence Panel */}
        <div className="card" style={{ gridColumn: 'span 6' }}>
          <div className="card-header">
            <h2 className="section-title">Operations Intelligence</h2>
            <span className="badge neutral">AI-Assisted</span>
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <div>
              <div className="text-label" style={{ marginBottom: '8px' }}>Primary Bottleneck</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <div style={{ fontSize: '24px', fontWeight: 600, color: 'var(--status-critical)' }}>{metrics.bottleneck}</div>
                <div className="badge critical">High Pressure</div>
              </div>
            </div>
            
            <div style={{ padding: '16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-sm)', borderLeft: '3px solid var(--accent-primary)' }}>
              <div className="text-label" style={{ marginBottom: '4px' }}>Assessment</div>
              <div className="text-body">{metrics.bottleneckReason}</div>
            </div>
            
            <div>
              <div className="text-label" style={{ marginBottom: '4px' }}>Recommended Action</div>
              <div className="text-body" style={{ fontWeight: 500 }}>{metrics.bottleneckAction}</div>
            </div>
          </div>
        </div>

        {/* Forecast Chart */}
        <div className="card" style={{ gridColumn: 'span 6' }}>
          <div className="card-header">
            <h2 className="section-title">Arrival Forecast (4H)</h2>
            <span className="badge warning">High Surge Risk</span>
          </div>
          <div style={{ height: '220px', marginTop: '16px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={forecast} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorArrivals" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--accent-primary)" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="var(--accent-primary)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-subtle)" />
                <XAxis dataKey="time" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: 'var(--text-secondary)' }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: 'var(--text-secondary)' }} />
                <Tooltip 
                  contentStyle={{ backgroundColor: 'var(--bg-surface)', border: '1px solid var(--border-subtle)', borderRadius: '4px', fontSize: '13px' }} 
                  itemStyle={{ color: 'var(--text-primary)', fontWeight: 600 }}
                />
                <Area type="monotone" dataKey="arrivals" stroke="var(--accent-primary)" strokeWidth={2} fillOpacity={1} fill="url(#colorArrivals)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>

      {/* What-If Simulation */}
      <SimulationPanel />
      
    </div>
  );
}

// --- SUBCOMPONENTS ---

function KPICard({ label, value, unit, trend, status }: { label: string, value: string | number, unit: string, trend: string, status: 'neutral' | 'warning' | 'critical' }) {
  const statusColor = status === 'warning' ? 'var(--status-warning)' : status === 'critical' ? 'var(--status-critical)' : 'var(--text-primary)';
  return (
    <div className="card" style={{ gridColumn: 'span 3' }}>
      <div className="text-label">{label}</div>
      <div className="text-kpi" style={{ marginTop: '8px', color: statusColor }}>
        {value} <span style={{ fontSize: '16px', fontWeight: 500, color: 'var(--text-secondary)' }}>{unit}</span>
      </div>
      <div className="text-secondary" style={{ marginTop: 'auto', display: 'flex', alignItems: 'center', gap: '4px' }}>
        {trend}
      </div>
    </div>
  );
}

function FlowStage({ name, count, icon, isBottleneck }: { name: string, count: number, icon: any, isBottleneck?: boolean }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
      <div style={{ 
        width: '40px', height: '40px', borderRadius: '50%', 
        background: 'var(--bg-surface)', 
        color: isBottleneck ? 'var(--status-critical)' : 'var(--text-primary)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        border: isBottleneck ? '2px solid var(--status-critical)' : '1px solid var(--border-strong)',
        boxShadow: isBottleneck ? '0 0 0 4px var(--status-critical-bg)' : 'none'
      }}>
        {icon}
      </div>
      <div style={{ textAlign: 'center' }}>
        <div className="text-label" style={{ color: isBottleneck ? 'var(--status-critical)' : 'var(--text-primary)' }}>{name}</div>
        <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-secondary)', marginTop: '2px' }}>{count}</div>
      </div>
    </div>
  );
}

function FlowArrow() {
  return (
    <div style={{ flex: 1, height: '1px', background: 'var(--border-strong)', margin: '0 16px', position: 'relative', top: '-16px' }}>
      <div style={{ position: 'absolute', right: '0px', top: '-3px', width: '6px', height: '6px', borderTop: '1px solid var(--border-strong)', borderRight: '1px solid var(--border-strong)', transform: 'rotate(45deg)' }}></div>
    </div>
  );
}

function SimulationPanel() {
  const [nurses, setNurses] = useState(2);
  const [beds, setBeds] = useState(4);
  const [simState, setSimState] = useState<'idle' | 'running' | 'done'>('idle');
  const [result, setResult] = useState<ScenarioResult | null>(null);

  const handleSimulate = async () => {
    setSimState('running');
    const res = await api.runScenario(nurses, beds);
    setResult(res);
    setSimState('done');
  };

  return (
    <section className="card">
      <div className="card-header" style={{ borderBottom: '1px solid var(--border-subtle)', paddingBottom: '16px', marginBottom: '24px' }}>
        <div>
          <h2 className="section-title">What-If Simulation</h2>
          <p className="text-secondary" style={{ marginTop: '4px' }}>Model operational changes against live twin data</p>
        </div>
      </div>

      <div className="dashboard-grid">
        {/* Controls */}
        <div style={{ gridColumn: 'span 5', display: 'flex', flexDirection: 'column', gap: '24px', paddingRight: '24px', borderRight: '1px solid var(--border-subtle)' }}>
          <div>
            <div className="text-label" style={{ marginBottom: '12px' }}>Triage Nurses</div>
            <input type="range" min="1" max="5" value={nurses} onChange={e => setNurses(Number(e.target.value))} disabled={simState === 'running'} />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '8px', fontSize: '13px', fontWeight: 600 }}>
              <span>1</span><span style={{ color: 'var(--accent-primary)' }}>{nurses}</span><span>5</span>
            </div>
          </div>
          
          <div>
            <div className="text-label" style={{ marginBottom: '12px' }}>Fast-Track Beds</div>
            <input type="range" min="0" max="10" value={beds} onChange={e => setBeds(Number(e.target.value))} disabled={simState === 'running'} />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '8px', fontSize: '13px', fontWeight: 600 }}>
              <span>0</span><span style={{ color: 'var(--accent-primary)' }}>{beds}</span><span>10</span>
            </div>
          </div>

          <div style={{ padding: '12px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-sm)' }}>
            <div className="text-label" style={{ marginBottom: '4px' }}>Scenario Period</div>
            <div className="text-body" style={{ fontWeight: 500 }}>18:00 → 21:00 (Next 3 Hours)</div>
          </div>

          <button 
            className="button button-primary" 
            onClick={handleSimulate} 
            disabled={simState === 'running'}
            style={{ padding: '12px', width: '100%' }}
          >
            {simState === 'running' ? 'Running Simulation...' : 'Run Simulation'}
          </button>
        </div>

        {/* Results */}
        <div style={{ gridColumn: 'span 7', paddingLeft: '12px' }}>
          {simState === 'idle' ? (
            <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
              Configure scenario and run simulation to view impact.
            </div>
          ) : simState === 'running' ? (
            <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <div className="skeleton" style={{ width: '100%', height: '160px' }}></div>
            </div>
          ) : result && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
                <div style={{ color: 'var(--text-secondary)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase' }}>Metric</div>
                <div style={{ color: 'var(--text-secondary)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase' }}>Baseline</div>
                <div style={{ color: 'var(--accent-primary)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase' }}>Scenario</div>
                
                <div className="text-body">P90 Wait</div>
                <div className="text-body">{mockScenarioBaseline.p90WaitMin} min</div>
                <div className="text-body" style={{ fontWeight: 600 }}>{result.p90WaitMin} min</div>
                
                <div className="text-body">Queue Count</div>
                <div className="text-body">{mockScenarioBaseline.queueCount}</div>
                <div className="text-body" style={{ fontWeight: 600 }}>{result.queueCount}</div>
              </div>

              {result.improvementMin > 0 && (
                <div style={{ background: 'var(--status-healthy-bg)', border: '1px solid #c6f6d5', padding: '16px', borderRadius: 'var(--radius-sm)' }}>
                  <div className="text-label" style={{ color: 'var(--status-healthy)', marginBottom: '8px' }}>Projected Improvement</div>
                  <div style={{ fontSize: '24px', fontWeight: 600, color: 'var(--text-primary)' }}>
                    {result.improvementMin} min reduction <span style={{ fontSize: '16px', fontWeight: 400, color: 'var(--text-secondary)' }}>in P90 Wait</span>
                  </div>
                  <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '13px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><CheckCircle size={14} color="var(--status-healthy)"/> Lower triage congestion</div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><CheckCircle size={14} color="var(--status-healthy)"/> Improved patient flow</div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

// --- PLACEHOLDER PAGES ---
function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <h1 className="page-title">{title}</h1>
      <div className="card" style={{ marginTop: '24px' }}>
        <p className="text-secondary">This section is currently under development.</p>
      </div>
    </div>
  );
}

// --- APP ROUTER ---
export default function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/flow" element={<PlaceholderPage title="Live Flow Analytics" />} />
          <Route path="/scenarios" element={<PlaceholderPage title="Scenario Manager" />} />
          <Route path="/intelligence" element={<PlaceholderPage title="AI Insights Hub" />} />
          <Route path="/forecast" element={<PlaceholderPage title="Forecast Engine" />} />
          <Route path="/history" element={<PlaceholderPage title="Historical Analytics" />} />
          <Route path="/system" element={<PlaceholderPage title="System Status" />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}
