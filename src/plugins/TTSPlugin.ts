import { registerPlugin, PluginResultError } from '@capacitor/core';

export interface TTSPlugin {
  speak(options: { text: string }): Promise<{ success: boolean }>;
  stop(): Promise<void>;
  cancel(): Promise<void>;
}

export const TTSPlugin = registerPlugin<TTSPlugin>('TTSPlugin');