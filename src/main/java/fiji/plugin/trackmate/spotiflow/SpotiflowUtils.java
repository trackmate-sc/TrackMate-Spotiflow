package fiji.plugin.trackmate.spotiflow;

import static fiji.plugin.trackmate.gui.GuiUtils.getResource;
import static fiji.plugin.trackmate.gui.GuiUtils.scaleImage;

import javax.swing.ImageIcon;

public class SpotiflowUtils
{

	public static final ImageIcon spotiflowLogo()
	{
		return new ImageIcon( getResource( "images/spotiflow_transp_small.png", SpotiflowUtils.class ) );
	}

	public static final ImageIcon spotiflowLogo64()
	{
		return scaleImage( spotiflowLogo(), 64, 64 );
	}
}
