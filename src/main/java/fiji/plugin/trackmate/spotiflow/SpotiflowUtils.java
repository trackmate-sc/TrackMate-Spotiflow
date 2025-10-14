package fiji.plugin.trackmate.spotiflow;

import static fiji.plugin.trackmate.gui.GuiUtils.getResource;
import static fiji.plugin.trackmate.gui.GuiUtils.scaleImage;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;

public class SpotiflowUtils
{

	public static final ImageIcon spotiflowLogo()
	{
		return new ImageIcon( getResource( "images/spotiflow_logo_squared.png", SpotiflowUtils.class ) );
	}

	public static final ImageIcon spotiflowLogo64()
	{
		return scaleImage( spotiflowLogo(), 64, 64 );
	}

	public static final ImageIcon spotiflowHorizontalLogo()
	{
		return new ImageIcon( getResource( "images/spotiflow_transp_small.png", SpotiflowUtils.class ) );
	}

	public static final ImageIcon spotiflowHorizontalLogo64()
	{
		return scaleImage( spotiflowLogo(), 64, 64 );
	}

	public static List< Spot > readCSV( final File csvFile, final double[] calibration, final Logger logger )
	{
		// FWHM to sigma ratio.
		final double fwhmRatio = 2. * Math.sqrt( 2. * Math.log( 2. ) );
		final ArrayList< Spot > spots = new ArrayList< Spot >();
		try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware( new FileReader( csvFile ) ))
		{
			Map< String, String > values;
			while ( ( values = reader.readMap() ) != null )
			{
				final double x = Double.parseDouble( values.get( "x" ) );
				final double y = Double.parseDouble( values.get( "y" ) );
				final double quality = Double.parseDouble( values.get( "probability" ) );
				final String zStr = values.get( "z" );
				final double z = zStr == null ? 0. : Double.parseDouble( zStr );
				// Gaussian sigma to particle radius.
				final double dimRatio = zStr == null ? Math.sqrt( 2. ) : Math.sqrt( 3. );
				// Radius
				final double r;
				final String fwhmStr = values.get( "fwhm" );
				if ( fwhmStr == null )
				{
					r = 0.5 * calibration[ 0 ];
				}
				else
				{
					final double fwhm = Double.parseDouble( fwhmStr );
					r = fwhm / fwhmRatio * dimRatio * calibration[ 0 ];
				}
				final Spot spot = new Spot( x * calibration[ 0 ], y * calibration[ 1 ], z * calibration[ 2 ], r, quality );
				spots.add( spot );
			}
		}
		catch ( CsvValidationException | IOException e )
		{
			logger.error( "Problem reading CSV file " + csvFile.getAbsolutePath() + "\n" + e.getMessage() + '\n' );
			e.printStackTrace();
		}
		return spots;
	}
}
