package fiji.plugin.trackmate.spotiflow;

import fiji.plugin.trackmate.TrackMatePlugIn;
import ij.ImageJ;

public class SpotiflowTestDrive
{

	public static void main( final String[] args )
	{
		final String filename = "samples/telomeres_timelapse.tif";
		ImageJ.main( args );
		new TrackMatePlugIn().run( filename );
	}

}
