import math
import os
import struct
import wave

path = os.path.join('stt', 'src', 'test', 'resources', 'audio', 'shop_milk_newspaper.wav')
os.makedirs(os.path.dirname(path), exist_ok=True)
rate = 16000
seconds = 2.0
samples = []
for i in range(int(rate * seconds)):
    t = i / rate
    if t < 0.8:
        value = 0.25 * math.sin(2 * math.pi * 440 * t)
    elif t < 1.2:
        value = 0.0
    else:
        value = 0.25 * math.sin(2 * math.pi * 660 * t)
    samples.append(int(max(-32768, min(32767, value * 32767))))

with wave.open(path, 'wb') as wf:
    wf.setnchannels(1)
    wf.setsampwidth(2)
    wf.setframerate(rate)
    wf.writeframes(b''.join(struct.pack('<h', s) for s in samples))
print(path)
