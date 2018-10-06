package lu.tessyglodt.site;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.stereotype.Component;

@Component
public class TwitterTemplateCreator {

	@Value("${app.twitter.consumer-key}")
	private String	consumerKey;

	@Value("${app.twitter.consumer-secret}")
	private String	consumerSecret;

	@Value("${app.twitter.access-token}")
	private String	accessToken;

	@Value("${app.twitter.access-token-secret}")
	private String	accessTokenSecret;

	public Twitter getTwitterTemplate() {
		final TwitterTemplate twitterTemplate = new TwitterTemplate(consumerKey, consumerSecret, accessToken, accessTokenSecret);
		return twitterTemplate;
	}
}