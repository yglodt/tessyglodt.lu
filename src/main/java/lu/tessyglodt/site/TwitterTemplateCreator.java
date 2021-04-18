package lu.tessyglodt.site;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

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

	// public Twitter getTwitterTemplate() {
	// final TwitterTemplate twitterTemplate = new TwitterTemplate(consumerKey, consumerSecret, accessToken, accessTokenSecret);
	// return twitterTemplate;
	// }

	public Twitter getTwitter() {
		final ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
				.setOAuthConsumerKey(consumerKey)
				.setOAuthConsumerSecret(consumerSecret)
				.setOAuthAccessToken(accessToken)
				.setOAuthAccessTokenSecret(accessTokenSecret);
		final TwitterFactory tf = new TwitterFactory(cb.build());
		return tf.getInstance();
	}
}