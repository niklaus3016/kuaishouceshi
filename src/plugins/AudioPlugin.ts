import { registerPlugin } from '@capacitor/core';

export interface AudioPlugin {
  play(options: { filePath: string; volume?: number; loop?: boolean }): Promise<void>;
  pause(): Promise<void>;
  stop(): Promise<void>;
  setVolume(options: { volume: number }): Promise<void>;
}

export const AudioPlugin = registerPlugin<AudioPlugin>('AudioPlugin');
