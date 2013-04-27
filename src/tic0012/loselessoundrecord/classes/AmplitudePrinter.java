package tic0012.loselessoundrecord.classes;

import java.util.ArrayList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

/**
 * Prints sound amplitude into bitmap
 * 
 * @author tic0012, Michal Tichý
 */
public class AmplitudePrinter {

	/**
	 * All RAW amplitudes from WAF file
	 */
	private ArrayList<Short> amplitudes;
	
	/**
	 * List of gunShots indexes(sample number)
	 */
	private ArrayList<Integer> gunShotSamples;
	
	/**
	 * Number of RAW amplitudes, which will be stretch into one
	 */
	private int amplStretch = 60;
	
	/**
	 * Sample rate in Hz
	 */
	private int sampleRate;
	
	/**
	 * Default width of bitmap in px
	 */
	private int imgWidth = 340;
	
	/**
	 * Image canvas height in px
	 */
	private int imgHeight;
	
	/**
	 * GunShot icon
	 */
	private Bitmap shotIcon;
	
	/**
	 * Maximum SHORT value used for amplitude transform from source to image
	 */
	private final short SHORT_MAX = 32767;
	
	/**
	 * Negative max SHORT value used for amplitude transform from source to
	 * image
	 */
	private final short SHORT_MIN = -32768;
	
	/**
	 * Max bitmap length in seconds
	 */
	private int maxBitmapSeconds;		
	
	public AmplitudePrinter(			
			int sampleRate,
			Bitmap icon,
			int imgHeight,
			int maxBitmapSeconds,
			int amplStretch
			){
				
		this.sampleRate = sampleRate;
		this.shotIcon = icon;
		this.imgHeight = imgHeight;		
		this.maxBitmapSeconds = maxBitmapSeconds;		
		this.amplStretch = amplStretch;
	}
	
	/**
	 * Print points into bitmap
	 * 
	 * @param All amplitudes
	 * @return bitmap with points
	 */
	public Bitmap printAmplitudes(ArrayList<Short> amplitudes, ArrayList<Integer> shotsIndex, int screenDensity) {
		this.amplitudes = amplitudes;
		this.gunShotSamples = shotsIndex;
		ArrayList<Float[]> amplToPrint = this.prepareAmplitude(this.amplitudes);					
		float startX, startY, stopX, stopY, halfSec;
		int loopCount;
		int halfSecondAmplCount;
		
		Log.d("samples count", "" + amplitudes.size());
		
		halfSecondAmplCount = ((this.sampleRate / this.amplStretch) / 2);
		int maxAmplCount = this.maxBitmapSeconds * halfSecondAmplCount * 2;
		
		Log.d("hlf second", "" + halfSecondAmplCount);
		
		// set maximum bitmap size
		if(amplToPrint.size() > maxAmplCount){
			this.imgWidth = maxAmplCount;
		}
		else{
			this.imgWidth = amplToPrint.size();
		}		

		Log.d("img width", "" + this.imgWidth);

		Bitmap bmp = Bitmap.createBitmap(this.imgWidth, this.imgHeight,
				Bitmap.Config.RGB_565);
		
		bmp.setDensity(screenDensity);
		Canvas canvas = new Canvas(bmp);

		// colors prepare
		Paint greyPaint = new Paint();
		Paint blackPaint = new Paint();
		Paint bluePaint = new Paint();
		greyPaint.setColor(Color.LTGRAY);
		blackPaint.setColor(Color.BLACK);
		bluePaint.setColor(0xff4A73F1);
		Paint whitePaint = new Paint();
		whitePaint.setColor(Color.WHITE);

		// set canvas BG color
		canvas.drawRect(new Rect(0, 0, this.imgWidth, this.imgHeight), whitePaint);

		// print amplitude into image
		halfSec = startX = startY = loopCount = 0;

		//for (Float[] point : amplToPrint) {
		Float[] point;
		for (int i = 0; i < this.imgWidth; i++) {			
			point = amplToPrint.get(i);
			// draw half second vertical line
			if ((loopCount % halfSecondAmplCount) == 0) {
				canvas.drawLine(loopCount, this.imgHeight, loopCount, 0, greyPaint);
				canvas.drawText("" + halfSec, loopCount + 7, this.imgHeight - 10,
						greyPaint);
				halfSec += 0.5;
				Log.d("seconds ", halfSec + "");
			}

			stopX = loopCount;
			stopY = this.imgHeight / 2;
			
			// Draw positive part
			if(point[0] != 0){				
				stopY = (this.imgHeight - point[0]) - (this.imgHeight / 2);

				if (loopCount == 0) {
					startY = stopY;
				}

				// draw positive amplitude part
				canvas.drawLine(startX, startY, stopX, stopY, bluePaint);
			}
						
			// Draw negative part
			if(point[1] != 0){
				stopY = (this.imgHeight - point[1]) - (this.imgHeight / 2);

				if (loopCount == 0) {
					startY = stopY;
				}

				// draw negative amplitude part
				canvas.drawLine(startX, startY, stopX, stopY, bluePaint);
			}			

			// draw shot icon
			for (int shot : this.gunShotSamples) {
				if ((shot / this.amplStretch) == loopCount) {

					canvas.drawBitmap(this.shotIcon,
							(loopCount - ( 2 * this.shotIcon.getWidth())), 0, null);
				}
			}

			startX = loopCount;
			startY = stopY;
			loopCount++;
		}
		
		for (int i = 0; i < this.imgWidth; i++) {
			
		}

		// draw zero horizontal line
		canvas.drawLine(0, (this.imgHeight / 2), this.imgWidth, (this.imgHeight / 2),
				greyPaint);

		return bmp;
	}
	
	/**
	 * Prepare amplitudes to print
	 * 
	 * @param amplitudes
	 * @return
	 */
	private ArrayList<Float[]> prepareAmplitude(ArrayList<Short> amplitudes){
		ArrayList<Float> tempPoints = new ArrayList<Float>();
		ArrayList<Float[]> result = new ArrayList<Float[]>();
		short loopCount = 0;

		// get average amplitudes
		for (float point : this.convertAmplSize(amplitudes)) {
			tempPoints.add(point);

			if (loopCount == this.amplStretch) {
				Float average[] = this.countAverage(tempPoints);

				result.add(average);

				tempPoints.clear();

				loopCount = -1;
			}

			loopCount++;
		}
		
		return result;
	}
	
	/**
	 * Transform amplitude size to image size
	 * 
	 * @param points
	 * @return
	 */
	private ArrayList<Float> convertAmplSize(ArrayList<Short> points) {
		ArrayList<Float> result = new ArrayList<Float>();
		float result1, result2;

		for (short ampl : points) {
			if (ampl > 0) {
				result1 = ampl / (this.SHORT_MAX / 100);
				result2 = (float) Math.ceil(((imgHeight / 2) / 100) * result1);
			} else if (ampl < 0) {
				result1 = ampl / (this.SHORT_MIN / 100);
				result2 = (float) Math.floor(((imgHeight / 2) / 100) * result1);
				result2 -= 2 * result2;
			} else {
				result2 = 0;
			}

			result.add(result2);
		}

		return result;
	}
	
	/**
	 * Count average of number list
	 * 
	 * @param points
	 * @return list of arrays, 0 => positive average, 1 => negative average
	 */
	private Float[] countAverage(ArrayList<Float> points) {
		float positiveAverage = 0, negativeAverage = 0;
		int positiveCount = 0, negativeCount = 0;				
		Float[] average = new Float[2];

		// setup points
		for (float point : points) {
			if(point > 0){ // positive amplitude
				positiveAverage += point;
				positiveCount++;
			}
			else{ // negative amplitude
				negativeAverage += point;
				negativeCount++;
			}			
		}
		
		if(positiveCount == 0){
			average[0] = Float.valueOf(0);
		}
		else{
			average[0] = positiveAverage / positiveCount;
		}
		
		if(negativeAverage == 0){
			average[1] = Float.valueOf(0);
		}
		else{
			average[1] = negativeAverage / negativeCount;	
		}				

		return average;
	}
}
