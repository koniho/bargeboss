#!/usr/bin/env python3
"""Reproducible audition separation, NOT clean semantic source isolation.
Decode input with ffmpeg first. Requires numpy, scipy and soundfile.
"""
import argparse
import json
from pathlib import Path
import numpy as np
import soundfile as sf
from scipy import signal, ndimage

parser = argparse.ArgumentParser()
parser.add_argument('decoded_wav')
parser.add_argument('output_directory')
args = parser.parse_args()
out = Path(args.output_directory)
out.mkdir(parents=True, exist_ok=True)
x, rate = sf.read(args.decoded_wav, always_2d=True)
nfft, hop = 4096, 1024
f, t, spectrum = signal.stft(x.T, fs=rate, nperseg=nfft,
                             noverlap=nfft-hop, boundary='zeros', padded=True)
# One mask for both channels preserves their phase and stereo relationship.
magnitude = np.sqrt(np.mean(np.abs(spectrum)**2, axis=0))
steady = ndimage.median_filter(magnitude, size=(1, 41), mode='reflect')
broad = ndimage.median_filter(magnitude, size=(31, 1), mode='reflect')
harmonic = steady**2 / (steady**2 + (1.7*broad)**2 + 1e-20)
# Keep the deep exhaust pulses even when they are transient. This also retains
# low-frequency water noise: source labels describe emphasis, not purity.
bass = 1 / (1 + (f/260)**4)
mask = bass[:, None]*.94 + (1-bass[:, None])*(.08 + .86*harmonic)
mask = ndimage.gaussian_filter(mask, sigma=(.65, 1.2))
_, engine = signal.istft(spectrum*mask[None, :, :], fs=rate,
                        nperseg=nfft, noverlap=nfft-hop, boundary=True)
engine = engine.T[:len(x)]
residual = x-engine
# Apply the SAME gain to both stems, keeping reconstruction valid.
peak = max(float(np.max(np.abs(engine))), float(np.max(np.abs(residual))),
           float(np.max(np.abs(x))))
gain = min(1., .95/max(peak, 1e-12))
engine *= gain
residual *= gain
sf.write(out/'diesel-focused-v1.flac', engine, rate, subtype='PCM_24')
sf.write(out/'water-noise-residual-v1.flac', residual, rate, subtype='PCM_24')
# Source, engine and residual at matched gain: 10 seconds each, one-second gaps.
start, stop = int(5*rate), int(15*rate)
gap = np.zeros((rate, x.shape[1]))
comparison = np.concatenate([x[start:stop]*gain, gap, engine[start:stop], gap,
                             residual[start:stop]])
sf.write(out/'comparison-original-engine-residual-v1.wav', comparison, rate,
         subtype='PCM_16')
report = {'sample_rate': rate, 'channels': x.shape[1],
          'duration_seconds': len(x)/rate, 'shared_gain': gain,
          'reconstruction_peak_error': float(np.max(np.abs(engine+residual-x*gain))),
          'engine_peak': float(np.max(np.abs(engine))),
          'residual_peak': float(np.max(np.abs(residual))),
          'engine_rms': float(np.sqrt(np.mean(engine**2))),
          'residual_rms': float(np.sqrt(np.mean(residual**2))),
          'method': 'stereo-linked harmonic/steady mask with bass retention',
          'limitations': 'Not semantic isolation; engine and water overlap. Not listening-approved.'}
(out/'separation-report.json').write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps(report, indent=2))
