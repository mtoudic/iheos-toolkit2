package gov.nist.toolkit.xdstools2.client.widgets.buttons;

import com.google.gwt.user.client.ui.Image;
import gov.nist.toolkit.xdstools2.client.util.ClientFactoryImpl;

/**
 * Created by Diane Azais local on 11/29/2015.
 */
public class PlayButton extends IconButton {
    private Image PLAY_ICON = new Image(ClientFactoryImpl.getIconsResources().getPlayIconBlack36());

    PlayButton(String _tooltip){
        super(ButtonType.PLAY_BUTTON, _tooltip);
        setIcon();
    }

    @Override
    protected void setIcon() {
        getElement().appendChild(PLAY_ICON.getElement());
    }

}
