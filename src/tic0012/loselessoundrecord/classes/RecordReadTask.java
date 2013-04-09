package tic0012.loselessoundrecord.classes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import tic0012.loselessoundrecord.R;
import tic0012.loselessoundrecord.ReadActivity;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

/**
 * Asynchronous WAV file reading Gets sound amplitude and transform it into
 * bitmap image. Sets image into ImageView
 * 
 * @author tic0012, Michal Tichý
 */
public class RecordReadTask extends AsyncTask<Void, Integer, ArrayList<Short>> {

	/**
	 * Parent Activity
	 */
	private ReadActivity readActivity;

	/**
	 * Absolute path to WAV file
	 */
	private String filePath;

	/**
	 * Number of bytes to skip WAV header = 44 bytes
	 */
	private final long SKIP_BYTES = 44;

	/**
	 * Progress bar to show reading progress
	 */
	private ProgressBar progressBar;

	/**
	 * WAV source file for reading
	 */
	private File srcFile = null;
	private FileInputStream inputStream = null;
	private BufferedInputStream buffInStream = null;

	/**
	 * Contains all RAW amplitudes from WAF file
	 */
	private ArrayList<Short> amplitudeList = null;

	/**
	 * Lists of times when was fired
	 */
	private ArrayList<Float> gunshots = null;

	/**
	 * List of gunShots indexes(sample number)
	 */
	private ArrayList<Integer> sampleShots = null;

	/**
	 * Sample rate in Hz
	 */
	private int sampleRate;

	/**
	 * Amplitudes count value, which is GunShot
	 */
	private int gunshotSensitivity;		
	
	/**
	 * Time space between gunShots in seconds
	 */
	private final float GUNSHOT_SPACE = 0.15f;
	
	/**
	 * Minimum space between gunshot in samples
	 */
	private int gunshotSpaceSamples;
	
	/**
	 * Time samples, where search for gunSot
	 */
	//private final float SEARCHING_PART = 0.05f;
	private final float SEARCHING_PART = 0.04f;
	
	/**
	 * Number of samples, where to search for gunShot
	 */
	private int searchingPartSamples;
	
	/**
	 * Samples count representing gunshot for each sensitivity
	 */
	//private final int[] SHOT_VALUES = {0, 1, 2, 3, 4, 8800000, 6, 7, 8, 9, 10};
	private final int[] SHOT_VALUES = {
			10000000, 
			9400000, 
			8800000, 
			8200000, 
			7600000, 
			7100000, // default value
			6500000, 
			6000000, 
			5500000, 
			5000000, 
			4000000
			};	

	/**
	 * Set true for debugging output
	 */
	private final boolean DEBUG = false;

	/**
	 * If true, some bytes will be skipped in file reading
	 */
	private boolean compression;

	/**
	 * Number which sample read, for example = 4, read each fourth sample
	 */
	private int compressionRatio;

	/**
	 * Maximum number of amplitudes need to be stored
	 */
	private int maxAmplitudes;

	public RecordReadTask(int sampleRate, ProgressBar progrBar, String fileP,
			ReadActivity activity, boolean compression, int compressionRatio, int sensitivity) {
		this.filePath = fileP;
		this.progressBar = progrBar;
		this.readActivity = activity;
		this.sampleRate = sampleRate;
		this.compression = compression;
		this.compressionRatio = compressionRatio;
		this.gunshotSensitivity = this.SHOT_VALUES[sensitivity];
		
		if(this.compression){
			// set searching part in samples
			this.searchingPartSamples = (int)((this.sampleRate / this.compressionRatio ) * this.SEARCHING_PART);
			// set minimal space between gunShots
			this.gunshotSpaceSamples = (int)((this.sampleRate / this.compressionRatio ) * this.GUNSHOT_SPACE);
		}		
		else{
			// set searching part in samples
			this.searchingPartSamples = (int)(this.sampleRate * this.SEARCHING_PART);
			// set minimal space between gunShots			
			this.gunshotSpaceSamples = (int)(this.sampleRate * this.GUNSHOT_SPACE);
		}

		int maxSeconds = this.readActivity.getResources().getInteger(
				R.integer.max_bitmap_seconds);
		if (this.compression) {
			this.maxAmplitudes = (this.sampleRate / this.compressionRatio)
					* maxSeconds;
		} else {
			this.maxAmplitudes = this.sampleRate * maxSeconds;
		}
		
	}

	@Override
	/**
	 * Asynchronous reading
	 */
	protected ArrayList<Short> doInBackground(Void... arg0) {
		// ArrayList<Short> points = new ArrayList<Short>(); // whole amplitudes

		OutputStreamWriter outStreamWriter = null;
		FileOutputStream fileOutpuStream = null;
		int readedBytes = 0; // bytes reading counter
		int readedSamples = 0;
		int readedForSearch = 0;
		short[] amplBuffer = new short[this.searchingPartSamples];
		float shotTime;
		
		long start = System.currentTimeMillis();

		try {
			// debugging amplitude output file			
			if (this.DEBUG) {
				File ouputFile = new File(Environment
						.getExternalStorageDirectory().getAbsolutePath()
						+ "/GunshotRecorder/sinus.txt");
				ouputFile.createNewFile();
				fileOutpuStream = new FileOutputStream(ouputFile);
				outStreamWriter = new OutputStreamWriter(fileOutpuStream);
			}
			

			byte[] myBuff = new byte[2];
			short ampl;

			// start reading
			Log.d("sound proccessing", "Getting amplitude");
			buffInStream.skip(SKIP_BYTES); // skip WAV header

			int readedFordCompression = 0;
			int toSkip = this.compressionRatio - 1;

			// read WAF file by 2 bytes
			while (buffInStream.read(myBuff, 0, 2) > 1) {
				if (isCancelled()) { // check if user canceled work
					break;
				}

				// skip byte for compression
				if (readedFordCompression < toSkip && this.compression) {
					readedFordCompression++;
					readedBytes += 2;
					continue;
				}

				readedBytes += 2;
				readedFordCompression = 0;

				// get number representation of amplitude
				ampl = ((short) ((myBuff[0] & 0xff) | (myBuff[1] << 8)));
				// points.add(ampl);

				if (readedSamples < this.maxAmplitudes) {
					this.amplitudeList.add(ampl);
				}

				// search for gunShot
				if (readedForSearch == this.searchingPartSamples) {
					boolean enoughSpace = true;
					int amplitudeIndex = readedSamples - this.searchingPartSamples;
					int samplesSize = this.sampleShots.size();
					// check space between gunShots	
					if(samplesSize > 0){
						if((this.sampleShots.get(samplesSize - 1) + this.gunshotSpaceSamples) > amplitudeIndex){
							enoughSpace = false;
						}
					}
					
					if(enoughSpace){ // enough time space between gunShots, potencialy new gunShot
						if (this.isShot(amplBuffer)) {
							//float shotTime = (float) readedSamples / (float) this.sampleRate;																						
							
							if(this.compression){									
								shotTime = (float)amplitudeIndex / ((float)this.sampleRate / (float)this.compressionRatio);							
							}		
							else{
								shotTime = (float)amplitudeIndex / (float)this.sampleRate;
							}												
							this.gunshots.add(shotTime);
							this.sampleShots.add(amplitudeIndex);
							Log.i("Found shot", "sample time " + shotTime
									+ "s, index: " + amplitudeIndex);
						}						
					}
					
					// clear buffer
					for(int i = 0; i < amplBuffer.length; i++){
						amplBuffer[i] = 0;
					}
					readedForSearch = 0;
				} else {					
					// get only positive amplitudes
					if(ampl > 0){
						amplBuffer[readedForSearch] = ampl;
						readedForSearch++;
					}					
				}

				// write amplitude into debug file
				if (this.DEBUG) {
					outStreamWriter.append(Short.toString(ampl) + "\n");
				}

				publishProgress(readedBytes);
				readedSamples++;
			}

			Log.d("sound proccessing", "Whole amplitude getted");

		} catch (Exception e) {
			System.err.println(e);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (buffInStream != null) {
					buffInStream.close();
				}
				if (outStreamWriter != null) {
					outStreamWriter.close();
				}
				if (fileOutpuStream != null) {
					fileOutpuStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		long end = System.currentTimeMillis();
		Log.i("execution time", ((end-start) / 1000) + " s");
		
		// return points;
		return this.amplitudeList;
	}

	@Override
	/**
	 * Prepare for reading
	 */
	protected void onPreExecute() {
		super.onPreExecute();

		long filesize;

		srcFile = new File(filePath);
		try {
			// open file
			inputStream = new FileInputStream(srcFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();

			this.cancel(true);
		}

		// prepare file to read
		buffInStream = new BufferedInputStream(inputStream);
		filesize = srcFile.length();

		filesize -= SKIP_BYTES;

		// set maximum value to progress Bar
		progressBar.setMax((int) filesize);
		progressBar.setVisibility(View.VISIBLE);

		this.amplitudeList = new ArrayList<Short>();
		this.gunshots = new ArrayList<Float>();
		this.sampleShots = new ArrayList<Integer>();
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		super.onProgressUpdate(progress);

		progressBar.setProgress(progress[0]);
	}

	@Override
	/**
	 * Reading done, print image with amplitude
	 */
	protected void onPostExecute(ArrayList<Short> points) {
		super.onPostExecute(points);

		// handlePoints(points);
		this.readActivity.setResults(this.amplitudeList, this.gunshots,
				this.sampleShots);
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<Short> getResult() {
		if (this.getStatus() == AsyncTask.Status.FINISHED) {
			return amplitudeList;
		} else {
			return null;
		}
	}	

	/**
	 * Check values, if they are a gunShot
	 * 
	 * @param samples
	 * @return
	 */
	private boolean isShot(short[] samples) {
		int counter = 0;

		for (int i = 0; i < samples.length; i++) {
			counter += samples[i];
		}

		if (counter > this.gunshotSensitivity) {
			Log.i("sensitivity", "" + this.gunshotSensitivity);
			Log.i("value", "" + counter);
			return true;
		}

		return false;
	}
}