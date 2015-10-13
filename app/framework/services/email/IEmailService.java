package framework.services.email;

public interface IEmailService {
	
	  /**
     * Send an e-mail
     * 
     * @param subject
     *            the subject of the mail
     * @param from
     *            the sender of the mail
     * @param body
     *            the body of the message
     * @param to
     *            a table of recipients for this email
     */
    public void sendEmail(final String subject, final String from, final String body, final String... to);
   
}
