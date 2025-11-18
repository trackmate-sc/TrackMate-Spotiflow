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

import fiji.plugin.trackmate.util.cli.CommonTrackMateArguments;
import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator;

public class SpotiflowCLI extends CondaCLIConfigurator
{

	private static final String KEY_SPOTIFLOW_PRETRAINED_MODEL = "SPOTIFLOW_PRETRAINED_MODEL";

	protected final PathArgument imageFolder;

	protected final ChoiceArgument modelPretrained;

	/** Target channel in the input image. 1-based index. */
	protected final IntArgument targetChannel;

	protected final PathArgument outputFolder;

	/** Store this field so that subclasses can remove it. */
	protected final StringArgument estimateFitParametersNotShown;

	public SpotiflowCLI( final int nChannels )
	{
		// Folders to store input images and output results.
		this.imageFolder = addPathArgument()
				.name( "Input image folder path" )
				.help( "Path to image file or directory of image files. If a directory, will process all images in the directory." )
				.argument( "" ) // Empty but must be first
				.visible( false )
				.required( true )
				.get();
		this.outputFolder = addPathArgument()
				.name( "Output folder path" )
				.help( "Path to output folder." )
				.argument( "--out-dir" )
				.visible( false )
				.required( true )
				.get();

		// Force calculation of shape parameters
		this.estimateFitParametersNotShown = addStringArgument()
				.name( "Estimate fit parameters" )
				.help( "Estimate fit parameters of detected spots by Gaussian fitting (eg FWHM, intensity)." )
				.argument( "--estimate-params" )
				.visible( false )
				.defaultValue( "true" )
				.required( true )
				.get();
		estimateFitParametersNotShown.set( "true" );

		// Pretrained model.
		this.modelPretrained = addChoiceArgument()
				.name( "Pretrained model" )
				.help( "Pretrained model name" )
				.key( KEY_SPOTIFLOW_PRETRAINED_MODEL )
				.argument( "--pretrained-model" )
				.addChoice( "fluo_live" )
				.addChoice( "general" )
				.addChoice( "hybiss" )
				.addChoice( "synth_complex" )
				.addChoice( "synth_3d" )
				.addChoice( "smfish_3d" )
				.defaultValue( "fluo_live" )
				.get();

		// Target channel.
		this.targetChannel = CommonTrackMateArguments.addTargetChannel( this, nChannels );
	}

	public PathArgument imageFolder()
	{
		return imageFolder;
	}

	public PathArgument outputFolder()
	{
		return outputFolder;
	}

	public ChoiceArgument modelPretrained()
	{
		return modelPretrained;
	}

	public IntArgument targetChannel()
	{
		return targetChannel;
	}

	@Override
	protected String getCommand()
	{
		return "spotiflow.cli.predict";
	}

}
