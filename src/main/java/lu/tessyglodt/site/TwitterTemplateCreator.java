package lu.tessyglodt.site;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.stereotype.Component;

@Component
public class TwitterTemplateCreator {

	@Value("${app.twitter.consumerKey}")
	private String	consumerKey;

	@Value("${app.twitter.consumerSecret}")
	private String	consumerSecret;

	@Value("${app.twitter.accessToken}")
	private String	accessToken;

	@Value("${app.twitter.accessTokenSecret}")
	private String	accessTokenSecret;

	public Twitter getTwitterTemplate() {
		TwitterTemplate twitterTemplate = new TwitterTemplate(consumerKey, consumerSecret, accessToken, accessTokenSecret);
		return twitterTemplate;
	}
}