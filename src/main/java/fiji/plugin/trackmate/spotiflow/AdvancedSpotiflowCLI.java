/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2025 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.spotiflow;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;

import java.util.Collections;

import fiji.plugin.trackmate.util.cli.HasInteractivePreview;

public class AdvancedSpotiflowCLI extends SpotiflowCLI implements HasInteractivePreview
{

	public static final String DEFAULT_SPOTIFLOW_CUSTOM_MODEL_FOLDER_PATH = "";

	public static final String KEY_SPOTIFLOW_CUSTOM_MODEL_FOLDER_PATH = "SPOTIFLOW_MODEL_FILEPATH";

	public static final String KEY_SPOTIFLOW_PRETRAINED_OR_CUSTOM = "PRETRAINED_OR_CUSTOM";

	public static final String KEY_PROBABILITY_THRESHOLD = "PROBABILITY_THRESHOLD";

	public static final String KEY_MIN_DISTANCE = "MIN_DISTANCE";

	private static final String KEY_ESTIMATE_FIT_PARAMETERS = "ESTIMATE_FIT_PARAMETERS";

	private final PathArgument customModelFolder;

	private final SelectableArguments selectPretrainedOrCustom;

	private final DoubleArgument probaThreshold;

	private final DoubleArgument minDistance;

	private final Flag fitGaussian;

	private final Flag doSubpixelLocalization;

	public AdvancedSpotiflowCLI( final int nChannels, final String units, final double pixelSize )
	{
		super( nChannels );

		// Custom model.
		this.customModelFolder = addPathArgument()
				.name( "Path to a custom model folder" )
				.argument( "--model-dir" )
				.help( "Path to a custom model folder." )
				.defaultValue( DEFAULT_SPOTIFLOW_CUSTOM_MODEL_FOLDER_PATH )
				.key( KEY_SPOTIFLOW_CUSTOM_MODEL_FOLDER_PATH )
				.get();

		// State that we can use pretrained or custom.
		this.selectPretrainedOrCustom = addSelectableArguments()
				.add( modelPretrained )
				.add( customModelFolder )
				.key( KEY_SPOTIFLOW_PRETRAINED_OR_CUSTOM );

		// Probability threhsold.
		this.probaThreshold = addDoubleArgument()
				.name( "Probability threshold" )
				.help( "The probability threshold for peak detection. Increase this value to be more stringent." )
				.argument( "--probability-threshold" )
				.min( 0. )
				.max( 1. )
				.defaultValue( 0.5 )
				.key( KEY_PROBABILITY_THRESHOLD )
				.get();

		// Min distance.
		this.minDistance = addDoubleArgument()
				.name( "Min. distance" )
				.help( "Minimum distance between spots for non-maxima suppression."
						+ "This defines the minimum distance between detected peaks for them to be considered as different spots." )
				.argument( "--min-distance" )
				.defaultValue( 2. * pixelSize )
				.units( units )
				.min( pixelSize )
				.key( KEY_MIN_DISTANCE )
				.get();
		// Translate to pixel size.
		setCommandTranslator( minDistance, d -> {
			final double dist = ( double ) d;
			final int distPix = ( int ) ( dist > 0 ? ( dist / pixelSize ) : 0. );
			// Spotiflow only accepts integer values for this parameter.
			return Collections.singletonList( "" + distPix );
		} );

		// Fit parameters calculation (replace existing hidden parameter).
		arguments.remove( estimateFitParametersNotShown );
		this.fitGaussian = addFlag()
				.name( "Estimate radius" )
				.help( "Estimate radius of detected spots by Gaussian fitting." )
				.argument( "--estimate-params" )
				.key( KEY_ESTIMATE_FIT_PARAMETERS )
				.defaultValue( true )
				.get();
		// Translate to 'true' or 'false' for Spotiflow CLI.
		setCommandTranslator( fitGaussian, b -> ( ( boolean ) b )
				? Collections.singletonList( "true" )
				: Collections.singletonList( "false" ) );

		// Skip subpixel accuracy.
		this.doSubpixelLocalization = addFlag()
				.name( "Sub-pixel localization" )
				.help( "Whether to use the stereographic flow to compute subpixel localization." )
				.argument( "--subpix" )
				.key( KEY_DO_SUBPIXEL_LOCALIZATION )
				.defaultValue( true )
				.get();
		// Translate to 'true' or 'false' for Spotiflow CLI.
		setCommandTranslator( doSubpixelLocalization, b -> ( ( boolean ) b )
				? Collections.singletonList( "true" )
				: Collections.singletonList( "false" ) );

		// Rearrange arguments order.
		arguments.remove( modelPretrained );
		arguments.add( 1, modelPretrained );
		arguments.remove( targetChannel );
		arguments.add( 5, targetChannel );
	}

	@Override
	public String getPreviewArgumentKey()
	{
		return KEY_PROBABILITY_THRESHOLD;
	}

	@Override
	public String getPreviewAxisLabel()
	{
		return "Probability";
	}

	public PathArgument customModelFolder()
	{
		return customModelFolder;
	}

	public SelectableArguments selectPretrainedOrCustom()
	{
		return selectPretrainedOrCustom;
	}

	public DoubleArgument pobabilityThreshold()
	{
		return probaThreshold;
	}

	public DoubleArgument minDistance()
	{
		return minDistance;
	}
}
