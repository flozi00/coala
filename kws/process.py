from pydub import AudioSegment
from pydub.utils import make_chunks
import glob
import soundfile as sf
from audiomentations import Compose, AddGaussianNoise, Gain, PitchShift, AddBackgroundNoise
from tqdm.auto import tqdm
import os

augment = Compose([
    AddGaussianNoise(min_amplitude=0.0001, max_amplitude=0.0003, p=0.3),
    PitchShift(min_semitones=-4, max_semitones=4, p=0.7),
    Gain(min_gain_in_db=-6, max_gain_in_db=6, p=0.5),
    AddBackgroundNoise(sounds_path='dataset/data/other/', min_snr_in_db=3, max_snr_in_db=12, p=0.8)
])

other_list = list(glob.glob('dataset/data/other/*.wav', recursive=True))
hilfe_list = list(glob.glob('dataset/data/hilfe/*.wav', recursive=True))

for f in tqdm(other_list + hilfe_list):
    array, sr = sf.read(f)
    condition = sr == 16000
    if not condition:
        print(f)
        raise AssertionError()

print("Other")
for f in tqdm(other_list):
    if("wavchunk" in f):
        try:
            #os.remove(f)
            pass
        except:
            pass
    else:
        myaudio = AudioSegment.from_file(f , "wav") 
        if(myaudio.duration_seconds > 5):
            print(f)
            chunk_length_ms = 1000 # pydub calculates in millisec
            chunks = make_chunks(myaudio, chunk_length_ms) #Make chunks of one sec

            for i, chunk in enumerate(chunks):
                if(i < 10000):
                    chunk_name = f + "chunk{0}.wav".format(i)
                    chunk.export(chunk_name, format="wav")

other_list = list(glob.glob('dataset/data/other/*.wav', recursive=True))

print("clean hilfe")
for f in tqdm(hilfe_list):
    if(f.endswith('augmented.wav') != False):
        try:
            os.remove(f)
        except:
            pass

hilfe_list = list(glob.glob('dataset/data/hilfe/*.wav', recursive=True))

print("process hilfe")
for f in tqdm(hilfe_list):
    if(f.endswith('augmented.wav') == False):
        for x in range(int((len(other_list) / len(hilfe_list))/2)):
            speech_array, sampling_rate = sf.read(f)
            speech_array = augment(samples=speech_array, sample_rate=sampling_rate)
            sf.write(f"{f}{x}_augmented.wav", speech_array, sampling_rate, subtype='PCM_16')

print(len(other_list))
print(len(list(glob.glob('dataset/data/hilfe/*.wav', recursive=True))))