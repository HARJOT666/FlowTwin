import React, { useState, useEffect } from 'react';
import { Activity, Clock, Users, Bed, AlertTriangle, CheckCircle, BarChart3, TrendingUp, Info } from 'lucide-react';
import './index.css';

// --- MOCK DATA & TYPES ---
interface TwinState {
  patients: number;
  avgWaitMin: number;
  p90WaitMin: number;
  queuePressure: string;
  bottleneck: string;
}

const mockBaseline: TwinState = {
  patients: 124,
  avgWaitMin: 32,
  p90WaitMin: 48,
  queuePressure: 'High',
  bottleneck: 'Triage',
};

const mockScenarioResult: TwinState = {
  patients: 124,
  avgWaitMin: 22,
  p90WaitMin: 31,
  queuePressure: 'Moderate',
  bottleneck: 'None',
};

export default function App() {
  const [twinState, setTwinState] = useState<TwinState>(mockBaseline);
  const [scenarioActive, setScenarioActive] = useState(false);
  const [isSimulating, setIsSimulating] = useState(false);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

  // Simulate live updates
  useEffect(() => {
    const interval = setInterval(() => {
      setLastUpdate(new Date());
    }, 12000); // update every 12s
    return () => clearInterval(interval);
  }, []);

  const runScenario = () => {
    setIsSimulating(true);
    setTimeout(() => {
      setScenarioActive(true);
      setIsSimulating(false);
    }, 1500); // fake network delay
  };

  const resetScenario = () => {
    setScenarioActive(false);
  };

  return (
    <div className="app-container">
      {/* Header */}
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '1.5rem' }}>
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', margin: 0 }}>
            <Activity color="var(--accent-primary)" />
            FlowTwin
          </h1>
          <p className="card-subtitle" style={{ marginTop: '0.25rem' }}>Hospital Operations Intelligence</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', fontSize: '0.875rem' }}>
            <span className="live-indicator"></span>
            LIVE
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Last updated: {lastUpdate.toLocaleTimeString()}
          </div>
        </div>
      </header>

      {/* Overview KPIs */}
      <section className="dashboard-grid">
        <div className="card" style={{ gridColumn: 'span 3' }}>
          <div className="card-subtitle">Total Patients</div>
          <div style={{ fontSize: '2rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.5rem' }}>
            {twinState.patients}
            <Users size={24} color="var(--text-muted)" />
          </div>
        </div>
        <div className="card" style={{ gridColumn: 'span 3' }}>
          <div className="card-subtitle">Avg Wait Time</div>
          <div style={{ fontSize: '2rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.5rem' }}>
            {scenarioActive ? mockScenarioResult.avgWaitMin : twinState.avgWaitMin} min
            <Clock size={24} color="var(--text-muted)" />
          </div>
        </div>
        <div className="card" style={{ gridColumn: 'span 3', borderLeft: scenarioActive ? '4px solid var(--status-healthy)' : '1px solid var(--border-subtle)' }}>
          <div className="card-subtitle">P90 Wait Time</div>
          <div style={{ fontSize: '2rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.5rem', color: scenarioActive ? 'var(--status-healthy)' : 'var(--status-critical)' }}>
            {scenarioActive ? mockScenarioResult.p90WaitMin : twinState.p90WaitMin} min
            <AlertTriangle size={24} color={scenarioActive ? "var(--status-healthy)" : "var(--status-critical)"} />
          </div>
        </div>
        <div className="card" style={{ gridColumn: 'span 3' }}>
          <div className="card-subtitle">Queue Pressure</div>
          <div style={{ marginTop: '1rem' }}>
             <span className={`badge ${scenarioActive ? 'healthy' : 'critical'}`}>
               {scenarioActive ? mockScenarioResult.queuePressure : twinState.queuePressure}
             </span>
          </div>
        </div>
      </section>

      {/* Main Grid for Visuals and Scenarios */}
      <section className="dashboard-grid">
        
        {/* Left Column: Live Flow & Scenario */}
        <div style={{ gridColumn: 'span 8', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          <div className="card">
            <div className="card-header">
              <h2 className="card-title">Live Patient Flow</h2>
            </div>
            {/* Simple Flow Visualization */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '2rem 1rem', background: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
              <FlowStage name="Arrival" count={12} icon={<Users size={20}/>} active />
              <FlowArrow />
              <FlowStage name="Triage" count={24} icon={<Activity size={20}/>} warning={!scenarioActive} active />
              <FlowArrow />
              <FlowStage name="Waiting" count={45} icon={<Clock size={20}/>} active />
              <FlowArrow />
              <FlowStage name="Treatment" count={32} icon={<CheckCircle size={20}/>} active />
              <FlowArrow />
              <FlowStage name="Bed" count={11} icon={<Bed size={20}/>} warning active />
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h2 className="card-title">What-If Simulation</h2>
              <span className="badge info">Scenario Engine</span>
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
              <div>
                <h4 style={{ marginBottom: '1rem' }}>Configure Scenario</h4>
                <div style={{ marginBottom: '1.5rem' }}>
                  <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.5rem', fontWeight: 500 }}>Triage Nurses (6PM - 9PM)</label>
                  <input type="range" min="1" max="5" defaultValue="3" style={{ width: '100%' }} />
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    <span>1</span><span>+1 (Scenario)</span><span>5</span>
                  </div>
                </div>
                <div style={{ marginBottom: '1.5rem' }}>
                  <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.5rem', fontWeight: 500 }}>Fast-Track Beds</label>
                  <input type="range" min="0" max="10" defaultValue="2" style={{ width: '100%' }} />
                </div>
                
                <div style={{ display: 'flex', gap: '1rem' }}>
                  <button className="button button-primary" onClick={runScenario} disabled={isSimulating || scenarioActive}>
                    {isSimulating ? 'Simulating...' : 'Run Simulation'}
                  </button>
                  {scenarioActive && (
                    <button className="button button-outline" onClick={resetScenario}>
                      Reset
                    </button>
                  )}
                </div>
              </div>
              
              <div style={{ background: 'var(--bg-subtle)', padding: '1.5rem', borderRadius: 'var(--radius-md)' }}>
                <h4 style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between' }}>
                  Impact Comparison
                  {scenarioActive && <span className="badge healthy">Scenario Active</span>}
                </h4>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
                    <span style={{ color: 'var(--text-secondary)' }}>P90 Wait Time</span>
                    <span style={{ fontWeight: 600 }}>
                      {twinState.p90WaitMin}m → <span style={{ color: scenarioActive ? 'var(--status-healthy)' : 'var(--text-muted)' }}>{scenarioActive ? mockScenarioResult.p90WaitMin : '--'}m</span>
                    </span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
                    <span style={{ color: 'var(--text-secondary)' }}>Queue Pressure</span>
                    <span style={{ fontWeight: 600 }}>
                      {twinState.queuePressure} → <span style={{ color: scenarioActive ? 'var(--status-healthy)' : 'var(--text-muted)' }}>{scenarioActive ? mockScenarioResult.queuePressure : '--'}</span>
                    </span>
                  </div>
                  {scenarioActive && (
                    <div style={{ marginTop: '1rem', fontSize: '0.875rem', color: 'var(--status-healthy)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <TrendingUp size={16} />
                      Projected 17 min reduction in wait times
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

        </div>

        {/* Right Column: AI Insights & Forecast */}
        <div style={{ gridColumn: 'span 4', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          <div className="card" style={{ borderTop: '4px solid #8A2BE2' }}>
            <div className="card-header">
              <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Info size={20} color="#8A2BE2" />
                Operations Insight
              </h2>
              <span className="badge" style={{ background: '#f3e8ff', color: '#6b21a8' }}>AI Generated</span>
            </div>
            
            <div style={{ fontSize: '0.875rem', lineHeight: 1.6 }}>
              <p style={{ fontWeight: 600, marginBottom: '0.5rem', color: 'var(--status-critical)' }}>
                Current Bottleneck: {twinState.bottleneck}
              </p>
              <p style={{ marginBottom: '1.5rem', color: 'var(--text-secondary)' }}>
                Triage queue is building rapidly due to an unexpected surge in acuity-level 3 arrivals.
              </p>
              
              <div style={{ background: 'var(--bg-subtle)', padding: '1rem', borderRadius: 'var(--radius-sm)' }}>
                <p style={{ fontWeight: 600, marginBottom: '0.5rem', fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-secondary)' }}>
                  Recommendation
                </p>
                <p style={{ color: 'var(--text-primary)' }}>
                  Adding one triage nurse during the evening surge (18:00 - 21:00) is projected to reduce P90 wait time by ~22%. Next best lever: open 2 fast-track beds.
                </p>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h2 className="card-title">Surge Forecast</h2>
              <BarChart3 size={20} color="var(--text-muted)" />
            </div>
            <div style={{ height: '200px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', border: '1px dashed var(--border-strong)', borderRadius: 'var(--radius-sm)' }}>
              [Forecast Chart Placeholder]
            </div>
          </div>

        </div>
      </section>

      {/* Footer System Status */}
      <footer style={{ marginTop: '2rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border-subtle)', display: 'flex', gap: '2rem', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--status-healthy)' }}></span>
          Backend Connected
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--status-healthy)' }}></span>
          WebSocket Live
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--status-info)' }}></span>
          Demo Simulator Active
        </div>
      </footer>
    </div>
  );
}

// Helper components for the flow chart
function FlowStage({ name, count, icon, active, warning }: { name: string, count: number, icon: React.ReactNode, active?: boolean, warning?: boolean }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem', opacity: active ? 1 : 0.5 }}>
      <div style={{ 
        width: '48px', height: '48px', borderRadius: '50%', 
        background: warning ? 'var(--status-critical-bg)' : 'var(--bg-surface)', 
        color: warning ? 'var(--status-critical)' : 'var(--accent-primary)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        border: warning ? '2px solid var(--status-critical)' : '1px solid var(--border-strong)',
        boxShadow: 'var(--shadow-sm)'
      }}>
        {icon}
      </div>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: '0.75rem', fontWeight: 600 }}>{name}</div>
        <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{count}</div>
      </div>
    </div>
  );
}

function FlowArrow() {
  return (
    <div style={{ flex: 1, height: '2px', background: 'var(--border-strong)', margin: '0 1rem', position: 'relative', top: '-1rem' }}>
      <div style={{ position: 'absolute', right: '-4px', top: '-3px', width: '8px', height: '8px', borderTop: '2px solid var(--border-strong)', borderRight: '2px solid var(--border-strong)', transform: 'rotate(45deg)' }}></div>
    </div>
  );
}
