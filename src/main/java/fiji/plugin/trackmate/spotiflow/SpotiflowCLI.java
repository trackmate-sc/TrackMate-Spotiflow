package fiji.plugin.trackmate.spotiflow;

import fiji.plugin.trackmate.util.cli.CommonTrackMateArguments;
import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator;

public class SpotiflowCLI extends CondaCLIConfigurator
{

	private static final String KEY_SPOTIFLOW_PRETRAINED_MODEL = "SPOTIFLOW_PRETRAINED_MODEL";

	private final PathArgument imageFolder;

	private final ChoiceArgument modelPretrained;

	/** Target channel in the input image. 1-based index. */
	private final IntArgument targetChannel;

	public SpotiflowCLI( final int nChannels )
	{
		// Folder to store input images.
		this.imageFolder = addPathArgument()
				.name( "Input image folder path" )
				.help( "Path to image file or directory of image files. If a directory, will process all images in the directory." )
				.argument( "" ) // Empty but must be first
				.visible( false )
				.required( true )
				.get();

		// Pretrained model.
		this.modelPretrained = addChoiceArgument()
				.name( "Pretrained model" )
				.help( "Pretrained model name" )
				.key( KEY_SPOTIFLOW_PRETRAINED_MODEL )
				.argument( "--pretrained-model" )
				.addChoice( "general" )
				.addChoice( "hybiss" )
				.addChoice( "synth_complex" )
				.addChoice( "synth_3d" )
				.addChoice( "smfish_3d" )
				.defaultValue( "general" )
				.get();

		// Target channel.
		this.targetChannel = CommonTrackMateArguments.addTargetChannel( this, nChannels );
	}

	public PathArgument imageFolder()
	{
		return imageFolder;
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
