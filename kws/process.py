from pydub import AudioSegment
from pydub.utils import make_chunks
import glob
import soundfile as sf
from audiomentations import Compose, AddGaussianNoise, Gain, PitchShift
from tqdm.auto import tqdm
import os

augment = Compose([
    AddGaussianNoise(min_amplitude=0.0001, max_amplitude=0.001, p=0.7),
    PitchShift(min_semitones=-0.3, max_semitones=0.5, p=0.6),
    Gain(min_gain_in_db=-17, max_gain_in_db=4, p=0.7),
])

other_list = list(glob.glob('dataset/data/other/*.wav', recursive=True))
hilfe_list = list(glob.glob('dataset/data/hilfe/*.wav', recursive=True))

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
            chunk_length_ms = 1000 # pydub calculates in millisec
            chunks = make_chunks(myaudio, chunk_length_ms) #Make chunks of one sec

            for i, chunk in enumerate(chunks):
                if(i < 10000):
                    chunk_name = f + "chunk{0}.wav".format(i)
                    print(chunk_name)
                    chunk.export(chunk_name, format="wav")

other_list = list(glob.glob('dataset/data/other/*.wav', recursive=True))
print(len(other_list))

hilfe_raw = 0
for f in tqdm(hilfe_list):
    if(f.endswith('augmented.wav') == False):
        hilfe_raw += 1

for f in tqdm(hilfe_list):
    if(f.endswith('augmented.wav') == False):
        for x in range(int((len(other_list)/hilfe_raw)/2)):
            speech_array, sampling_rate = sf.read(f)
            speech_array = augment(samples=speech_array, sample_rate=sampling_rate)
            sf.write(f"{f}{x}_augmented.wav", speech_array, sampling_rate, subtype='PCM_16')

print(len(list(glob.glob('dataset/data/hilfe/*.wav', recursive=True))))