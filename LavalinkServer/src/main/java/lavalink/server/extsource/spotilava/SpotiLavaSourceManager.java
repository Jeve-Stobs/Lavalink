package lavalink.server.extsource.spotilava;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.CloseableHttpResponse;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;


public class SpotiLavaSourceManager implements AudioSourceManager, HttpConfigurable {
    private static final String DOMAIN_REGEX = "^(?:http://|https://|)(?:open\\.|)spotify\\.com/(track|episode)[/:]([A-Za-z0-9]+).*";
    private final HttpInterfaceManager httpInterfaceManager;
    private static final Pattern urlPattern = Pattern.compile(DOMAIN_REGEX);
    private static final Logger log = LoggerFactory.getLogger(SpotiLavaSourceManager.class);
    public String spotiLavaUrl;

    public SpotiLavaSourceManager(String spotiLavaUrl) {
        httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
        this.spotiLavaUrl = spotiLavaUrl;
    }

    @Override
    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
        if (spotiLavaUrl == null) {
            log.warn("SpotiLavaSource is enabled but no spotiLavaUrl set, returning as null");
            return null;
        }
        Matcher urlMatcher = urlPattern.matcher(reference.identifier);
        if (urlMatcher.group(1).equals("track")) {
            return loadTrack(urlMatcher.group(2)); 
        }
        return null;
    }

    private AudioTrack loadTrack(String trackId) {
        try (HttpInterface httpInterface = getHttpInterface()) {
            try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://" + spotiLavaUrl + "/" + trackId))) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                    throw new IOException("Unexpected response code from video info: " + statusCode);
                }
            JsonBrowser trackData = JsonBrowser.parse(response.getEntity().getContent());
            String title = trackData.get("data").get("title").safeText();
            String uploader = trackData.get("data").get("artist").values().get(0).safeText();
            long duration = Integer.parseInt(trackData.get("data").get("duration").text()) * 1000;
            String thumbnailUrl = trackData.get("data").get("image").text();
            return new SpotiLavaAudioTrack(new AudioTrackInfo(title, uploader, duration, trackId, false, getTrackUrl(trackId, "track"), thumbnailUrl), this);
            }
        } catch (IOException e) {
            throw new FriendlyException("Error occurred when extracting video info.", SUSPICIOUS, e);
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }
    
    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        // No extra information to save
    }
        
    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new SpotiLavaAudioTrack(trackInfo, this);
    }
    
    @Override
    public void shutdown() {
        // Nothing to shut down
    }
        
    /**
    * @return Get an HTTP interface for a playing track.
    */
    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }
        
    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
    }
        
    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
    }

    private static String getTrackUrl(String videoId, String type) {
        return "https://open.spotify.com/" + type + "/" + videoId;
    }
}