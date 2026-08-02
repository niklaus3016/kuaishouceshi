import { registerPlugin } from '@capacitor/core';

export interface RiskCheckPlugin {
  startRiskCheck(): Promise<{ success: boolean }>;
  getRiskStatus(): Promise<{ hasRisk: boolean; riskDescription: string }>;
}

export const RiskCheckPlugin = registerPlugin<RiskCheckPlugin>('RiskCheck');
