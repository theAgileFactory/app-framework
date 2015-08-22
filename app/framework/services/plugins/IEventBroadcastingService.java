package framework.services.plugins;

import framework.commons.message.EventMessage;

public interface IEventBroadcastingService {

    /**
     * Post an event to the OUT interface.
     * 
     * @param eventMessage
     *            an event message
     */
    public void postOutMessage(EventMessage eventMessage);

}
