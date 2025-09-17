package fiji.plugin.trackmate.spotiflow;

import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryGenericConfig;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.detection.SpotGlobalDetectorFactory;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW )
public class SpotiflowDetectorFactory< T extends RealType< T > & NativeType< T > >
		implements SpotGlobalDetectorFactory< T >, SpotDetectorFactoryGenericConfig< T, SpotiflowCLI >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "SPOTIFLOW_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Spotiflow detector";

	public static final String DOC_SPOTIFLOW_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-spotiflow";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on Spotiflow to detect sub-resolved particles."
			+ "<p>"
			+ "The detector calls an external <b>Spotiflow</b> installation. So for this "
			+ "to work, you must have a Spotiflow installation running on your computer. "
			+ "Please follow the instructions on the TrackMate-Spotiflow page, linked below."
			+ "<p>"
			+ "You must also configure properly the conda (or mamba) executable in Fiji. "
			+ "Run <u>Edit >  Options > Configure TrackMate Conda path...</u> to do so."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the Spotiflow paper: <a href=\"https://doi.org/10.1038/s41592-025-02662-x\">Dominguez Mantes, A., Herrera, A., Khven, I. et al. "
			+ "Spotiflow: accurate and efficient spot detection for fluorescence microscopy with deep stereographic flow regression. "
			+ "Nat Methods 22, 1495–1504 (2025)</a>"
			+ "</html>";

	@Override
	public SpotiflowCLI getConfigurator( final ImagePlus imp )
	{
		return new SpotiflowCLI( imp.getNChannels() );
	}

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		// Create the CLI and loads settings into it.
		final SpotiflowCLI cli = getConfigurator( img );
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		// Create the detector.
		return new SpotiflowDetector<>( img, interval, cli );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getUrl()
	{
		return DOC_SPOTIFLOW_URL;
	}

	@Override
	public ImageIcon getIcon()
	{
		return SpotiflowUtils.spotiflowLogo64();
	}
}
