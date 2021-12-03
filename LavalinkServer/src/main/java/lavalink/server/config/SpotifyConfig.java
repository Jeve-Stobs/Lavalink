package lavalink.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by KagChi on 03.12.21.
 */
@ConfigurationProperties(prefix = "lavalink.server.spotify")
@Component
public class SpotifyConfig {
    public String spotiLavaUrl = "";
}
