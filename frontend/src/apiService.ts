export interface TwinMetrics {
  patients: number;
  avgWaitMin: number;
  p90WaitMin: number;
  queuePressure: 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
  bottleneck: string;
  bottleneckReason: string;
  bottleneckAction: string;
}

export interface ScenarioResult {
  p90WaitMin: number;
  queueCount: number;
  bedWaitMin: number;
  improvementMin: number;
}

export interface ForecastData {
  time: string;
  arrivals: number;
}

// Mock Data
export const mockMetrics: TwinMetrics = {
  patients: 124,
  avgWaitMin: 32,
  p90WaitMin: 48,
  queuePressure: 'HIGH',
  bottleneck: 'TRIAGE',
  bottleneckReason: 'Arrival volume is currently exceeding available triage capacity.',
  bottleneckAction: 'Consider increasing triage capacity during the evening surge.'
};

export const mockScenarioBaseline: ScenarioResult = {
  p90WaitMin: 48,
  queueCount: 24,
  bedWaitMin: 16,
  improvementMin: 0
};

export const mockScenarioSuccess: ScenarioResult = {
  p90WaitMin: 31,
  queueCount: 15,
  bedWaitMin: 10,
  improvementMin: 17
};

export const mockForecast: ForecastData[] = [
  { time: 'Now', arrivals: 12 },
  { time: '+1h', arrivals: 18 },
  { time: '+2h', arrivals: 24 },
  { time: '+3h', arrivals: 22 },
  { time: '+4h', arrivals: 15 }
];

// Mock API Functions
export const api = {
  getTwinMetrics: async (): Promise<TwinMetrics> => {
    return new Promise(resolve => setTimeout(() => resolve(mockMetrics), 400));
  },
  runScenario: async (nurses: number, beds: number): Promise<ScenarioResult> => {
    return new Promise(resolve => {
      setTimeout(() => {
        // Simple logic for the hackathon: if we add resources, it improves
        if (nurses > 2 || beds > 2) {
          resolve(mockScenarioSuccess);
        } else {
          resolve(mockScenarioBaseline);
        }
      }, 1200);
    });
  },
  getForecast: async (): Promise<ForecastData[]> => {
    return new Promise(resolve => setTimeout(() => resolve(mockForecast), 300));
  }
};
