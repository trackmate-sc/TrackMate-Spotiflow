package fiji.plugin.trackmate.spotiflow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.apache.commons.io.input.Tailer;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.cli.CLIUtils;
import fiji.plugin.trackmate.util.cli.CLIUtils.LoggerTailerListener;
import fiji.plugin.trackmate.util.cli.CommandBuilder;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class SpotiflowDetector< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetector< T >, Cancelable, MultiThreaded
{

	private static final Function< Long, String > nameGen = ( frame ) -> String.format( "%d", frame );

	private final ImgPlus< T > img;

	private final Interval interval;

	private Logger logger = Logger.VOID_LOGGER;

	private final String baseErrorMessage;

	private String errorMessage;

	private long processingTime;

	private SpotCollection spots;

	private String cancelReason;

	private boolean isCanceled;

	private final List< SpotiflowTask > processes = new ArrayList<>();

	private int numThreads;

	private final File spotiflowLogFile;

	private final SpotiflowCLI cli;

	public SpotiflowDetector(
			final ImgPlus< T > img,
			final Interval interval,
			final SpotiflowCLI cli )
	{
		this.img = img;
		this.interval = interval;
		this.cli = cli;
		final String command = cli.getCommand();
		this.spotiflowLogFile = new File( new File( System.getProperty( "user.home" ), "." + command ), "run.log" );
		this.baseErrorMessage = "[" + command + "Detector] ";
	}

	@Override
	public boolean process()
	{
		final String command = cli.getCommand();
		final long start = System.currentTimeMillis();
		isCanceled = false;
		cancelReason = null;

		/*
		 * Dispatch time-points to several tasks.
		 */

		final int c = cli.targetChannel().getValue() - 1; // 0-based
		final List< ImagePlus > imps = DetectionUtils.splitSingleTimePoints( img, interval, c, nameGen );

		final int nConcurrentTasks = numThreads;
		final List< List< ImagePlus > > timepoints = new ArrayList<>( nConcurrentTasks );
		for ( int i = 0; i < nConcurrentTasks; i++ )
			timepoints.add( new ArrayList<>() );

		Iterator< List< ImagePlus > > it = timepoints.iterator();
		for ( int t = 0; t < imps.size(); t++ )
		{
			if ( !it.hasNext() )
				it = timepoints.iterator();
			it.next().add( imps.get( t ) );
		}

		/*
		 * Create tasks for each list of imps.
		 */

		final SpotCollection tmpSpots = new SpotCollection();
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		processes.clear();
		for ( final List< ImagePlus > list : timepoints )
			processes.add( new SpotiflowTask( list, tmpSpots, calibration ) );

		/*
		 * Pass tasks to executors.
		 */

		// Redirect log to logger.
		final Tailer tailer = Tailer.builder()
				.setFile( spotiflowLogFile )
				.setTailerListener( new LoggerTailerListener( logger ) )
				.setDelayDuration( Duration.ofMillis( 200 ) )
				.setTailFromEnd( true )
				.get();

		final ExecutorService executors = Executors.newFixedThreadPool( nConcurrentTasks );
		final List< String > resultDirs = new ArrayList<>( nConcurrentTasks );
		List< Future< String > > results;
		try
		{
			results = executors.invokeAll( processes );
			for ( final Future< String > future : results )
				resultDirs.add( future.get() );
		}
		catch ( final InterruptedException | ExecutionException e )
		{
			errorMessage = baseErrorMessage + "Problem running "
					+ command
					+ ":\n" + e.getMessage() + '\n';
			e.printStackTrace();
			return false;
		}
		finally
		{
			tailer.close();
			logger.setStatus( "" );
			logger.setProgress( 1. );
		}

		/*
		 * Did we have a problem with independent tasks?
		 */

		for ( final SpotiflowTask task : processes )
		{
			if ( !task.isOk() )
				return false;
		}

		/*
		 * Reposition spots with respect to the interval and time.
		 */

		final int timeIndex = img.dimensionIndex( Axes.TIME );
		final double frameInterval = ( timeIndex < 0 ) ? 1. : img.averageScale( timeIndex );

		final List< Spot > slist = new ArrayList<>();
		for ( final Spot spot : tmpSpots.iterable( false ) )
		{
			for ( int d = 0; d < interval.numDimensions() - 1; d++ )
			{
				final double pos = spot.getDoublePosition( d ) + interval.min( d ) * calibration[ d ];
				spot.putFeature( Spot.POSITION_FEATURES[ d ], Double.valueOf( pos ) );
			}
			// Set the time properly.
			final int frame = spot.getFeature( Spot.FRAME ).intValue();
			spot.putFeature( Spot.POSITION_T, frame * frameInterval );
			spot.putFeature( Spot.FRAME, Double.valueOf( frame ) );
			slist.add( spot );
		}
		spots = SpotCollection.fromCollection( slist );

		/*
		 * End.
		 */

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
	}

	@Override
	public SpotCollection getResult()
	{
		return spots;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if ( img.dimensionIndex( Axes.Z ) >= 0 )
		{
			errorMessage = baseErrorMessage + "Image must be 2D over time, got an image with multiple Z.";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	// --- org.scijava.Cancelable methods ---

	@Override
	public boolean isCanceled()
	{
		return isCanceled;
	}

	@Override
	public void cancel( final String reason )
	{
		isCanceled = true;
		cancelReason = reason;
		for ( final SpotiflowTask task : processes )
			task.cancel();
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}

	// --- Multithreaded methods ---

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors() / 2;
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	// --- private classes ---

	final class SpotiflowTask implements Callable< String >
	{

		private Process process;

		private final AtomicBoolean ok;

		private final List< ImagePlus > imps;

		private final SpotCollection tmpSpots;

		private final double[] calibration;

		public SpotiflowTask(
				final List< ImagePlus > imps,
				final SpotCollection tmpSpots,
				final double[] calibration )
		{
			this.imps = imps;
			this.tmpSpots = tmpSpots;
			this.calibration = calibration;
			this.ok = new AtomicBoolean( true );
		}

		public boolean isOk()
		{
			return ok.get();
		}

		void cancel()
		{
			if ( process != null )
				process.destroy();
		}

		@Override
		public String call() throws Exception
		{
			final String command = cli.getCommand();

			/*
			 * Prepare tmp dir.
			 */
			Path tmpDir = null;
			try
			{
				tmpDir = Files.createTempDirectory( "TrackMate-" + command + "_" );
				CLIUtils.recursiveDeleteOnShutdownHook( tmpDir );
			}
			catch ( final IOException e1 )
			{
				errorMessage = baseErrorMessage + "Could not create tmp dir to save and load images:\n" + e1.getMessage();
				ok.set( false );
				return null;
			}

			/*
			 * Save time-points as individual frames.
			 */

			logger.log( "Saving single time-points.\n" );
			// Careful, now time starts at 0, even if in the interval it is not
			// the case.
			for ( final ImagePlus imp : imps )
			{
				final String name = imp.getShortTitle() + ".tif";
				IJ.saveAsTiff( imp, Paths.get( tmpDir.toString(), name ).toString() );
			}

			/*
			 * Run Spotiflow.
			 */

			try
			{
				final List< String > cmd;
				synchronized ( cli )
				{
					/*
					 * In synchronized block so that we can safely generate the
					 * command line even if one instance of the cli is used in
					 * several threads.
					 */
					cli.imageFolder().set( tmpDir.toString() );
					cli.outputFolder().set( tmpDir.toString() );
					cmd = CommandBuilder.build( cli );
				}
				logger.setStatus( "Running " + command );
				logger.log( "Running " + command + " with args:\n" );
				logger.log( String.join( " ", cmd ) );
				logger.log( "\n" );

				final ProcessBuilder pb = new ProcessBuilder( cmd );
				pb.redirectOutput( ProcessBuilder.Redirect.INHERIT );
				pb.redirectError( ProcessBuilder.Redirect.INHERIT );

				process = pb.start();
				process.waitFor();
			}
			catch ( final Exception e )
			{
				errorMessage = baseErrorMessage + "Problem running " + command + ":\n" + e.getMessage();
				e.printStackTrace();
				ok.set( false );
				return null;
			}
			finally
			{
				process = null;
			}

			// List all CSV files in the result dir.
			final File dir = new File( tmpDir.toString() );
			final File[] csvFiles = dir.listFiles( ( d, name ) -> name.toLowerCase().endsWith( ".csv" ) );
			if ( null == csvFiles || 0 == csvFiles.length )
			{
				logger.error( baseErrorMessage + "No CSV results found in " + tmpDir + '\n' );
				return tmpDir.toString();
			}

			for ( final File csvFile : csvFiles )
			{
				// Read time from file name in the shape of img-t6.csv
				final String fname = csvFile.getName();
				final String[] tokens = fname.split( "-" );
				final String timeStr = tokens[ 1 ].replaceAll( "\\D+", "" );
				final int t = Integer.parseInt( timeStr );

				// Read spots.
				final List< Spot > spotsInFrame = SpotiflowUtils.readCSV( csvFile, calibration, logger );
				tmpSpots.put( t, spotsInFrame );
			}

			return tmpDir.toString();
		}
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}
}
