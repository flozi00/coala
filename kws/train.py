# Copyright 2020 The TensorFlow Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import shutil

from absl import app
from absl import logging
from tflite_model_maker import audio_classifier
from tflite_model_maker import ExportFormat
from tflite_model_maker import model_spec

from tflite_support.metadata_writers import audio_classifier as audio_classifier_writer
from tflite_support.metadata_writers import writer_utils

import process


def run(spec,
        data_dir,
        export_dir,
        epochs=10,
        batch_size=8,
        **kwargs):
  """Runs demo."""
  spec = model_spec.get(spec)
  
  data = audio_classifier.DataLoader.from_folder(spec, data_dir, cache=True)
  train_data, rest_data = data.split(0.7)
  #validation_data, test_data = rest_data.split(0.5)


  print('\nTraining the model')
  model = audio_classifier.create(train_data, spec, rest_data, batch_size=batch_size, epochs=epochs,train_whole_model=True, **kwargs)

  #print('\nEvaluating the model')
  #model.evaluate(test_data)

  print('\nConfusion matrix: ')
  print(model.confusion_matrix(rest_data))
  print('labels: ', rest_data.index_to_label)

  print('\nExporing the Saved model and TFLite model to {}'.format(export_dir))
  model.export(
      export_dir, export_format=(ExportFormat.TFLITE, ExportFormat.LABEL))
  

def meta(export_dir):
  AudioClassifierWriter = audio_classifier_writer.MetadataWriter
  _MODEL_PATH = export_dir + "model.tflite"
  # Task Library expects label files that are in the same format as the one below.
  _LABEL_FILE = export_dir + "labels.txt"
  # Expected sampling rate of the input audio buffer.
  _SAMPLE_RATE = 44100
  # Expected number of channels of the input audio buffer. Note, Task library only
  # support single channel so far.
  _CHANNELS = 1
  _SAVE_TO_PATH = export_dir + "model_meta.tflite"

  # Create the metadata writer.
  writer = AudioClassifierWriter.create_for_inference(
      writer_utils.load_file(_MODEL_PATH), _SAMPLE_RATE, _CHANNELS, [_LABEL_FILE])


  # Populate the metadata into the model.
  writer_utils.save_file(writer.populate(), _SAVE_TO_PATH)

  shutil.copy(_SAVE_TO_PATH, "../app/src/main/assets/")
  shutil.copy(_LABEL_FILE, "../app/src/main/assets/")
  


def main(_):
  logging.set_verbosity(logging.INFO)

  export_dir = os.path.expanduser("./speechcommands/")

  data_dir = os.path.expanduser("./dataset/data")

  run("audio_browser_fft", data_dir, export_dir=export_dir)

  meta(export_dir)

if __name__ == '__main__':
  app.run(main)