package tic0012.loselessoundrecord.classes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

/**
 * Uncompressed Mono 16bit recorder
 * Sample rate = 44 100 kHz
 * 
 * @author tic0012
 */
public class UncompressedAudioRecorder {
	
	/**
	 * Sample Rate in Hz
	 * 
	 * the sample rate expressed in Hertz. 44100Hz is currently the only rate
	 * that is guaranteed to work on all devices
	 * 
	 * @see android.media.MediaRecorder.AudioRecord
	 */
	private final int RECORDER_SAMPLE_RATE = 44100;

	/**
	 * the recording source
	 * 
	 * @see android.media.MediaRecorder.AudioSource
	 */
	private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

	/**
	 * Describes the configuration of the audio channels
	 * 
	 * @see android.media.AudioFormat
	 */
	private final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;

	/**
	 * Format in which the audio data is represented
	 * 
	 * @see android.media.AudioFormat
	 */
	private final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	/**
	 * Number of channel
	 * 1 = mono, 2 = stereo
	 */
	private final int CHANNEL_COUNT = 1;

	/**
	 * Number of bits per Sample
	 */
	private final int RECORDER_BIT_PER_SAMPLE = 16;	
	
	/**
	 * File extension
	 */
	private final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
	
	/**
	 * Folder where to save files
	 */
	private String saveFolder;
	
	/**
	 * Temporary filename
	 */
	private final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";

	/**
	 * Total size (in bytes) of the buffer where audio data is written to
	 * during the recording.
	 */
	private int bufferSize = 0;

	/**
	 * The AudioRecord class manages the audio resources
	 */
	private AudioRecord recorder = null;

	/**
	 * Recording state
	 */
	private boolean isRecording = false;
	
	
	public UncompressedAudioRecorder() {	
		//this.saveFolder = saveFolder;
		
		this.prepare();			
	}

	/**
	 * Prepare to recording
	 */
	private void prepare() {
		bufferSize = AudioRecord.getMinBufferSize(this.RECORDER_SAMPLE_RATE,
				this.RECORDER_CHANNELS, this.RECORDER_AUDIO_ENCODING);
		
		recorder = new AudioRecord(this.AUDIO_SOURCE, this.RECORDER_SAMPLE_RATE,
				this.RECORDER_CHANNELS, this.RECORDER_AUDIO_ENCODING, bufferSize);		
	}

	/**
	 * Prepare output WAV file and return its full path
	 * @return full WAV file path
	 */
	private String getFilename(){
		//String filepath = Environment.getExternalStorageDirectory().getPath();		
		//File file = new File(filepath, this.saveFolder);
		File file = new File(this.saveFolder);
		String outputFilePath = file.getAbsolutePath() + "/" + System.currentTimeMillis() + this.AUDIO_RECORDER_FILE_EXT_WAV;
		
		if(!file.exists()){
			file.mkdirs();
		}						
		
		Log.d("new filepath", outputFilePath);
		
		return outputFilePath;
	}
	
	/**
	 * Prepare temporary file and return its fullPath
	 * @return fullPath to temporary file
	 */
	private String getTempFilename(){
		//String filepath = Environment.getExternalStorageDirectory().getPath();
		//File file = new File(filepath, this.saveFolder);
		File file = new File(this.saveFolder);
		
		if(!file.exists()){
			file.mkdirs();
		}		
		
		return (file.getAbsolutePath() + "/" + this.AUDIO_RECORDER_TEMP_FILE);
	}
	
	/**
	 * Start audio recording
	 */
	public void startRecording(String savePath) {
		this.saveFolder = savePath;
		
		if(this.recorder == null){
			prepare();
		}
		
		this.recorder.startRecording();
		this.isRecording = true;
		
		this.writeAudioDataToFile();

	}
	
	/**
	 * Permanently writes audio data from recorder into file
	 */
	private void writeAudioDataToFile() {
		byte data[] = new byte[this.bufferSize];
		String filename = this.getTempFilename();
		
		FileOutputStream os = null;

		try {
			os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		int read = 0;

		if (os != null) {
			while (this.isRecording) {
				read = this.recorder.read(data, 0, this.bufferSize);

				if (AudioRecord.ERROR_INVALID_OPERATION != read) {
					try {
						os.write(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else{
					Log.e("MyError", "Error reading data from recorder");
				}
			}

			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Stop recording and release sources
	 */
	public String stopRecording(){
		String outputFilePath = getFilename();
		
		if(this.recorder != null){
			this.isRecording = false;
			
			this.recorder.stop();
			this.recorder.release();
			
			this.recorder = null;			
		}						
		
		Log.d("temp filename", this.getTempFilename());
		
		this.copyWaveFile( this.getTempFilename(), outputFilePath );
		this.deleteTempFile();
		
		return outputFilePath;
	}
	
	private void deleteTempFile() {
		File file = new File(this.getTempFilename());
				
		file.delete();
	}
	
	/**
	 * Copy RAW data into WAV file
	 * 
	 * @param inFilename
	 * @param outFilename
	 * 
	 * @see https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
	 */
	private void copyWaveFile(String inFilename, String outFilename) {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0; // SubChunk2Size, number of bytes that file currently contains	
		long totalDataLen = 0; // ChunkSize        
		long longSampleRate = this.RECORDER_SAMPLE_RATE;				
		long byteRate = this.RECORDER_BIT_PER_SAMPLE * this.RECORDER_SAMPLE_RATE
				* this.CHANNEL_COUNT / 8;

		byte[] data = new byte[this.bufferSize];

		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36; // 36 + SubChunk2Size			

			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, this.CHANNEL_COUNT, byteRate);

			while (in.read(data) != -1) {
				out.write(data);
			}

			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Write Wave header into file Output stream
	 * 
	 * @param out
	 * @param totalAudioLen
	 * @param totalDataLen
	 * @param longSampleRate
	 * @param channels
	 * @param byteRate
	 * @throws IOException
	 * 
	 * @see https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
	 */
	private void WriteWaveFileHeader(
			FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels,
			long byteRate) throws IOException {
		
		byte[] header = new byte[44];
		
		header[0] = 'R';  // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';				
		header[4] = (byte) (totalDataLen & 0xff); // Final data size
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);		
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f';  // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';		
		header[16] = 16;  // 4 bytes: size of 'fmt ' chunk // Sub-chunk size, 16 for PCM
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1;  // AudioFormat, 1 for PCM
		header[21] = 0;
		header[22] = (byte) channels; // Number of channels, 1 for mono, 2 for stereo
		header[23] = 0;		
		header[24] = (byte) (longSampleRate & 0xff); // Sample rate
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);			
		header[28] = (byte) (byteRate & 0xff); // Byte rate, SampleRate * NumberOfChannels * BitsPerSample / 8
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);			
		header[32] = (byte) (channels * this.RECORDER_BIT_PER_SAMPLE / 8); // Block align, NumberOfChannels * BitsPerSample / 8
		header[33] = 0;
		header[34] = (byte) this.RECORDER_BIT_PER_SAMPLE;  // Bits per sample
		header[35] = 0;		
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';			
		header[40] = (byte) (totalAudioLen & 0xff); // Data chunk size
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

		out.write(header, 0, 44);
	}
}
