package message;

import net.Constants;

public class GetAddr extends CommonMessage {
	
	public GetAddr(){
		super(Constants.GETADDR_CMD);
	}

}
