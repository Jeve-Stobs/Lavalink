package lavalink.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by napster on 05.03.18.
 */
@ConfigurationProperties(prefix = "lavalink.server.spotify")
@Component
public class SpotifyConfig {
    public spotiLavaUrl: String? = null
}
